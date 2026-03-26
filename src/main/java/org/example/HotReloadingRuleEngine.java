package org.example;

import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.builder.ReleaseId;
import org.kie.api.event.rule.AfterMatchFiredEvent;
import org.kie.api.event.rule.DefaultAgendaEventListener;
import org.kie.api.io.Resource;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 热更新规则引擎核心实现。
 *
 * 职责：
 * 1. 管理 KieContainer 生命周期
 * 2. 检测规则文件/参数文件变更并自动重载
 * 3. 在并发场景下提供“原子切换”能力，避免请求读到半成品状态
 */
public class HotReloadingRuleEngine {
    /**
     * 写入 KieFileSystem 时使用的虚拟资源路径。
     * 该路径用于让 Drools 识别资源位置，不要求真实存在。
     */
    private static final String RULE_RESOURCE_PATH = "src/main/resources/org/example/rules/credit_rules.drl";

    /** Drools 服务入口。 */
    private final KieServices kieServices = KieServices.Factory.get();
    /** 外部规则文件路径。 */
    private final Path rulePath;
    /** 外部配置文件路径。 */
    private final Path configPath;
    /** 当前生效快照（容器 + 配置 + 版本），通过原子引用切换。 */
    private final AtomicReference<EngineSnapshot> snapshotRef = new AtomicReference<>();

    /** 上次加载时记录的规则文件时间戳。 */
    private volatile FileTime ruleLastModified;
    /** 上次加载时记录的配置文件时间戳。 */
    private volatile FileTime configLastModified;

    /**
     * 初始化时立即执行一次加载，保证首次请求可用。
     */
    public HotReloadingRuleEngine(Path rulePath, Path configPath) {
        this.rulePath = rulePath;
        this.configPath = configPath;
        Trace.log("ENGINE", "初始化热更新引擎 rulePath=" + rulePath + ", configPath=" + configPath);
        reload("initial-load");
    }

    /**
     * 对单个申请进行规则评估。
     *
     * 执行前先检查是否需要热更新，之后使用当前快照创建会话执行。
     */
    public void evaluate(Application app) {
        refreshIfNeeded();
        EngineSnapshot snapshot = snapshotRef.get();
        Trace.log("ENGINE", "开始执行 appId=" + app.getAppId() + ", activeVersion=" + snapshot.version);
        KieSession session = null;
        try {
            session = snapshot.kieContainer.newKieSession();
            session.addEventListener(new DefaultAgendaEventListener() {
                @Override
                public void afterMatchFired(AfterMatchFiredEvent event) {
                    Trace.log("RULE", "命中规则: " + event.getMatch().getRule().getName());
                }
            });
            session.insert(snapshot.ruleConfig);
            session.insert(app);
            int fired = session.fireAllRules();
            Trace.log("ENGINE", "执行完成 appId=" + app.getAppId() + ", firedRules=" + fired
                    + ", status=" + app.getStatus() + ", riskScore=" + app.getRiskScore());
        } finally {
            // 每次请求结束都释放 session，避免状态污染
            if (session != null) {
                session.dispose();
            }
        }
    }

    /**
     * 获取当前活动规则版本（用于日志、监控、灰度排查）。
     */
    public String getActiveVersion() {
        return snapshotRef.get().version;
    }

    /**
     * 手动强制重载，不依赖时间戳变更。
     */
    public void forceReload() {
        reload("manual-force-reload");
    }

    /**
     * 仅在文件发生变化时触发热更新。
     */
    private void refreshIfNeeded() {
        if (hasChanged(rulePath, ruleLastModified) || hasChanged(configPath, configLastModified)) {
            reload("hot-reload");
        }
    }

    /**
     * 重载逻辑（串行执行）：
     * 1. 读取最新参数
     * 2. 构建新 KieContainer
     * 3. 原子替换快照
     * 4. 释放旧容器
     */
    private synchronized void reload(String reason) {
        // 除首次加载和强制重载外，其余场景都做“无变化即跳过”
        if (!"initial-load".equals(reason)) {
            if (!hasChanged(rulePath, ruleLastModified) && !hasChanged(configPath, configLastModified)
                    && !"manual-force-reload".equals(reason)) {
                return;
            }
        }

        Trace.log("ENGINE", "触发重载 reason=" + reason);
        RuleConfig ruleConfig = RuleConfigLoader.load(configPath);
        KieContainer newContainer = buildKieContainer();
        String version = buildVersionTag();
        EngineSnapshot newSnapshot = new EngineSnapshot(newContainer, ruleConfig, version);

        // 原子切换，保证并发请求要么读到旧版本，要么读到新版本
        EngineSnapshot oldSnapshot = snapshotRef.getAndSet(newSnapshot);
        if (oldSnapshot != null) {
            // 释放旧容器资源（类加载器、线程池等）
            oldSnapshot.kieContainer.dispose();
        }

        this.ruleLastModified = readLastModified(rulePath);
        this.configLastModified = readLastModified(configPath);
        Trace.log("ENGINE", "重载完成 newVersion=" + version
                + ", ruleLastModified=" + this.ruleLastModified
                + ", configLastModified=" + this.configLastModified);
    }

    /**
     * 编译规则并构建新的 KieContainer。
     * 编译失败时直接抛错，避免错误规则上线。
     */
    private KieContainer buildKieContainer() {
        KieFileSystem kfs = kieServices.newKieFileSystem();
        ReleaseId releaseId = kieServices.newReleaseId("org.example", "rules", buildVersionTag());
        Trace.log("ENGINE", "开始编译规则 releaseId=" + releaseId);
        kfs.generateAndWritePomXML(releaseId);
        kfs.write(RULE_RESOURCE_PATH, resolveRuleResource());

        KieBuilder kieBuilder = kieServices.newKieBuilder(kfs);
        kieBuilder.buildAll();
        if (kieBuilder.getResults().hasMessages(Message.Level.ERROR)) {
            throw new IllegalStateException("rule compile failed: " + kieBuilder.getResults());
        }
        Trace.log("ENGINE", "规则编译成功 releaseId=" + releaseId);
        return kieServices.newKieContainer(releaseId);
    }

    /**
     * 解析规则资源：仅允许使用外部规则文件。
     */
    private Resource resolveRuleResource() {
        if (rulePath == null || !Files.exists(rulePath)) {
            throw new IllegalStateException("rule file is required: " + rulePath);
        }
        Trace.log("ENGINE", "加载外部规则文件: " + rulePath.toAbsolutePath());
        return kieServices.getResources().newFileSystemResource(rulePath.toFile(), "UTF-8");
    }

    /**
     * 判断文件时间戳是否变化。
     */
    private static boolean hasChanged(Path path, FileTime previous) {
        if (path == null || !Files.exists(path)) {
            return false;
        }
        FileTime current = readLastModified(path);
        if (current == null) {
            return false;
        }
        return !Objects.equals(current, previous);
    }

    /**
     * 安全读取文件最后修改时间。
     */
    private static FileTime readLastModified(Path path) {
        try {
            if (path == null || !Files.exists(path)) {
                return null;
            }
            return Files.getLastModifiedTime(path);
        } catch (Exception e) {
            throw new IllegalStateException("failed to read lastModified for " + path, e);
        }
    }

    /**
     * 生成版本号。
     * 使用纳秒时间戳可在同毫秒内区分连续发布。
     */
    private static String buildVersionTag() {
        return String.valueOf(System.nanoTime());
    }

    /**
     * 引擎快照对象：
     * 每次重载都会创建新快照，并通过 AtomicReference 一次性替换。
     */
    private static final class EngineSnapshot {
        private final KieContainer kieContainer;
        private final RuleConfig ruleConfig;
        private final String version;

        /**
         * @param kieContainer 已编译规则容器
         * @param ruleConfig   对应规则参数快照
         * @param version      本次发布版本号
         */
        private EngineSnapshot(KieContainer kieContainer, RuleConfig ruleConfig, String version) {
            this.kieContainer = kieContainer;
            this.ruleConfig = ruleConfig;
            this.version = version;
        }
    }
}
