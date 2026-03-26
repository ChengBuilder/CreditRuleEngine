package org.example;

import java.util.Collections;
import java.util.Map;

/**
 * 特征字典对象：
 * 作用是维护“业务别名 -> 特征编码”的映射关系，避免在业务代码中硬编码特征码。
 *
 * 例如：
 * - age -> 100001001
 * - gender -> 110001001
 *
 * 在规则执行前，我们会先拿到原始特征 Map（key 是编码），
 * 再通过本字典按别名取值，这样规则侧始终使用业务语义字段。
 */
public class FeatureDictionary {
    /**
     * 别名到编码的不可变映射。
     * key：业务语义名（如 age、income）
     * value：特征编码（如 100001001）
     */
    private final Map<String, String> aliasToCode;

    /**
     * 构造函数会复制一份传入映射，并保证内部不可变，避免运行中被外部修改。
     */
    public FeatureDictionary(Map<String, String> aliasToCode) {
        this.aliasToCode = Map.copyOf(aliasToCode);
    }

    /**
     * 根据业务别名获取对应特征编码。
     *
     * @param alias 业务字段别名
     * @return 对应编码；找不到时返回 null
     */
    public String codeOf(String alias) {
        return aliasToCode.get(alias);
    }

    /**
     * 从原始特征中取值：
     * 1. 先用别名找到编码
     * 2. 再用编码到 rawFeatures 中取对应值
     *
     * @param rawFeatures 原始特征（key=编码，value=字符串值）
     * @param alias       业务字段别名
     * @return 对应特征值；找不到时返回 null
     */
    public String valueOf(Map<String, String> rawFeatures, String alias) {
        if (rawFeatures == null) {
            return null;
        }
        String code = codeOf(alias);
        if (code == null || code.isBlank()) {
            return null;
        }
        return rawFeatures.get(code);
    }

    /**
     * 返回只读视图，便于调试和观测当前字典内容。
     */
    public Map<String, String> getAliasToCode() {
        return Collections.unmodifiableMap(aliasToCode);
    }
}
