# Drools 从小白到上手（风控场景版）

这份文档按“先跑通，再理解，再工程化”的顺序写。建议你边读边改本项目代码。

## 1. Drools 是什么

Drools 是 Java 规则引擎，核心价值是把业务决策从 `if/else` 里拆出来，用规则文件统一管理。

在风控里非常适合：
- 准入规则频繁变更（策略同学常改）
- 需要可解释（命中了哪些规则）
- 需要快速灰度不同策略版本

## 2. 核心概念（必须掌握）

- Fact：事实对象，比如 `Application`
- Rule：规则，`when` 条件 + `then` 动作
- Working Memory：规则引擎里的事实集合
- KieBase：编译后的规则库
- KieSession：一次执行会话（插入事实、触发规则）
- Agenda：待执行规则队列

你可以先把 Drools 理解成：
1. 把对象丢进规则引擎
2. 引擎自动匹配符合条件的规则
3. 依次执行动作并更新对象

## 3. 项目里的执行链路

当前 demo 的主流程：
1. `Main` 里构造多个申请单
2. `DroolsRunner` 调用 `HotReloadingRuleEngine`
3. 插入 `Application`，`fireAllRules()`
4. 输出最终状态、风险分和命中轨迹

关键文件：
- `src/main/java/org/example/Application.java`
- `src/main/java/org/example/DroolsRunner.java`
- `src/main/java/org/example/HotReloadingRuleEngine.java`
- `src/main/java/org/example/RuleConfig.java`
- `src/main/java/org/example/RuleConfigLoader.java`
- `config/risk-config.properties`
- `rules/credit_rules.drl`

## 3.1 热更新与配置化（本项目已实现）

实现方式：
- 规则文件外置：`rules/credit_rules.drl`
- 参数外置：`config/risk-config.properties`
- 每次评估前检查两个文件的 `lastModified`
- 检测到变化后，重新编译规则并原子替换 `KieContainer`

你可以直接改：
- `config/risk-config.properties` 里的阈值
- `rules/credit_rules.drl` 里的规则逻辑

然后再次调用 `DroolsRunner.fireRules(...)`，会自动热加载。

## 4. DRL 语法快速入门

最小规则结构：

```drl
rule "RuleName"
when
    $app : Application(age < 18)
then
    $app.setStatus("REJECTED");
    update($app);
end
```

重点：
- `$app : Application(...)` 是模式匹配，并把命中的对象绑定到变量 `$app`
- `update($app)` 很关键，告诉引擎事实已变更，需要重新匹配后续规则
- 规则名称要语义清晰，后续排查会非常省事

## 5. 本项目风控规则分层（推荐范式）

你现在这版规则按三层组织：

1. 硬拒绝层（Hard Reject）
- 年龄不达标、黑名单设备、严重逾期、强欺诈信号
- 特点：命中即拒绝，优先级最高（`salience 100`）

2. 风险评分层（Score Rules）
- 信用分、负债率、在职月数、查询次数、申请金额收入比等
- 特点：可叠加加减分，方便策略精调

3. 决策层（Decision Rules）
- `riskScore >= 70` 拒绝
- `40 <= riskScore < 70` 人审
- `< 40` 自动通过

这是风控里非常常见的“规则 + 打分 + 阈值决策”组合。

## 6. 如何设计“复杂但可维护”的规则

推荐原则：
- 先硬拒绝，再打分，再决策，避免规则互相打架
- 每条规则只做一件事
- 每条规则都留下命中轨迹（本项目用 `addRuleHit`）
- 不把太多业务参数写死在 Java，尽量写在 DRL

命名建议：
- `HardReject_xxx`
- `Score_xxx`
- `Decision_xxx`

看名字就知道层级和作用。

## 7. 规则冲突与执行顺序

常用控制手段：
- `salience`：优先级，值越大越先执行
- `activation-group`：组内只执行一条（互斥决策）
- `agenda-group`：手动分组触发
- `no-loop`：防止规则 update 后自己反复触发

你现在先用 `salience + 状态条件(status == "PENDING")` 就够了。

## 8. 常见坑（你刚刚就遇到了其中一个）

1. 依赖缺失导致规则编译异常
- 你这次是缺 `drools-mvel`，导致约束构建器不可用，抛 `UnsupportedOperationException`

2. 规则文件路径和 package 不一致
- 会有警告，建议目录与 `package` 一致

3. `update()` 忘写
- 事实改了但引擎不知道，后续规则不触发

4. SLF4J 版本不匹配
- 会出现 binder 警告，已在本项目对齐

## 9. 调试与排查方法

排查优先级：
1. 先看 `KieBuilder` 编译错误（语法、类型、依赖）
2. 看 `ruleHits` 是否记录到预期轨迹
3. 打印关键字段（`status`, `riskScore`, `rejectReason`）
4. 用最小样本输入复现单条规则

建议加一类“策略回放”样本：
- 同一申请单改一个字段，看结果差异是否符合预期

## 10. 测试建议（非常重要）

最少要有三类测试：
- 单规则测试：某条规则是否命中
- 决策边界测试：39/40/69/70 这类阈值边缘
- 回归测试：老样本在新规则下结果是否符合预期

实战里通常把样本放 CSV/JSON，批量回放并比对结果。

## 11. 性能与工程化建议

- `KieBase` 尽量复用，不要每次请求都重新编译
- `KieSession` 轻量，按请求创建与释放
- 把规则按业务域拆文件（反欺诈、信用、额度）
- 引入版本管理：`rules_v1`, `rules_v2` 做 A/B
- 每次规则发版都做回归回放

## 12. 进阶能力地图

掌握这些后，你就不只是“会写规则”，而是能做规则系统：
- `accumulate` 聚合统计（例如 7 天内事件次数）
- `query` 可复用查询
- `function` DRL 内通用方法
- DSL（业务可读性更高）
- 决策表（Excel 驱动策略）
- Rule Unit（Drools 8 新模型）

## 13. 一周上手计划（可执行）

Day 1：
- 跑通 demo，读懂 `Application` 和 `credit_rules.drl`

Day 2：
- 自己加 3 条打分规则，验证命中轨迹

Day 3：
- 增加边界样例（例如信用分 639/640/641）

Day 4：
- 加入 `activation-group` 处理互斥决策

Day 5：
- 把规则拆为多个 DRL 文件并统一加载

Day 6：
- 写基础单元测试和回归样本回放

Day 7：
- 设计一版“灰度发布策略”文档（规则版本化）

## 14. 特征码 HashMap 接入模式（大厂常见）

你的场景是：
- 输入：`Map<String, String>`，例如 `{ "100001001": "18", "110001001": "male" }`
- 规则需要按“业务含义”判断，而不是直接操作编码

本项目已实现一版标准接入：
1. 特征字典：`config/feature-dictionary.properties`
2. 特征映射：`FeatureApplicationMapper` 把 raw feature map 转为 `Application`
3. 决策执行：Drools 规则只面向业务字段（age、income、creditScore 等）

关键文件：
- `src/main/java/org/example/FeatureDictionary.java`
- `src/main/java/org/example/FeatureDictionaryLoader.java`
- `src/main/java/org/example/FeatureApplicationMapper.java`
- `src/main/java/org/example/DroolsRunner.java`
- `config/feature-dictionary.properties`

为什么推荐这样做：
- 规则可读性高（策略能看懂）
- 特征编码变更只改字典/映射层，不用重写规则
- 便于做数据质量校验和降级默认值

不太推荐的方式：
- 在 DRL 里直接写 `"100001001"` 这种硬编码
- 规则层直接做大量字符串解析/类型转换

更进一步的企业级做法：
1. Feature Store 统一产出标准特征（含时间窗定义）
2. 决策服务只消费“标准语义特征对象”
3. 规则与模型共享同一套特征定义版本
4. 决策日志记录：原始特征、映射后特征、规则版本、命中轨迹

---

如果你愿意，我下一步可以继续帮你做两件事：
1. 补完整的 JUnit 回归测试模板（批量样本回放）
2. 再上一个台阶：做 `kjar + KieScanner + 灰度路由 + 回滚`
