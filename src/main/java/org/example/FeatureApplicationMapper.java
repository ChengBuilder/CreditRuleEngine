package org.example;

import java.util.Map;
import java.util.UUID;

/**
 * 特征映射器：
 * 将“原始特征 Map（编码->值）”转换为规则引擎可消费的 Application 对象。
 *
 * 设计目标：
 * 1. 让规则层只关注业务字段，不直接处理特征编码
 * 2. 在映射层统一完成类型转换与默认值兜底
 * 3. 为数据质量问题提供降级能力，避免单个坏值打崩整条决策链路
 */
public final class FeatureApplicationMapper {
    /**
     * 工具类不允许实例化。
     */
    private FeatureApplicationMapper() {
    }

    /**
     * 进行核心映射。
     *
     * 映射策略：
     * - 先通过字典按别名取值
     * - 再按字段目标类型做转换（int/double/boolean/string）
     * - 转换失败时使用默认值，保证流程可继续
     *
     * @param rawFeatures   原始特征（编码->字符串值）
     * @param dictionary    特征字典（别名->编码）
     * @param fallbackAppId 当特征中缺 app_id 时使用的备选申请编号
     * @return 转换后的 Application
     */
    public static Application toApplication(
            Map<String, String> rawFeatures,
            FeatureDictionary dictionary,
            String fallbackAppId
    ) {
        // appId 兜底顺序：特征值 -> 入参 fallback -> 随机生成
        String appId = firstNonBlank(
                dictionary.valueOf(rawFeatures, "app_id"),
                fallbackAppId,
                "APP-" + UUID.randomUUID()
        );

        // 基础字段
        int age = intVal(rawFeatures, dictionary, "age", 0);
        double income = doubleVal(rawFeatures, dictionary, "income", 0.0);

        // 风控字段（均提供缺省值，避免特征缺失导致 NPE 或转换异常）
        double requestedAmount = doubleVal(rawFeatures, dictionary, "requested_amount", income * 4);
        int creditScore = intVal(rawFeatures, dictionary, "credit_score", 680);
        double dti = doubleVal(rawFeatures, dictionary, "debt_to_income_ratio", 0.35);
        int employmentMonths = intVal(rawFeatures, dictionary, "employment_months", 24);
        int existingLoanCount = intVal(rawFeatures, dictionary, "existing_loan_count", 1);
        int recentInquiryCount = intVal(rawFeatures, dictionary, "recent_inquiry_count", 1);
        int pastDue30dCount = intVal(rawFeatures, dictionary, "past_due_30d_count", 0);
        int pastDue90dCount = intVal(rawFeatures, dictionary, "past_due_90d_count", 0);
        boolean blacklistedDevice = boolVal(rawFeatures, dictionary, "blacklisted_device", false);
        String ipRiskLevel = firstNonBlank(dictionary.valueOf(rawFeatures, "ip_risk_level"), "LOW");
        String channel = firstNonBlank(dictionary.valueOf(rawFeatures, "channel"), "APP");

        Application app = new Application(
                appId,
                age,
                income,
                requestedAmount,
                creditScore,
                dti,
                employmentMonths,
                existingLoanCount,
                recentInquiryCount,
                pastDue30dCount,
                pastDue90dCount,
                blacklistedDevice,
                ipRiskLevel,
                channel
        );

        // 示例：对暂未进入规则的字段，也可打到轨迹里用于可解释性
        String gender = dictionary.valueOf(rawFeatures, "gender");
        if (gender != null && !gender.isBlank()) {
            app.addRuleHit("feature gender=" + gender);
        }
        return app;
    }

    /**
     * 读取整型特征。
     * 解析失败时返回默认值，不抛异常。
     */
    private static int intVal(Map<String, String> raw, FeatureDictionary dict, String alias, int defaultValue) {
        String rawValue = dict.valueOf(raw, alias);
        if (rawValue == null || rawValue.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(rawValue.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 读取浮点型特征。
     * 解析失败时返回默认值，不抛异常。
     */
    private static double doubleVal(Map<String, String> raw, FeatureDictionary dict, String alias, double defaultValue) {
        String rawValue = dict.valueOf(raw, alias);
        if (rawValue == null || rawValue.isBlank()) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(rawValue.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 读取布尔型特征，兼容多种常见表达：
     * true: 1/true/yes/y
     * false: 0/false/no/n
     * 其他值：返回默认值
     */
    private static boolean boolVal(Map<String, String> raw, FeatureDictionary dict, String alias, boolean defaultValue) {
        String rawValue = dict.valueOf(raw, alias);
        if (rawValue == null || rawValue.isBlank()) {
            return defaultValue;
        }
        String normalized = rawValue.trim().toLowerCase();
        if ("1".equals(normalized) || "true".equals(normalized) || "yes".equals(normalized) || "y".equals(normalized)) {
            return true;
        }
        if ("0".equals(normalized) || "false".equals(normalized) || "no".equals(normalized) || "n".equals(normalized)) {
            return false;
        }
        return defaultValue;
    }

    /**
     * 返回第一个非空白字符串，用于多级兜底。
     */
    private static String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate.trim();
            }
        }
        return "";
    }
}
