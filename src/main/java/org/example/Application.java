package org.example;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 贷款申请事实对象（Fact）。
 *
 * 说明：
 * - 该对象会被插入 Drools Session 作为规则匹配输入
 * - 规则执行过程中会更新 status/riskScore/reason/ruleHits 等字段
 * - 该类同时承担“可解释输出”职责，便于排查决策路径
 */
public class Application {
    /** 申请编号。 */
    private String appId;
    /** 年龄。 */
    private int age;
    /** 月收入。 */
    private double income;
    /** 申请金额。 */
    private double requestedAmount;
    /** 征信分。 */
    private int creditScore;
    /** 负债收入比（Debt To Income）。 */
    private double debtToIncomeRatio;
    /** 当前在职时长（月）。 */
    private int employmentMonths;
    /** 当前存量贷款笔数。 */
    private int existingLoanCount;
    /** 近期查询次数。 */
    private int recentInquiryCount;
    /** 30 天轻微逾期次数。 */
    private int pastDue30dCount;
    /** 90 天严重逾期次数。 */
    private int pastDue90dCount;
    /** 是否命中设备黑名单。 */
    private boolean blacklistedDevice;
    /** IP 风险等级（LOW/MEDIUM/HIGH）。 */
    private String ipRiskLevel;
    /** 进件渠道（APP/WEB/OFFLINE）。 */
    private String channel;

    /** 决策状态：PENDING / APPROVED / MANUAL_REVIEW / REJECTED。 */
    private String status = "PENDING";
    /** 累计风险分（越高风险越大）。 */
    private int riskScore = 0;
    /** 拒绝原因，可拼接多个。 */
    private String rejectReason = "";
    /** 人审原因，可拼接多个。 */
    private String manualReviewReason = "";
    /** 命中轨迹，用于解释“为什么是这个结果”。 */
    private final List<String> ruleHits = new ArrayList<>();
    /** 已触发规则编码，防止 update 后重复触发同一规则。 */
    private final Set<String> firedRuleCodes = new HashSet<>();

    /**
     * 精简构造函数：
     * 只传最常用字段，其余风控字段使用默认值。
     */
    public Application(String appId, int age, double income) {
        this(
                appId, age, income,
                income * 4,
                680,
                0.35,
                24,
                1,
                1,
                0,
                0,
                false,
                "LOW",
                "APP"
        );
    }

    /**
     * 完整构造函数：用于映射层将特征全部注入。
     */
    public Application(
            String appId,
            int age,
            double income,
            double requestedAmount,
            int creditScore,
            double debtToIncomeRatio,
            int employmentMonths,
            int existingLoanCount,
            int recentInquiryCount,
            int pastDue30dCount,
            int pastDue90dCount,
            boolean blacklistedDevice,
            String ipRiskLevel,
            String channel
    ) {
        this.appId = appId;
        this.age = age;
        this.income = income;
        this.requestedAmount = requestedAmount;
        this.creditScore = creditScore;
        this.debtToIncomeRatio = debtToIncomeRatio;
        this.employmentMonths = employmentMonths;
        this.existingLoanCount = existingLoanCount;
        this.recentInquiryCount = recentInquiryCount;
        this.pastDue30dCount = pastDue30dCount;
        this.pastDue90dCount = pastDue90dCount;
        this.blacklistedDevice = blacklistedDevice;
        this.ipRiskLevel = normalize(ipRiskLevel, "LOW");
        this.channel = normalize(channel, "APP");
        this.status = "PENDING";
    }

    /**
     * 字符串字段标准化：
     * - 空值使用兜底值
     * - 去掉首尾空白
     * - 统一转大写，避免规则侧大小写不一致
     */
    private String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim().toUpperCase();
    }

    public String getAppId() { return appId; }
    public void setAppId(String appId) { this.appId = appId; }
    public int getAge() { return age; }
    public double getIncome() { return income; }
    public double getRequestedAmount() { return requestedAmount; }
    public int getCreditScore() { return creditScore; }
    public double getDebtToIncomeRatio() { return debtToIncomeRatio; }
    public int getEmploymentMonths() { return employmentMonths; }
    public int getExistingLoanCount() { return existingLoanCount; }
    public int getRecentInquiryCount() { return recentInquiryCount; }
    public int getPastDue30dCount() { return pastDue30dCount; }
    public int getPastDue90dCount() { return pastDue90dCount; }
    public boolean isBlacklistedDevice() { return blacklistedDevice; }
    public String getIpRiskLevel() { return ipRiskLevel; }
    public String getChannel() { return channel; }
    public String getStatus() { return status; }
    public int getRiskScore() { return riskScore; }
    public String getRejectReason() { return rejectReason; }
    public String getManualReviewReason() { return manualReviewReason; }
    public List<String> getRuleHits() { return Collections.unmodifiableList(ruleHits); }

    /** 直接设置状态。一般由规则层调用。 */
    public void setStatus(String status) { this.status = status; }
    /** 追加拒绝原因。 */
    public void setRejectReason(String reason) {
        this.rejectReason = appendText(this.rejectReason, reason);
    }
    /** 追加人审原因。 */
    public void setManualReviewReason(String reason) {
        this.manualReviewReason = appendText(this.manualReviewReason, reason);
    }

    /**
     * 文本拼接工具：
     * - 当前为空：直接写入
     * - 当前非空：使用 "; " 拼接
     */
    private String appendText(String current, String incoming) {
        if (incoming == null || incoming.isBlank()) {
            return current == null ? "" : current;
        }
        if (current == null || current.isBlank()) {
            return incoming;
        }
        return current + "; " + incoming;
    }

    /**
     * 风险分加减，并记录轨迹。
     */
    public void addRiskScore(int delta, String reason) {
        this.riskScore += delta;
        addRuleHit("score " + (delta >= 0 ? "+" : "") + delta + ": " + reason);
    }

    /**
     * 追加命中轨迹。
     */
    public void addRuleHit(String message) {
        if (message != null && !message.isBlank()) {
            this.ruleHits.add(message);
        }
    }

    /**
     * 判断某条规则是否已触发（用于去重）。
     */
    public boolean hasRuleFired(String ruleCode) {
        return firedRuleCodes.contains(ruleCode);
    }

    /**
     * 标记某条规则已触发。
     */
    public void markRuleFired(String ruleCode) {
        if (ruleCode != null && !ruleCode.isBlank()) {
            firedRuleCodes.add(ruleCode);
        }
    }

    /**
     * 执行硬拒绝。
     * 该方法会直接修改状态为 REJECTED，并写入拒绝轨迹。
     */
    public void reject(String reason) {
        this.status = "REJECTED";
        setRejectReason(reason);
        addRuleHit("hard reject: " + reason);
    }

    /**
     * 输出用于日志与调试的紧凑字符串，包含：
     * - 状态
     * - 风险分
     * - 拒绝/人审原因
     * - 命中轨迹
     */
    @Override
    public String toString() {
        return "App[" + appId + "] status=" + status +
                ", riskScore=" + riskScore +
                (rejectReason.isEmpty() ? "" : ", rejectReason=" + rejectReason) +
                (manualReviewReason.isEmpty() ? "" : ", manualReviewReason=" + manualReviewReason) +
                ", hits=" + ruleHits;
    }
}
