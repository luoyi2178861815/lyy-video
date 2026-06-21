package com.lyy.user.service;

import com.lyy.common.result.PageResult;
import com.lyy.user.entity.dto.AdminUserAddDTO;
import com.lyy.user.entity.dto.AdminUserUpdateDTO;
import com.lyy.user.entity.dto.UserUpdateDTO;
import com.lyy.user.entity.po.ExpRecord;
import com.lyy.user.entity.po.User;
import com.lyy.user.entity.vo.UserProfileVO;

import java.util.List;

public interface UserService {
    // ==================== C端业务 ====================
    boolean register(User user);
    User login(String username, String password);
    UserProfileVO getProfileById(Long id);
    void updateProfile(Long userId, UserUpdateDTO dto);
    void changePassword(Long userId, String oldPassword, String newPassword);
    void deleteAccount(Long userId);

    // ==================== B端业务 ====================
    PageResult getUserPage(int pageNum, int pageSize, String keyword);
    UserProfileVO getUserById(Long id);
    void addUser(AdminUserAddDTO dto);
    void updateUser(AdminUserUpdateDTO dto);
    void deleteUser(Long id);
    void changeUserStatus(Long userId, Integer status);

    // ==================== 内部调用 ====================
    List<UserProfileVO> getProfilesByIds(List<Long> ids);

    void incrementVideoLikeCount(Long videoId, Integer increment, Long userId);

    void incrementFollowCount(Long userId, Integer increment);

    void incrementFansCount(Long userId, Integer increment);

    // ==================== 经验相关 ====================
    void addExp(Long userId, int expValue, String reason);
    List<ExpRecord> getExpRecords(Long userId);
    Integer getUserLevel(Long userId);
    void updateRealNameVerified(Long userId);

}
