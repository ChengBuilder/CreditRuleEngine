package org.example;

/**
 * 规则参数配置对象（可外置、可热更新）。
 *
 * 说明：
 * - 这个类承载了 DRL 中使用到的阈值和分值
 * - 规则逻辑本身尽量稳定，策略变化优先通过修改这些参数完成
 * - 字段按“硬拒绝 / 分数分层 / 最终决策”进行分组
 */
public class RuleConfig {
    /** 最小准入年龄（低于该值直接硬拒绝）。 */
    private int minAge = 18;
    /** 90 天严重逾期达到该次数时硬拒绝。 */
    private int severeDelinquency90dRejectCount = 2;
    /** 当 IP 高风险且查询次数达到该阈值时硬拒绝。 */
    private int highRiskInquiryRejectCount = 8;

    /** 信用分极低区间上界。 */
    private int creditScoreVeryLowUpper = 580;
    /** 信用分偏低区间上界。 */
    private int creditScoreLowUpper = 640;
    /** 信用分中等区间上界。 */
    private int creditScoreMediumUpper = 700;
    /** 信用分优质区间下界。 */
    private int creditScoreExcellentMin = 760;

    /** 极低信用分加分（风险上浮）。 */
    private int scoreCreditVeryLow = 45;
    /** 偏低信用分加分。 */
    private int scoreCreditLow = 30;
    /** 中等信用分加分。 */
    private int scoreCreditMedium = 15;
    /** 优质信用分减分（风险下调）。 */
    private int scoreCreditExcellentBonus = -12;

    /** 负债收入比高风险阈值。 */
    private double dtiHighThreshold = 0.65;
    /** 负债收入比中风险阈值。 */
    private double dtiMediumThreshold = 0.50;
    /** DTI 高风险加分。 */
    private int scoreDtiHigh = 35;
    /** DTI 中风险加分。 */
    private int scoreDtiMedium = 20;

    /** 在职月数薄档上界。 */
    private int thinEmploymentMonthsUpper = 6;
    /** 在职月数短档上界。 */
    private int shortEmploymentMonthsUpper = 12;
    /** 在职月数过短加分。 */
    private int scoreThinEmployment = 15;
    /** 在职月数较短加分。 */
    private int scoreShortEmployment = 8;

    /** 近期查询次数突增阈值。 */
    private int inquirySpikeCount = 6;
    /** 查询突增加分。 */
    private int scoreInquirySpike = 12;

    /** 存量贷款过多阈值。 */
    private int tooManyLoansCount = 5;
    /** 存量贷款过多加分。 */
    private int scoreTooManyLoans = 10;

    /** 30 天轻微逾期次数阈值。 */
    private int mildDelinquency30dCount = 2;
    /** 30 天轻微逾期加分。 */
    private int scoreMildDelinquency30d = 10;

    /** 申请金额/年收入高风险比例阈值。 */
    private double highRequestToIncomeRatio = 1.2;
    /** 申请金额/年收入中风险比例阈值。 */
    private double mediumRequestToIncomeRatio = 0.8;
    /** 高比例加分。 */
    private int scoreHighRequestToIncome = 20;
    /** 中比例加分。 */
    private int scoreMediumRequestToIncome = 10;

    /** 强资质用户信用分下界。 */
    private int strongApplicantCreditMin = 760;
    /** 强资质用户 DTI 上界。 */
    private double strongApplicantDtiMax = 0.35;
    /** 强资质用户收入下界。 */
    private double strongApplicantIncomeMin = 20000;
    /** 强资质奖励分（负数表示减风险）。 */
    private int scoreStrongApplicantBonus = -10;

    /** 线下可信渠道信用分下界。 */
    private int offlineTrustedCreditMin = 740;
    /** 线下可信渠道 DTI 上界。 */
    private double offlineTrustedDtiMax = 0.35;
    /** 线下可信渠道奖励分。 */
    private int scoreOfflineTrustedBonus = -8;

    /** 拒绝阈值：风险分 >= 该值则拒绝。 */
    private int rejectScoreThreshold = 70;
    /** 人审阈值：风险分 >= 该值进入人审（且 < 拒绝阈值）。 */
    private int manualReviewScoreThreshold = 40;

    // 以下 getter/setter 为字段一一对应访问器，供：
    // 1) 配置加载器写入
    // 2) DRL 规则读取（MVEL/Java 反射调用）
    public int getMinAge() { return minAge; }
    public void setMinAge(int minAge) { this.minAge = minAge; }
    public int getSevereDelinquency90dRejectCount() { return severeDelinquency90dRejectCount; }
    public void setSevereDelinquency90dRejectCount(int severeDelinquency90dRejectCount) { this.severeDelinquency90dRejectCount = severeDelinquency90dRejectCount; }
    public int getHighRiskInquiryRejectCount() { return highRiskInquiryRejectCount; }
    public void setHighRiskInquiryRejectCount(int highRiskInquiryRejectCount) { this.highRiskInquiryRejectCount = highRiskInquiryRejectCount; }
    public int getCreditScoreVeryLowUpper() { return creditScoreVeryLowUpper; }
    public void setCreditScoreVeryLowUpper(int creditScoreVeryLowUpper) { this.creditScoreVeryLowUpper = creditScoreVeryLowUpper; }
    public int getCreditScoreLowUpper() { return creditScoreLowUpper; }
    public void setCreditScoreLowUpper(int creditScoreLowUpper) { this.creditScoreLowUpper = creditScoreLowUpper; }
    public int getCreditScoreMediumUpper() { return creditScoreMediumUpper; }
    public void setCreditScoreMediumUpper(int creditScoreMediumUpper) { this.creditScoreMediumUpper = creditScoreMediumUpper; }
    public int getCreditScoreExcellentMin() { return creditScoreExcellentMin; }
    public void setCreditScoreExcellentMin(int creditScoreExcellentMin) { this.creditScoreExcellentMin = creditScoreExcellentMin; }
    public int getScoreCreditVeryLow() { return scoreCreditVeryLow; }
    public void setScoreCreditVeryLow(int scoreCreditVeryLow) { this.scoreCreditVeryLow = scoreCreditVeryLow; }
    public int getScoreCreditLow() { return scoreCreditLow; }
    public void setScoreCreditLow(int scoreCreditLow) { this.scoreCreditLow = scoreCreditLow; }
    public int getScoreCreditMedium() { return scoreCreditMedium; }
    public void setScoreCreditMedium(int scoreCreditMedium) { this.scoreCreditMedium = scoreCreditMedium; }
    public int getScoreCreditExcellentBonus() { return scoreCreditExcellentBonus; }
    public void setScoreCreditExcellentBonus(int scoreCreditExcellentBonus) { this.scoreCreditExcellentBonus = scoreCreditExcellentBonus; }
    public double getDtiHighThreshold() { return dtiHighThreshold; }
    public void setDtiHighThreshold(double dtiHighThreshold) { this.dtiHighThreshold = dtiHighThreshold; }
    public double getDtiMediumThreshold() { return dtiMediumThreshold; }
    public void setDtiMediumThreshold(double dtiMediumThreshold) { this.dtiMediumThreshold = dtiMediumThreshold; }
    public int getScoreDtiHigh() { return scoreDtiHigh; }
    public void setScoreDtiHigh(int scoreDtiHigh) { this.scoreDtiHigh = scoreDtiHigh; }
    public int getScoreDtiMedium() { return scoreDtiMedium; }
    public void setScoreDtiMedium(int scoreDtiMedium) { this.scoreDtiMedium = scoreDtiMedium; }
    public int getThinEmploymentMonthsUpper() { return thinEmploymentMonthsUpper; }
    public void setThinEmploymentMonthsUpper(int thinEmploymentMonthsUpper) { this.thinEmploymentMonthsUpper = thinEmploymentMonthsUpper; }
    public int getShortEmploymentMonthsUpper() { return shortEmploymentMonthsUpper; }
    public void setShortEmploymentMonthsUpper(int shortEmploymentMonthsUpper) { this.shortEmploymentMonthsUpper = shortEmploymentMonthsUpper; }
    public int getScoreThinEmployment() { return scoreThinEmployment; }
    public void setScoreThinEmployment(int scoreThinEmployment) { this.scoreThinEmployment = scoreThinEmployment; }
    public int getScoreShortEmployment() { return scoreShortEmployment; }
    public void setScoreShortEmployment(int scoreShortEmployment) { this.scoreShortEmployment = scoreShortEmployment; }
    public int getInquirySpikeCount() { return inquirySpikeCount; }
    public void setInquirySpikeCount(int inquirySpikeCount) { this.inquirySpikeCount = inquirySpikeCount; }
    public int getScoreInquirySpike() { return scoreInquirySpike; }
    public void setScoreInquirySpike(int scoreInquirySpike) { this.scoreInquirySpike = scoreInquirySpike; }
    public int getTooManyLoansCount() { return tooManyLoansCount; }
    public void setTooManyLoansCount(int tooManyLoansCount) { this.tooManyLoansCount = tooManyLoansCount; }
    public int getScoreTooManyLoans() { return scoreTooManyLoans; }
    public void setScoreTooManyLoans(int scoreTooManyLoans) { this.scoreTooManyLoans = scoreTooManyLoans; }
    public int getMildDelinquency30dCount() { return mildDelinquency30dCount; }
    public void setMildDelinquency30dCount(int mildDelinquency30dCount) { this.mildDelinquency30dCount = mildDelinquency30dCount; }
    public int getScoreMildDelinquency30d() { return scoreMildDelinquency30d; }
    public void setScoreMildDelinquency30d(int scoreMildDelinquency30d) { this.scoreMildDelinquency30d = scoreMildDelinquency30d; }
    public double getHighRequestToIncomeRatio() { return highRequestToIncomeRatio; }
    public void setHighRequestToIncomeRatio(double highRequestToIncomeRatio) { this.highRequestToIncomeRatio = highRequestToIncomeRatio; }
    public double getMediumRequestToIncomeRatio() { return mediumRequestToIncomeRatio; }
    public void setMediumRequestToIncomeRatio(double mediumRequestToIncomeRatio) { this.mediumRequestToIncomeRatio = mediumRequestToIncomeRatio; }
    public int getScoreHighRequestToIncome() { return scoreHighRequestToIncome; }
    public void setScoreHighRequestToIncome(int scoreHighRequestToIncome) { this.scoreHighRequestToIncome = scoreHighRequestToIncome; }
    public int getScoreMediumRequestToIncome() { return scoreMediumRequestToIncome; }
    public void setScoreMediumRequestToIncome(int scoreMediumRequestToIncome) { this.scoreMediumRequestToIncome = scoreMediumRequestToIncome; }
    public int getStrongApplicantCreditMin() { return strongApplicantCreditMin; }
    public void setStrongApplicantCreditMin(int strongApplicantCreditMin) { this.strongApplicantCreditMin = strongApplicantCreditMin; }
    public double getStrongApplicantDtiMax() { return strongApplicantDtiMax; }
    public void setStrongApplicantDtiMax(double strongApplicantDtiMax) { this.strongApplicantDtiMax = strongApplicantDtiMax; }
    public double getStrongApplicantIncomeMin() { return strongApplicantIncomeMin; }
    public void setStrongApplicantIncomeMin(double strongApplicantIncomeMin) { this.strongApplicantIncomeMin = strongApplicantIncomeMin; }
    public int getScoreStrongApplicantBonus() { return scoreStrongApplicantBonus; }
    public void setScoreStrongApplicantBonus(int scoreStrongApplicantBonus) { this.scoreStrongApplicantBonus = scoreStrongApplicantBonus; }
    public int getOfflineTrustedCreditMin() { return offlineTrustedCreditMin; }
    public void setOfflineTrustedCreditMin(int offlineTrustedCreditMin) { this.offlineTrustedCreditMin = offlineTrustedCreditMin; }
    public double getOfflineTrustedDtiMax() { return offlineTrustedDtiMax; }
    public void setOfflineTrustedDtiMax(double offlineTrustedDtiMax) { this.offlineTrustedDtiMax = offlineTrustedDtiMax; }
    public int getScoreOfflineTrustedBonus() { return scoreOfflineTrustedBonus; }
    public void setScoreOfflineTrustedBonus(int scoreOfflineTrustedBonus) { this.scoreOfflineTrustedBonus = scoreOfflineTrustedBonus; }
    public int getRejectScoreThreshold() { return rejectScoreThreshold; }
    public void setRejectScoreThreshold(int rejectScoreThreshold) { this.rejectScoreThreshold = rejectScoreThreshold; }
    public int getManualReviewScoreThreshold() { return manualReviewScoreThreshold; }
    public void setManualReviewScoreThreshold(int manualReviewScoreThreshold) { this.manualReviewScoreThreshold = manualReviewScoreThreshold; }

    /**
     * 配置校验：
     * 在系统启动或热加载阶段先校验参数关系，防止非法阈值上线。
     */
    public void validate() {
        // 人审阈值必须小于拒绝阈值，否则区间会重叠或反转
        if (manualReviewScoreThreshold >= rejectScoreThreshold) {
            throw new IllegalArgumentException("manualReviewScoreThreshold must be smaller than rejectScoreThreshold");
        }
        // 中比例阈值必须小于高比例阈值，保证区间正确
        if (mediumRequestToIncomeRatio >= highRequestToIncomeRatio) {
            throw new IllegalArgumentException("mediumRequestToIncomeRatio must be smaller than highRequestToIncomeRatio");
        }
        // 信用分分层必须严格递增
        if (creditScoreVeryLowUpper >= creditScoreLowUpper || creditScoreLowUpper >= creditScoreMediumUpper) {
            throw new IllegalArgumentException("credit score ladder is invalid");
        }
    }
}
