package org.example;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.util.Map;
import java.util.Objects;

/**
 * Drools 统一执行入口。
 *
 * 对外提供两种能力：
 * 1. 直接传 Application 执行规则
 * 2. 传入原始特征 Map（编码->值），先映射再执行规则
 *
 * 同时负责：
 * - 持有热更新规则引擎实例
 * - 缓存特征字典并按文件变更自动刷新
 */
public class DroolsRunner {
    /**
     * 外部规则文件路径（可在线编辑，触发热更新）。
     */
    private static final Path EXTERNAL_RULE_PATH = Paths.get("rules", "credit_rules.drl");
    /**
     * 外部规则参数路径（阈值、分值等）。
     */
    private static final Path EXTERNAL_CONFIG_PATH = Paths.get("config", "risk-config.properties");
    /**
     * 外部特征字典路径（别名 -> 编码）。
     */
    private static final Path EXTERNAL_FEATURE_DICT_PATH = Paths.get("config", "feature-dictionary.properties");
    /**
     * 规则引擎核心对象：内部会做规则与参数的热更新。
     */
    private static final HotReloadingRuleEngine ENGINE =
            new HotReloadingRuleEngine(EXTERNAL_RULE_PATH, EXTERNAL_CONFIG_PATH);
    /**
     * 当前生效的特征字典缓存。
     * 使用 volatile 保证多线程下可见性。
     */
    private static volatile FeatureDictionary FEATURE_DICTIONARY =
            FeatureDictionaryLoader.load(EXTERNAL_FEATURE_DICT_PATH);
    /**
     * 字典文件最后修改时间，用于变更检测。
     */
    private static volatile FileTime FEATURE_DICT_LAST_MODIFIED =
            readLastModified(EXTERNAL_FEATURE_DICT_PATH);

    /**
     * 直接执行已构造好的 Application。
     */
    public static void fireRules(Application app) {
        ENGINE.evaluate(app);
    }

    /**
     * 原始特征执行入口（无 fallbackAppId）。
     */
    public static Application fireRulesFromFeatures(Map<String, String> rawFeatures) {
        return fireRulesFromFeatures(rawFeatures, null);
    }

    /**
     * 原始特征执行入口（可指定 fallbackAppId）。
     *
     * 流程：
     * 1. 获取当前字典（必要时自动热刷新）
     * 2. 特征映射为 Application
     * 3. 执行规则并返回决策结果
     */
    public static Application fireRulesFromFeatures(Map<String, String> rawFeatures, String fallbackAppId) {
        FeatureDictionary dictionary = currentFeatureDictionary();
        Application app = FeatureApplicationMapper.toApplication(rawFeatures, dictionary, fallbackAppId);
        ENGINE.evaluate(app);
        return app;
    }

    public static String activeVersion() {
        return ENGINE.getActiveVersion();
    }

    /**
     * 手动强制刷新：
     * - 刷新规则引擎（规则文件 + 参数）
     * - 刷新特征字典缓存
     */
    public static void forceReload() {
        ENGINE.forceReload();
        synchronized (DroolsRunner.class) {
            FEATURE_DICTIONARY = FeatureDictionaryLoader.load(EXTERNAL_FEATURE_DICT_PATH);
            FEATURE_DICT_LAST_MODIFIED = readLastModified(EXTERNAL_FEATURE_DICT_PATH);
        }
    }

    /**
     * 获取当前可用的特征字典。
     *
     * 策略：
     * - 每次读取前先比对字典文件时间戳
     * - 发生变化时使用双重检查 + synchronized 刷新
     * - 未变化时直接返回缓存，避免频繁 IO
     */
    private static FeatureDictionary currentFeatureDictionary() {
        FileTime now = readLastModified(EXTERNAL_FEATURE_DICT_PATH);
        if (!Objects.equals(now, FEATURE_DICT_LAST_MODIFIED)) {
            synchronized (DroolsRunner.class) {
                FileTime latest = readLastModified(EXTERNAL_FEATURE_DICT_PATH);
                if (!Objects.equals(latest, FEATURE_DICT_LAST_MODIFIED)) {
                    FEATURE_DICTIONARY = FeatureDictionaryLoader.load(EXTERNAL_FEATURE_DICT_PATH);
                    FEATURE_DICT_LAST_MODIFIED = latest;
                }
            }
        }
        return FEATURE_DICTIONARY;
    }

    /**
     * 读取文件最后修改时间。
     * 路径不存在时返回 null。
     */
    private static FileTime readLastModified(Path path) {
        try {
            if (path == null || !Files.exists(path)) {
                return null;
            }
            return Files.getLastModifiedTime(path);
        } catch (Exception e) {
            throw new IllegalStateException("failed to read feature dictionary timestamp: " + path, e);
        }
    }
}
