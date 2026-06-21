package com.lyy.user.service.impl;

import com.lyy.common.constant.MqConstant;
import com.lyy.common.dto.ExpMessage;
import com.lyy.common.exception.BusinessException;
import com.lyy.common.result.PageResult;
import com.lyy.user.entity.dto.AdminUserAddDTO;
import com.lyy.user.entity.dto.AdminUserUpdateDTO;
import com.lyy.user.entity.dto.UserUpdateDTO;
import com.lyy.user.entity.po.ExpRecord;
import com.lyy.user.entity.po.User;
import com.lyy.user.entity.vo.UserProfileVO;
import com.lyy.user.mapper.UserMapper;
import com.lyy.user.service.ExpService;
import com.lyy.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ExpService expService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    // ==================== C端业务 ====================

    @Override
    public boolean register(User user) {
        if (userMapper.selectByUsername(user.getUsername()) != null) {
            throw new BusinessException("该账号已被注册");
        }
        return userMapper.insert(user) > 0;
    }

    @Override
    public User login(String username, String password) {
        User user = userMapper.selectByUsername(username);
        if (user == null || !user.getPassword().equals(password)) {
            throw new BusinessException(401, "账号或密码错误");
        }
        if (user.getStatus() == 0) {
            throw new BusinessException(403, "您的账号已被封禁");
        }
        // 登录经验：发送 MQ 消息
        ExpMessage loginExpMsg = ExpMessage.of(user.getId(), 5, "daily_login");
        rabbitTemplate.convertAndSend(MqConstant.EXP_EXCHANGE, MqConstant.EXP_ROUTING_KEY, loginExpMsg);
        log.info("发送每日登录经验消息：userId={}", user.getId());
        return user;
    }

    @Override
    public UserProfileVO getProfileById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return toProfileVO(user);
    }

    @Override
    @Transactional
    public void updateProfile(Long userId, UserUpdateDTO dto) {
        User user = new User();
        user.setId(userId);
        BeanUtils.copyProperties(dto, user);
        int rows = userMapper.updateById(user);
        if (rows == 0) {
            throw new BusinessException("用户不存在");
        }
    }

    @Override
    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (!user.getPassword().equals(oldPassword)) {
            throw new BusinessException("旧密码不正确");
        }
        User update = new User();
        update.setId(userId);
        update.setPassword(newPassword);
        userMapper.updateById(update);
    }

    @Override
    @Transactional
    public void deleteAccount(Long userId) {
        int rows = userMapper.deleteById(userId);
        if (rows == 0) {
            throw new BusinessException("用户不存在");
        }
    }

    // ==================== B端业务 ====================

    @Override
    public PageResult getUserPage(int pageNum, int pageSize, String keyword) {
        int offset = (pageNum - 1) * pageSize;
        List<User> users = userMapper.selectUserList(offset, pageSize, keyword);
        int total = userMapper.selectUserCount(keyword);
        List<UserProfileVO> records = users.stream()
                .map(this::toProfileVO)
                .collect(Collectors.toList());
        return new PageResult(total, records);
    }

    @Override
    public UserProfileVO getUserById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return toProfileVO(user);
    }

    @Override
    @Transactional
    public void addUser(AdminUserAddDTO dto) {
        if (userMapper.selectByUsername(dto.getUsername()) != null) {
            throw new BusinessException("该账号已被注册");
        }
        User user = new User();
        BeanUtils.copyProperties(dto, user);
        userMapper.insert(user);
    }

    @Override
    @Transactional
    public void updateUser(AdminUserUpdateDTO dto) {
        User existing = userMapper.selectById(dto.getId());
        if (existing == null) {
            throw new BusinessException("用户不存在");
        }
        User user = new User();
        BeanUtils.copyProperties(dto, user);
        userMapper.updateById(user);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        int rows = userMapper.deleteById(id);
        if (rows == 0) {
            throw new BusinessException("用户不存在");
        }
    }

    @Override
    @Transactional
    public void changeUserStatus(Long userId, Integer status) {
        User user = new User();
        user.setId(userId);
        user.setStatus(status);
        int rows = userMapper.updateById(user);
        if (rows == 0) {
            throw new BusinessException("用户不存在");
        }
    }

    // ==================== 内部调用 ====================

    @Override
    public List<UserProfileVO> getProfilesByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<User> users = userMapper.selectByIds(ids);
        return users.stream().map(this::toProfileVO).collect(Collectors.toList());
    }

    //处理由视频服务点赞的用户点赞数更新
    @Override
    public void incrementVideoLikeCount(Long videoId, Integer increment, Long userId) {
        log.info("收到点赞数更新消息：videoId={}, increment={}, userId={}", videoId, increment, userId);
        userMapper.updateUserLikeCount(userId, increment);

    }

    @Override
    public void incrementFollowCount(Long userId, Integer increment) {
        log.info("更新用户 {} 的关注数，增量 {}", userId, increment);
        userMapper.updateFollowCount(userId, increment);
    }

    @Override
    public void incrementFansCount(Long userId, Integer increment) {
        log.info("更新用户 {} 的粉丝数，增量 {}", userId, increment);
        userMapper.updateFansCount(userId, increment);
    }

    // ==================== 经验相关 ====================

    @Override
    public void addExp(Long userId, int expValue, String reason) {
        expService.addExp(userId, expValue, reason);
    }

    @Override
    public List<ExpRecord> getExpRecords(Long userId) {
        return expService.getRecentRecords(userId);
    }

    @Override
    public Integer getUserLevel(Long userId) {
        return userMapper.selectLevelById(userId);
    }

    @Override
    public void updateRealNameVerified(Long userId) {
        User update = new User();
        update.setId(userId);
        update.setRealNameVerified(1);
        userMapper.updateById(update);
    }

    // ==================== 工具方法 ====================

    private UserProfileVO toProfileVO(User user) {
        return UserProfileVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .sign(user.getSign())
                .phone(user.getPhone())
                .status(user.getStatus())
                .level(user.getLevel())
                .exp(user.getExp())
                .followCount(user.getFollowCount())
                .fansCount(user.getFansCount())
                .likeTotal(user.getLikeTotal())
                .playTotal(user.getPlayTotal())
                .coinCount(user.getCoinCount())
                .email(user.getEmail())
                .realNameVerified(user.getRealNameVerified())
                .createTime(user.getCreateTime())
                .updateTime(user.getUpdateTime())
                .build();
    }
}
