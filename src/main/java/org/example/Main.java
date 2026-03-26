package org.example;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 演示入口：
 * 1. 展示当前生效规则版本
 * 2. 构造“特征编码 -> 特征值”的样例输入
 * 3. 通过 DroolsRunner 进行映射+决策
 */
public class Main {
    public static void main(String[] args) {
        // 打印当前规则版本，便于观察热更新是否生效
        System.out.println("Active rule version: " + DroolsRunner.activeVersion());
        System.out.println("Edit ./rules/credit_rules.drl or ./config/risk-config.properties to hot reload.");
        System.out.println("Feature dictionary: ./config/feature-dictionary.properties");
        System.out.println();

        // 构造 4 组样例特征，模拟来自特征平台/实时特征服务的原始数据
        List<Map<String, String>> featureCases = List.of(
                // 低风险样例：预期 APPROVED
                features(
                        "100000000", "A1001",
                        "100001001", "32",
                        "110001001", "male",
                        "120001001", "22000",
                        "120001002", "120000",
                        "130001001", "780",
                        "140001001", "0.28",
                        "150001001", "48",
                        "160001001", "1",
                        "170001001", "1",
                        "180001001", "0",
                        "180001002", "0",
                        "190001001", "false",
                        "190001002", "LOW",
                        "190001003", "OFFLINE"
                ),
                // 中风险样例：预期 MANUAL_REVIEW
                features(
                        "100000000", "A1002",
                        "100001001", "26",
                        "110001001", "female",
                        "120001001", "9000",
                        "120001002", "85000",
                        "130001001", "655",
                        "140001001", "0.55",
                        "150001001", "10",
                        "160001001", "3",
                        "170001001", "5",
                        "180001001", "2",
                        "180001002", "0",
                        "190001001", "false",
                        "190001002", "MEDIUM",
                        "190001003", "WEB"
                ),
                // 高风险样例：预期按分数拒绝
                features(
                        "100000000", "A1003",
                        "100001001", "35",
                        "110001001", "male",
                        "120001001", "7000",
                        "120001002", "150000",
                        "130001001", "560",
                        "140001001", "0.68",
                        "150001001", "5",
                        "160001001", "6",
                        "170001001", "7",
                        "180001001", "3",
                        "180001002", "0",
                        "190001001", "false",
                        "190001002", "MEDIUM",
                        "190001003", "APP"
                ),
                // 硬拒绝样例：90 天严重逾期
                features(
                        "100000000", "A1004",
                        "100001001", "22",
                        "110001001", "male",
                        "120001001", "8000",
                        "120001002", "50000",
                        "130001001", "620",
                        "140001001", "0.42",
                        "150001001", "24",
                        "160001001", "2",
                        "170001001", "9",
                        "180001001", "1",
                        "180001002", "2",
                        "190001001", "false",
                        "190001002", "HIGH",
                        "190001003", "APP"
                )
        );

        // 逐条执行并输出最终决策对象（状态、分数、命中轨迹）
        for (Map<String, String> rawFeatures : featureCases) {
            Application app = DroolsRunner.fireRulesFromFeatures(rawFeatures);
            System.out.println(app);
            System.out.println();
        }
    }

    /**
     * 便捷构造特征 Map 的小工具方法。
     *
     * 入参采用 key-value 顺序拼接：
     * features("100001001","18","110001001","male")
     */
    private static Map<String, String> features(String... keyValues) {
        Map<String, String> map = new HashMap<>();
        // key-value 必须成对出现，否则直接抛错，避免构造出脏数据
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("feature keyValues must be even length");
        }
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(keyValues[i], keyValues[i + 1]);
        }
        return map;
    }
}
