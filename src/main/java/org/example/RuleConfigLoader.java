package org.example;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * 规则参数加载器：
 * 负责把外部 properties 配置文件加载并映射到 RuleConfig 对象。
 */
public final class RuleConfigLoader {
    /**
     * 工具类不允许实例化。
     */
    private RuleConfigLoader() {
    }

    /**
     * 加载规则参数。
     *
     * @param configPath 外部配置路径（必须存在）
     * @return RuleConfig（已填充并通过 validate 校验）
     */
    public static RuleConfig load(Path configPath) {
        RuleConfig config = new RuleConfig();
        Properties properties = new Properties();

        if (configPath == null || !Files.exists(configPath)) {
            throw new IllegalStateException("risk config file is required: " + configPath);
        }
        try (InputStream in = Files.newInputStream(configPath)) {
            properties.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("failed to load config file: " + configPath, e);
        }

        // 按字段逐项写入配置对象
        apply(properties, config);
        // 统一校验关键阈值关系
        config.validate();
        return config;
    }

    /**
     * 将 properties 中的配置按字段映射到 RuleConfig。
     * 未配置时保持 RuleConfig 默认值。
     */
    private static void apply(Properties p, RuleConfig c) {
        c.setMinAge(intValue(p, "minAge", c.getMinAge()));
        c.setSevereDelinquency90dRejectCount(intValue(p, "severeDelinquency90dRejectCount", c.getSevereDelinquency90dRejectCount()));
        c.setHighRiskInquiryRejectCount(intValue(p, "highRiskInquiryRejectCount", c.getHighRiskInquiryRejectCount()));

        c.setCreditScoreVeryLowUpper(intValue(p, "creditScoreVeryLowUpper", c.getCreditScoreVeryLowUpper()));
        c.setCreditScoreLowUpper(intValue(p, "creditScoreLowUpper", c.getCreditScoreLowUpper()));
        c.setCreditScoreMediumUpper(intValue(p, "creditScoreMediumUpper", c.getCreditScoreMediumUpper()));
        c.setCreditScoreExcellentMin(intValue(p, "creditScoreExcellentMin", c.getCreditScoreExcellentMin()));

        c.setScoreCreditVeryLow(intValue(p, "scoreCreditVeryLow", c.getScoreCreditVeryLow()));
        c.setScoreCreditLow(intValue(p, "scoreCreditLow", c.getScoreCreditLow()));
        c.setScoreCreditMedium(intValue(p, "scoreCreditMedium", c.getScoreCreditMedium()));
        c.setScoreCreditExcellentBonus(intValue(p, "scoreCreditExcellentBonus", c.getScoreCreditExcellentBonus()));

        c.setDtiHighThreshold(doubleValue(p, "dtiHighThreshold", c.getDtiHighThreshold()));
        c.setDtiMediumThreshold(doubleValue(p, "dtiMediumThreshold", c.getDtiMediumThreshold()));
        c.setScoreDtiHigh(intValue(p, "scoreDtiHigh", c.getScoreDtiHigh()));
        c.setScoreDtiMedium(intValue(p, "scoreDtiMedium", c.getScoreDtiMedium()));

        c.setThinEmploymentMonthsUpper(intValue(p, "thinEmploymentMonthsUpper", c.getThinEmploymentMonthsUpper()));
        c.setShortEmploymentMonthsUpper(intValue(p, "shortEmploymentMonthsUpper", c.getShortEmploymentMonthsUpper()));
        c.setScoreThinEmployment(intValue(p, "scoreThinEmployment", c.getScoreThinEmployment()));
        c.setScoreShortEmployment(intValue(p, "scoreShortEmployment", c.getScoreShortEmployment()));

        c.setInquirySpikeCount(intValue(p, "inquirySpikeCount", c.getInquirySpikeCount()));
        c.setScoreInquirySpike(intValue(p, "scoreInquirySpike", c.getScoreInquirySpike()));
        c.setTooManyLoansCount(intValue(p, "tooManyLoansCount", c.getTooManyLoansCount()));
        c.setScoreTooManyLoans(intValue(p, "scoreTooManyLoans", c.getScoreTooManyLoans()));
        c.setMildDelinquency30dCount(intValue(p, "mildDelinquency30dCount", c.getMildDelinquency30dCount()));
        c.setScoreMildDelinquency30d(intValue(p, "scoreMildDelinquency30d", c.getScoreMildDelinquency30d()));

        c.setHighRequestToIncomeRatio(doubleValue(p, "highRequestToIncomeRatio", c.getHighRequestToIncomeRatio()));
        c.setMediumRequestToIncomeRatio(doubleValue(p, "mediumRequestToIncomeRatio", c.getMediumRequestToIncomeRatio()));
        c.setScoreHighRequestToIncome(intValue(p, "scoreHighRequestToIncome", c.getScoreHighRequestToIncome()));
        c.setScoreMediumRequestToIncome(intValue(p, "scoreMediumRequestToIncome", c.getScoreMediumRequestToIncome()));

        c.setStrongApplicantCreditMin(intValue(p, "strongApplicantCreditMin", c.getStrongApplicantCreditMin()));
        c.setStrongApplicantDtiMax(doubleValue(p, "strongApplicantDtiMax", c.getStrongApplicantDtiMax()));
        c.setStrongApplicantIncomeMin(doubleValue(p, "strongApplicantIncomeMin", c.getStrongApplicantIncomeMin()));
        c.setScoreStrongApplicantBonus(intValue(p, "scoreStrongApplicantBonus", c.getScoreStrongApplicantBonus()));

        c.setOfflineTrustedCreditMin(intValue(p, "offlineTrustedCreditMin", c.getOfflineTrustedCreditMin()));
        c.setOfflineTrustedDtiMax(doubleValue(p, "offlineTrustedDtiMax", c.getOfflineTrustedDtiMax()));
        c.setScoreOfflineTrustedBonus(intValue(p, "scoreOfflineTrustedBonus", c.getScoreOfflineTrustedBonus()));

        c.setRejectScoreThreshold(intValue(p, "rejectScoreThreshold", c.getRejectScoreThreshold()));
        c.setManualReviewScoreThreshold(intValue(p, "manualReviewScoreThreshold", c.getManualReviewScoreThreshold()));
    }

    /**
     * 读取 int 配置值。
     * - 空值：返回默认值
     * - 非空：trim 后 parse
     */
    private static int intValue(Properties p, String key, int defaultValue) {
        String value = p.getProperty(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Integer.parseInt(value.trim());
    }

    /**
     * 读取 double 配置值。
     * - 空值：返回默认值
     * - 非空：trim 后 parse
     */
    private static double doubleValue(Properties p, String key, double defaultValue) {
        String value = p.getProperty(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Double.parseDouble(value.trim());
    }
}
