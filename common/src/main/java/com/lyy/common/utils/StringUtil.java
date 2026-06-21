package com.lyy.common.utils;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 字符串工具类
 * 包含：判空、去空格、截取、类型转换、占位符替换等
 */
public class StringUtil {

    private static final Pattern NUMBER_PATTERN = Pattern.compile("-?\\d+(\\.\\d+)?");

    // ==================== 判空 ====================
    public static boolean isBlank(CharSequence str) {
        int len;
        if (str == null || (len = str.length()) == 0) return true;
        for (int i = 0; i < len; i++) {
            if (!Character.isWhitespace(str.charAt(i))) return false;
        }
        return true;
    }

    public static boolean isNotBlank(CharSequence str) {
        return !isBlank(str);
    }

    // ==================== 去空格 / 截取 ====================
    public static String trimToNull(String str) {
        if (isBlank(str)) return null;
        return str.trim();
    }

    public static String trim(String str) {
        return str == null ? null : str.trim();
    }

    public static String substring(String str, int start, int end) {
        if (str == null) return null;
        if (start < 0) start = 0;
        if (end > str.length()) end = str.length();
        if (start >= end) return "";
        return str.substring(start, end);
    }

    // ==================== 字符串与基本类型转换 ====================
    public static Integer toInteger(String str) {
        if (isBlank(str)) return null;
        try {
            return Integer.parseInt(str.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Long toLong(String str) {
        if (isBlank(str)) return null;
        try {
            return Long.parseLong(str.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Double toDouble(String str) {
        if (isBlank(str)) return null;
        try {
            return Double.parseDouble(str.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Boolean toBoolean(String str) {
        if (isBlank(str)) return null;
        String s = str.trim().toLowerCase();
        if ("true".equals(s) || "1".equals(s)) return true;
        if ("false".equals(s) || "0".equals(s)) return false;
        return null;
    }

    /**
     * 字符串转 Date（支持自定义格式）
     */
    public static Date toDate(String dateStr, String pattern) {
        if (isBlank(dateStr) || isBlank(pattern)) return null;
        try {
            return new SimpleDateFormat(pattern).parse(dateStr.trim());
        } catch (ParseException e) {
            return null;
        }
    }

    public static Date toDateTime(String dateStr) {
        return toDate(dateStr, "yyyy-MM-dd HH:mm:ss");
    }

    public static Date toDateOnly(String dateStr) {
        return toDate(dateStr, "yyyy-MM-dd");
    }

    // ==================== 编码 / 格式化 ====================
    public static byte[] toBytes(String str) {
        if (str == null) return null;
        return str.getBytes(StandardCharsets.UTF_8);
    }

    public static String fromBytes(byte[] bytes) {
        if (bytes == null) return null;
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * 判断字符串是否为数字（整数或小数）
     */
    public static boolean isNumeric(String str) {
        if (isBlank(str)) return false;
        return NUMBER_PATTERN.matcher(str.trim()).matches();
    }

    /**
     * 首字母大写
     */
    public static String capitalize(String str) {
        if (isBlank(str)) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    /**
     * 首字母小写
     */
    public static String uncapitalize(String str) {
        if (isBlank(str)) return str;
        return str.substring(0, 1).toLowerCase() + str.substring(1);
    }

    // ==================== 占位符替换（用于 Redis Key 等） ====================

    /**
     * 使用 Map 进行占位符替换，占位符格式为 {key}
     * 示例: format("user:{id}:video:{vid}", Map.of("id", 123, "vid", 456)) -> "user:123:video:456"
     *
     * @param template 包含占位符的字符串
     * @param params   占位符名 -> 值
     * @return 替换后的字符串，若 template 为 null 则返回 null
     */
    public static String format(String template, Map<String, Object> params) {
        if (template == null) return null;
        if (params == null || params.isEmpty()) return template;
        String result = template;
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            String value = entry.getValue() == null ? "" : entry.getValue().toString();
            result = result.replace(placeholder, value);
        }
        return result;
    }

    /**
     * 使用顺序参数替换占位符，占位符格式为 {0}, {1}, {2}...
     * 示例: format("progress:user:{0}:video:{1}", 123, 456) -> "progress:user:123:video:456"
     *
     * @param template 包含占位符的字符串
     * @param args     替换参数，按顺序对应 {0}, {1}...
     * @return 替换后的字符串
     */
    public static String format(String template, Object... args) {
        if (template == null) return null;
        if (args == null || args.length == 0) return template;
        String result = template;
        for (int i = 0; i < args.length; i++) {
            String placeholder = "{" + i + "}";
            String value = args[i] == null ? "" : args[i].toString();
            result = result.replace(placeholder, value);
        }
        return result;
    }

    /**
     * 快速生成 Redis Key（观看进度专用，展示扩展性）
     * 示例: redisKey("progress", "user", 123, "video", 456) -> "progress:user:123:video:456"
     *
     * @param parts 可变参数，自动用冒号连接
     * @return 拼接后的字符串
     */
    public static String joinByColon(Object... parts) {
        if (parts == null || parts.length == 0) return "";
        StringBuilder sb = new StringBuilder();
        for (Object p : parts) {
            if (p == null) continue;
            if (sb.length() > 0) sb.append(":");
            sb.append(p);
        }
        return sb.toString();
    }
}