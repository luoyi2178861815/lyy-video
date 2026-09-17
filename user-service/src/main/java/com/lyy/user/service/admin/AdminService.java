package com.lyy.user.service.admin;

import com.lyy.user.entity.dto.AdminLoginDTO;
import com.lyy.user.entity.vo.AdminLoginVO;

/**
 * 管理端账号服务
 */
public interface AdminService {

    /**
     * 管理员登录
     * @param dto 登录入参
     * @return 登录结果（含 Admin-Token）
     */
    AdminLoginVO login(AdminLoginDTO dto);
}
