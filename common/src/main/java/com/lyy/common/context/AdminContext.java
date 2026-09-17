package com.lyy.common.context;

/**
 * 管理端上下文
 * 从网关注入的请求头还原当前登录管理员信息，供 Service 层读取。
 * 与 C 端的 BaseContext 隔离，避免两类身份互相污染。
 */
public class AdminContext {

    private static final ThreadLocal<Long> ADMIN_ID = new ThreadLocal<>();

    private static final ThreadLocal<String> ADMIN_NAME = new ThreadLocal<>();

    /** 写入当前管理员 ID */
    public static void setAdminId(Long adminId) {
        ADMIN_ID.set(adminId);
    }

    /** 获取当前管理员 ID，未登录返回 null */
    public static Long getAdminId() {
        return ADMIN_ID.get();
    }

    /** 写入当前管理员姓名 */
    public static void setAdminName(String adminName) {
        ADMIN_NAME.set(adminName);
    }

    /** 获取当前管理员姓名，未登录返回 null */
    public static String getAdminName() {
        return ADMIN_NAME.get();
    }

    /** 清理 ThreadLocal，必须在请求结束时调用，否则线程复用会串数据 */
    public static void remove() {
        ADMIN_ID.remove();
        ADMIN_NAME.remove();
    }
}
