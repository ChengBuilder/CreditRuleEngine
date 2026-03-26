package org.example;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;

/**
 * 控制台追踪工具：
 * - 默认开启（可通过 -Ddemo.trace=false 关闭）
 * - 统一日志格式，便于按阶段查看决策链路
 */
public final class Trace {
    private static final boolean ENABLED =
            Boolean.parseBoolean(System.getProperty("demo.trace", "true"));
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private Trace() {
    }

    public static boolean enabled() {
        return ENABLED;
    }

    public static void log(String stage, String message) {
        if (!ENABLED) {
            return;
        }
        String now = LocalDateTime.now().format(TIME_FORMAT);
        System.out.println("[TRACE][" + now + "][" + stage + "] " + message);
    }

    public static String map(Map<String, String> source) {
        if (source == null) {
            return "{}";
        }
        return new TreeMap<>(source).toString();
    }
}
