package org.example;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * 特征字典加载器：
 * 仅从外部文件路径加载（项目根目录 config/ 下）。
 */
public final class FeatureDictionaryLoader {
    /**
     * 工具类不允许实例化。
     */
    private FeatureDictionaryLoader() {
    }

    /**
     * 加载特征字典。
     *
     * @param dictionaryPath 外部字典文件路径（必须存在）
     * @return 解析后的 FeatureDictionary
     */
    public static FeatureDictionary load(Path dictionaryPath) {
        Properties properties = new Properties();
        if (dictionaryPath == null || !Files.exists(dictionaryPath)) {
            throw new IllegalStateException("feature dictionary file is required: " + dictionaryPath);
        }
        try (InputStream in = Files.newInputStream(dictionaryPath)) {
            properties.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("failed to load feature dictionary from " + dictionaryPath, e);
        }

        // 将 Properties 转换为普通 Map，并做基础清洗（trim + blank 过滤）
        Map<String, String> aliasToCode = new HashMap<>();
        for (String alias : properties.stringPropertyNames()) {
            String code = properties.getProperty(alias);
            if (code != null && !code.isBlank()) {
                aliasToCode.put(alias.trim(), code.trim());
            }
        }
        return new FeatureDictionary(aliasToCode);
    }
}
