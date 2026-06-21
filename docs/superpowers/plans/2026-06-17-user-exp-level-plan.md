# 用户经验值 & 等级系统 实施计划

> **面向 agentic workers：** 推荐使用 superpowers:subagent-driven-development 或 superpowers:executing-plans 按任务逐步实施。步骤使用 `- [ ]` 复选框语法跟踪进度。

**目标：** 实现完整的用户经验值获取与等级成长体系（Lv0-Lv6），包含每日任务追踪、MQ 异步经验发放、Lv0 权限限制。

**架构：** 采用事件驱动模式——video-service 和 interaction-service 通过 RabbitMQ 发送经验消息，user-service 的 ExpConsumer 统一消费处理。经验逻辑、每日上限检查、等级计算全部集中在 user-service 内部。

**技术栈：** Java 17, Spring Boot 3.2.5, Spring Cloud 2023.0.1, MyBatis, RabbitMQ, MySQL

---

### Task 1: 数据库变更 SQL

**文件：**
- Create: `lyy-video/user-service/src/main/resources/sql/exp-upgrade.sql`

- [ ] **Step 1: 编写数据库迁移 SQL**

```sql
-- 用户经验值 & 等级系统 DDL
-- user 表新增字段
ALTER TABLE `user`
  ADD COLUMN `email` VARCHAR(100) DEFAULT '' COMMENT '邮箱',
  ADD COLUMN `real_name_verified` TINYINT DEFAULT 0 COMMENT '实名认证: 0未认证 1已认证';

-- 经验记录表
CREATE TABLE IF NOT EXISTS `user_exp_record` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `exp_value` INT NOT NULL COMMENT '经验变化量',
    `reason` VARCHAR(50) NOT NULL COMMENT '来源: daily_login/daily_watch/daily_coin/daily_share/bind_email/bind_phone/real_name',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_user_time` (`user_id`, `create_time`)
) COMMENT '用户经验记录表，每个用户保留最近30条';
```

- [ ] **Step 2: 执行 SQL**

在 MySQL `db_user` 库中执行以上 SQL。

---

### Task 2: Common 模块 — ExpMessage DTO + MqConstant 扩展

**文件：**
- Create: `lyy-video/common/src/main/java/com/lyy/common/dto/ExpMessage.java`
- Modify: `lyy-video/common/src/main/java/com/lyy/common/constant/MqConstant.java`

- [ ] **Step 1: 创建 ExpMessage 消息体**

```java
package com.lyy.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long userId;
    private Integer expValue;
    private String reason;
    private LocalDateTime timestamp;

    public static ExpMessage of(Long userId, Integer expValue, String reason) {
        ExpMessage msg = new ExpMessage();
        msg.userId = userId;
        msg.expValue = expValue;
        msg.reason = reason;
        msg.timestamp = LocalDateTime.now();
        return msg;
    }
}
```

- [ ] **Step 2: MqConstant 新增经验相关常量**

在 `MqConstant.java` 文件末尾新增以下常量：

```java
//    经验
public static final String EXP_EXCHANGE = "exp.direct";
public static final String EXP_QUEUE = "exp.queue";
public static final String EXP_ROUTING_KEY = "exp.add";
```

- [ ] **Step 3: 编译验证**

```bash
cd lyy-video && mvn clean install -pl common -DskipTests
```

---

### Task 3: User 实体 + DTO + VO 字段扩展

**文件：**
- Modify: `lyy-video/user-service/src/main/java/com/lyy/user/entity/po/User.java`
- Modify: `lyy-video/user-service/src/main/java/com/lyy/user/entity/vo/UserProfileVO.java`
- Modify: `lyy-video/user-service/src/main/java/com/lyy/user/entity/dto/AdminUserUpdateDTO.java`
- Modify: `lyy-video/user-service/src/main/java/com/lyy/user/entity/dto/AdminUserAddDTO.java`
- Modify: `lyy-video/user-service/src/main/java/com/lyy/user/entity/dto/UserUpdateDTO.java`

- [ ] **Step 1: User.java 新增字段**

在 `User.java` 的类末尾追加两个字段（放在 `coinCount` 字段之后、`createTime` 之前）：

```java
@Schema(description = "邮箱")
private String email;

@Schema(description = "实名认证: 0未认证 1已认证")
private Integer realNameVerified;
```

- [ ] **Step 2: UserProfileVO.java 新增字段**

在 `UserProfileVO.java` 末尾追加：

```java
private String email;
private Integer realNameVerified;
```

- [ ] **Step 3: AdminUserUpdateDTO.java 新增字段**

在 `AdminUserUpdateDTO.java` 末尾追加：

```java
private String email;
private Integer realNameVerified;
```

- [ ] **Step 4: AdminUserAddDTO.java 新增字段**

在 `AdminUserAddDTO.java` 末尾追加：

```java
private String email;
```

- [ ] **Step 5: UserUpdateDTO.java 新增字段**

在 `UserUpdateDTO.java` 末尾追加：

```java
private String email;
```

- [ ] **Step 6: 编译验证**

```bash
cd lyy-video && mvn clean install -pl common,user-service -DskipTests
```

---

### Task 4: UserMapper + XML 更新

**文件：**
- Modify: `lyy-video/user-service/src/main/java/com/lyy/user/mapper/UserMapper.java`
- Modify: `lyy-video/user-service/src/main/resources/mapper/UserMapper.xml`

- [ ] **Step 1: UserMapper.java 新增方法**

在 `UserMapper.java` 末尾追加：

```java
void incrementExp(@Param("userId") Long userId,
                  @Param("increment") Integer increment);

Integer selectLevelById(@Param("userId") Long userId);
```

- [ ] **Step 2: UserMapper.xml 更新 Base_Column_List**

将 `<sql id="Base_Column_List">` 修改为包含新字段：

```xml
<sql id="Base_Column_List">
    id, username, password, nickname, avatar, sign, phone, status,
    level, exp, follow_count, fans_count, like_total, play_total, coin_count,
    email, real_name_verified, create_time, update_time
</sql>
```

- [ ] **Step 3: UserMapper.xml 修改 INSERT 语句**

将 INSERT 语句中的默认值 `level` 从 `1` 改为 `0`，新增 `email` 字段：

```xml
<insert id="insert" useGeneratedKeys="true" keyProperty="id">
    INSERT INTO user (username, password, nickname, avatar, sign, phone, status, level, exp, coin_count, email)
    VALUES (#{username}, #{password}, #{nickname}, #{avatar}, #{sign}, #{phone}, 1, 0, 0, 0, #{email})
</insert>
```

- [ ] **Step 4: UserMapper.xml 中 updateById 新增 email 条件**

在 `updateById` 的 `<set>` 标签中，`phone` 条件之后追加：

```xml
<if test="email != null and email != ''">email = #{email},</if>
```

- [ ] **Step 5: UserMapper.xml 新增 incrementExp 和 selectLevelById SQL**

在文件末尾 `</mapper>` 之前追加：

```xml
<update id="incrementExp">
    UPDATE user
    SET exp = GREATEST(0, exp + #{increment}),
        update_time = NOW()
    WHERE id = #{userId}
</update>

<select id="selectLevelById" resultType="int">
    SELECT level FROM user WHERE id = #{userId}
</select>
```

- [ ] **Step 6: 编译验证**

```bash
cd lyy-video && mvn clean install -pl user-service -DskipTests
```

---

### Task 5: ExpRecord 实体 + Mapper + XML

**文件：**
- Create: `lyy-video/user-service/src/main/java/com/lyy/user/entity/po/ExpRecord.java`
- Create: `lyy-video/user-service/src/main/java/com/lyy/user/mapper/ExpRecordMapper.java`
- Create: `lyy-video/user-service/src/main/resources/mapper/ExpRecordMapper.xml`

- [ ] **Step 1: 创建 ExpRecord 实体**

```java
package com.lyy.user.entity.po;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpRecord implements Serializable {

    private Long id;
    private Long userId;
    private Integer expValue;
    private String reason;
    private LocalDateTime createTime;
}
```

- [ ] **Step 2: 创建 ExpRecordMapper 接口**

```java
package com.lyy.user.mapper;

import com.lyy.user.entity.po.ExpRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ExpRecordMapper {

    int insert(ExpRecord record);

    List<ExpRecord> selectByUserId(@Param("userId") Long userId,
                                   @Param("limit") int limit);

    /** 统计用户当天某个 reason 的记录数 */
    int countTodayByReason(@Param("userId") Long userId,
                           @Param("reason") String reason);

    /** 统计用户所有时间某个 reason 的记录数（用于首次绑定判断） */
    int countTotalByReason(@Param("userId") Long userId,
                           @Param("reason") String reason);

    /** 删除超出30条的旧记录 */
    int deleteOldRecords(@Param("userId") Long userId,
                         @Param("keepCount") int keepCount);
}
```

- [ ] **Step 3: 创建 ExpRecordMapper.xml**

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.lyy.user.mapper.ExpRecordMapper">

    <insert id="insert" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO user_exp_record (user_id, exp_value, reason)
        VALUES (#{userId}, #{expValue}, #{reason})
    </insert>

    <select id="selectByUserId" resultType="com.lyy.user.entity.po.ExpRecord">
        SELECT id, user_id, exp_value, reason, create_time
        FROM user_exp_record
        WHERE user_id = #{userId}
        ORDER BY create_time DESC
        LIMIT #{limit}
    </select>

    <select id="countTodayByReason" resultType="int">
        SELECT COUNT(*)
        FROM user_exp_record
        WHERE user_id = #{userId}
          AND reason = #{reason}
          AND DATE(create_time) = CURDATE()
    </select>

    <select id="countTotalByReason" resultType="int">
        SELECT COUNT(*)
        FROM user_exp_record
        WHERE user_id = #{userId}
          AND reason = #{reason}
    </select>

    <delete id="deleteOldRecords">
        DELETE FROM user_exp_record
        WHERE user_id = #{userId}
          AND id NOT IN (
              SELECT id FROM (
                  SELECT id FROM user_exp_record
                  WHERE user_id = #{userId}
                  ORDER BY create_time DESC
                  LIMIT #{keepCount}
              ) AS tmp
          )
    </delete>

</mapper>
```

- [ ] **Step 4: 编译验证**

```bash
cd lyy-video && mvn clean install -pl user-service -DskipTests
```

---

### Task 6: ExpService 核心经验逻辑

**文件：**
- Create: `lyy-video/user-service/src/main/java/com/lyy/user/service/ExpService.java`
- Create: `lyy-video/user-service/src/main/java/com/lyy/user/service/impl/ExpServiceImpl.java`

- [ ] **Step 1: 创建 ExpService 接口**

```java
package com.lyy.user.service;

import com.lyy.user.entity.po.ExpRecord;

import java.util.List;

public interface ExpService {

    /** 添加经验值，内部处理每日上限、首次绑定、等级升级 */
    void addExp(Long userId, int expValue, String reason);

    /** 查询用户最近经验记录 */
    List<ExpRecord> getRecentRecords(Long userId);
}
```

- [ ] **Step 2: 创建 ExpServiceImpl 实现**

```java
package com.lyy.user.service.impl;

import com.lyy.common.exception.BusinessException;
import com.lyy.user.entity.po.ExpRecord;
import com.lyy.user.entity.po.User;
import com.lyy.user.mapper.ExpRecordMapper;
import com.lyy.user.mapper.UserMapper;
import com.lyy.user.service.ExpService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class ExpServiceImpl implements ExpService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ExpRecordMapper expRecordMapper;

    /** 每日任务上限配置 */
    private static final int DAILY_LOGIN_MAX = 1;
    private static final int DAILY_WATCH_MAX = 1;
    private static final int DAILY_COIN_MAX = 5;
    private static final int DAILY_SHARE_MAX = 1;

    @Override
    @Transactional
    public void addExp(Long userId, int expValue, String reason) {
        // 1. 查询用户
        User user = userMapper.selectById(userId);
        if (user == null) {
            log.warn("经验发放失败，用户不存在：userId={}", userId);
            return;
        }

        // 2. 每日任务上限检查
        if (reason.startsWith("daily_")) {
            int todayCount = expRecordMapper.countTodayByReason(userId, reason);
            int max = getDailyMax(reason);
            if (todayCount >= max) {
                log.info("每日上限已达，userId={}, reason={}, todayCount={}, max={}",
                        userId, reason, todayCount, max);
                return;
            }
        }

        // 3. 首次绑定检查（永久一次）
        if (reason.startsWith("bind_") || reason.equals("real_name")) {
            int totalCount = expRecordMapper.countTotalByReason(userId, reason);
            if (totalCount > 0) {
                log.info("首次绑定已领取过，userId={}, reason={}", userId, reason);
                return;
            }
        }

        // 4. 更新经验值
        userMapper.incrementExp(userId, expValue);
        int newExp = user.getExp() + expValue;

        // 5. 检查升级
        int newLevel = calcLevel(newExp);
        if (newLevel > user.getLevel()) {
            User update = new User();
            update.setId(userId);
            update.setLevel(newLevel);
            userMapper.updateById(update);
            log.info("用户升级：userId={}, oldLevel={}, newLevel={}, newExp={}",
                    userId, user.getLevel(), newLevel, newExp);
        }

        // 6. 记录经验流水
        ExpRecord record = new ExpRecord();
        record.setUserId(userId);
        record.setExpValue(expValue);
        record.setReason(reason);
        expRecordMapper.insert(record);

        // 7. 清理旧记录（保留最近30条）
        expRecordMapper.deleteOldRecords(userId, 30);

        log.info("经验发放成功：userId={}, expValue={}, reason={}, totalExp={}",
                userId, expValue, reason, newExp);
    }

    @Override
    public List<ExpRecord> getRecentRecords(Long userId) {
        return expRecordMapper.selectByUserId(userId, 30);
    }

    private int getDailyMax(String reason) {
        return switch (reason) {
            case "daily_login" -> DAILY_LOGIN_MAX;
            case "daily_watch" -> DAILY_WATCH_MAX;
            case "daily_coin" -> DAILY_COIN_MAX;
            case "daily_share" -> DAILY_SHARE_MAX;
            default -> 1;
        };
    }

    /** 根据经验值计算等级 */
    public static int calcLevel(int exp) {
        if (exp == 0) return 0;
        if (exp < 200) return 1;
        if (exp < 1500) return 2;
        if (exp < 4500) return 3;
        if (exp < 10800) return 4;
        if (exp < 28800) return 5;
        return 6;
    }
}
```

- [ ] **Step 3: 编译验证**

```bash
cd lyy-video && mvn clean install -pl user-service -DskipTests
```

---

### Task 7: ExpConsumer MQ 消费者

**文件：**
- Create: `lyy-video/user-service/src/main/java/com/lyy/user/mq/ExpConsumer.java`

- [ ] **Step 1: 创建 ExpConsumer**

```java
package com.lyy.user.mq;

import com.lyy.common.constant.MqConstant;
import com.lyy.common.dto.ExpMessage;
import com.lyy.user.service.ExpService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ExpConsumer {

    @Autowired
    private ExpService expService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(MqConstant.EXP_QUEUE),
            exchange = @Exchange(value = MqConstant.EXP_EXCHANGE),
            key = MqConstant.EXP_ROUTING_KEY
    ))
    public void handleExpMessage(ExpMessage message) {
        log.info("收到经验消息：userId={}, expValue={}, reason={}",
                message.getUserId(), message.getExpValue(), message.getReason());
        try {
            expService.addExp(message.getUserId(), message.getExpValue(), message.getReason());
            log.info("经验消息处理成功：userId={}", message.getUserId());
        } catch (Exception e) {
            log.error("经验消息处理失败：userId={}, error={}",
                    message.getUserId(), e.getMessage(), e);
        }
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
cd lyy-video && mvn clean install -pl user-service -DskipTests
```

---

### Task 8: UserService 更新

**文件：**
- Modify: `lyy-video/user-service/src/main/java/com/lyy/user/service/UserService.java`
- Modify: `lyy-video/user-service/src/main/java/com/lyy/user/service/impl/UserServiceImpl.java`

- [ ] **Step 1: UserService 接口新增方法**

在 `UserService.java` 末尾追加：

```java
// ==================== 经验相关 ====================
void addExp(Long userId, int expValue, String reason);
List<ExpRecord> getExpRecords(Long userId);
Integer getUserLevel(Long userId);
void updateRealNameVerified(Long userId);
```

需要在文件头部新增 import：

```java
import com.lyy.user.entity.po.ExpRecord;
```

- [ ] **Step 2: UserServiceImpl 实现新增方法**

在 `UserServiceImpl.java` 中注入 `ExpService` 和 `RabbitTemplate`：

```java
@Autowired
private ExpService expService;

@Autowired
private RabbitTemplate rabbitTemplate;
```

新增 import：

```java
import com.lyy.common.constant.MqConstant;
import com.lyy.common.dto.ExpMessage;
import com.lyy.user.entity.po.ExpRecord;
import com.lyy.user.service.ExpService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
```

在 `UserServiceImpl.java` 末尾追加三个方法实现：

```java
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
```

- [ ] **Step 3: 修改 register 方法（默认 level=0），登录后发送经验 MQ**

修改 `register` 方法，新用户 `level` 默认值不再硬编码为 1（已在 Mapper XML 中改为 0）。

修改 `login` 方法，登录成功后发送经验 MQ 消息。在 `return user;` 之前插入：

```java
// 登录经验：发送 MQ 消息
ExpMessage loginExpMsg = ExpMessage.of(user.getId(), 5, "daily_login");
rabbitTemplate.convertAndSend(MqConstant.EXP_EXCHANGE, MqConstant.EXP_ROUTING_KEY, loginExpMsg);
log.info("发送每日登录经验消息：userId={}", user.getId());
```

- [ ] **Step 4: 修改 toProfileVO 方法包含新字段**

在 `toProfileVO` 的 builder 中追加：

```java
.email(user.getEmail())
.realNameVerified(user.getRealNameVerified())
```

- [ ] **Step 5: 编译验证**

```bash
cd lyy-video && mvn clean install -pl user-service -DskipTests
```

---

### Task 9: UserController 新增接口

**文件：**
- Modify: `lyy-video/user-service/src/main/java/com/lyy/user/controller/user/UserController.java`

- [ ] **Step 1: 新增分享上报、经验记录查询、用户等级查询接口**

在 `UserController.java` 中注入 `ExpService` 和 `UserService`（已有），新增以下接口。

追加 import：

```java
import com.lyy.user.entity.po.ExpRecord;
import com.lyy.user.service.ExpService;
import java.util.stream.Collectors;
```

在类末尾追加三个接口方法：

```java
@Operation(summary = "每日分享上报")
@PostMapping("/exp/share")
public Result<String> shareVideo() {
    Long userId = BaseContext.getCurrentId();
    log.info("用户 {} 分享视频，获得经验", userId);
    userService.addExp(userId, 5, "daily_share");
    return Result.success("分享成功，经验+5");
}

@Operation(summary = "查询经验记录（最近30条）")
@GetMapping("/exp/records")
public Result<List<Map<String, Object>>> getExpRecords() {
    Long userId = BaseContext.getCurrentId();
    List<ExpRecord> records = userService.getExpRecords(userId);
    List<Map<String, Object>> list = records.stream().map(r -> {
        Map<String, Object> map = new java.util.HashMap<>();
        map.put("expValue", r.getExpValue());
        map.put("reason", r.getReason());
        map.put("createTime", r.getCreateTime());
        return map;
    }).collect(Collectors.toList());
    return Result.success(list);
}

@Operation(summary = "查询用户等级（供内部Feign调用）")
@GetMapping("/level/{userId}")
public Result<Integer> getUserLevel(@PathVariable Long userId) {
    Integer level = userService.getUserLevel(userId);
    return Result.success(level);
}
```

- [ ] **Step 2: 编译验证**

```bash
cd lyy-video && mvn clean install -pl user-service -DskipTests
```

---

### Task 10: video-service — 观看视频触发经验 MQ

**文件：**
- Modify: `lyy-video/video-service/src/main/java/com/lyy/video/service/impl/UserVideoRecordServiceImpl.java`

- [ ] **Step 1: 在 UserVideoRecordServiceImpl 中注入 RabbitTemplate**

在类中已有的 `@Autowired` 区域追加：

```java
@Autowired
private RabbitTemplate rabbitTemplate;
```

新增 import：

```java
import com.lyy.common.constant.MqConstant;
import com.lyy.common.dto.ExpMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
```

- [ ] **Step 2: 在 playVideo 方法中发送经验 MQ 消息**

在 `playVideo` 方法中，`if (userId == null) { return; }` 之后、`if (videoId == null ...)` 之前插入：

```java
// 每日观看经验（每天首次播放即触发）
ExpMessage watchExpMsg = ExpMessage.of(userId, 5, "daily_watch");
rabbitTemplate.convertAndSend(MqConstant.EXP_EXCHANGE, MqConstant.EXP_ROUTING_KEY, watchExpMsg);
log.info("发送每日观看经验消息：userId={}", userId);
```

- [ ] **Step 3: 编译验证**

```bash
cd lyy-video && mvn clean install -pl video-service -DskipTests
```

---

### Task 11: interaction-service — Lv0 权限限制 + 投币经验 MQ

**文件：**
- Modify: `lyy-video/interaction-service/src/main/java/com/lyy/interaction/controller/CommentController.java`
- Create: `lyy-video/interaction-service/src/main/java/com/lyy/interaction/controller/CoinController.java`
- Modify: `lyy-video/interaction-service/src/main/java/com/lyy/interaction/feign/UserFeignClient.java`

- [ ] **Step 1: UserFeignClient 新增获取用户等级方法**

在 `UserFeignClient.java` 末尾追加：

```java
@GetMapping("/user/level/{userId}")
Result<Integer> getUserLevel(@PathVariable("userId") Long userId);
```

新增 import：

```java
import org.springframework.web.bind.annotation.PathVariable;
```

- [ ] **Step 2: CommentController 新增 Lv0 评论限制**

在 `CommentController.java` 的 `addComment` 方法中，`commentService.addComment(commentAddDTO)` 调用之前，新增 Lv0 检查。

注入 Feign 客户端和 `BaseContext`：

```java
@Autowired
private UserFeignClient userFeignClient;
```

在 `addComment` 方法体中，`log.info("用户添加评论：{}", commentAddDTO);` 之后追加：

```java
// Lv0 用户禁止评论
Long userId = BaseContext.getCurrentId();
Result<Integer> levelResult = userFeignClient.getUserLevel(userId);
if (levelResult != null && levelResult.getData() != null && levelResult.getData() == 0) {
    return Result.error("Lv0 用户暂不支持评论，请先获取经验升级");
}
```

- [ ] **Step 3: 创建 CoinController 投币接口**

```java
package com.lyy.interaction.controller;

import com.lyy.common.constant.MqConstant;
import com.lyy.common.context.BaseContext;
import com.lyy.common.dto.ExpMessage;
import com.lyy.common.result.Result;
import com.lyy.interaction.feign.UserFeignClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "投币", description = "给视频投币")
@RestController
@RequestMapping("/interact/coin")
@Slf4j
public class CoinController {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private UserFeignClient userFeignClient;

    @Operation(summary = "给视频投币")
    @PostMapping("/{videoId}")
    public Result<String> giveCoin(@PathVariable Long videoId) {
        Long userId = BaseContext.getCurrentId();

        // Lv0 用户禁止投币
        Result<Integer> levelResult = userFeignClient.getUserLevel(userId);
        if (levelResult != null && levelResult.getData() != null && levelResult.getData() == 0) {
            return Result.error("Lv0 用户暂不支持投币，请先获取经验升级");
        }

        log.info("用户 {} 给视频 {} 投币", userId, videoId);

        // 发送投币经验消息
        ExpMessage coinExpMsg = ExpMessage.of(userId, 10, "daily_coin");
        rabbitTemplate.convertAndSend(MqConstant.EXP_EXCHANGE, MqConstant.EXP_ROUTING_KEY, coinExpMsg);

        return Result.success("投币成功，经验+10");
    }
}
```

- [ ] **Step 4: 编译验证**

```bash
cd lyy-video && mvn clean install -pl interaction-service -DskipTests
```

---

### Task 12: user-service — 绑定操作触发经验（邮箱/手机/实名）

**文件：**
- Modify: `lyy-video/user-service/src/main/java/com/lyy/user/controller/user/UserController.java`

- [ ] **Step 1: 在 UserController 中新增绑定相关接口**

在 `UserController.java` 末尾追加以下三个接口：

```java
@Operation(summary = "绑定邮箱（首次绑定获得经验）")
@PutMapping("/bind/email")
public Result<String> bindEmail(@RequestBody Map<String, String> body) {
    Long userId = BaseContext.getCurrentId();
    String email = body.get("email");
    if (email == null || email.isBlank()) {
        return Result.error("邮箱不能为空");
    }
    // 检查是否首次绑定（当前 email 为空才算首次）
    UserProfileVO profile = userService.getProfileById(userId);
    boolean isFirstBind = (profile.getEmail() == null || profile.getEmail().isEmpty());

    // 更新邮箱
    UserUpdateDTO dto = new UserUpdateDTO();
    dto.setEmail(email);
    userService.updateProfile(userId, dto);

    // 首次绑定发放经验
    if (isFirstBind) {
        userService.addExp(userId, 20, "bind_email");
        log.info("用户 {} 首次绑定邮箱，经验+20", userId);
    }
    return Result.success("绑定成功");
}

@Operation(summary = "绑定手机号（首次绑定获得经验）")
@PutMapping("/bind/phone")
public Result<String> bindPhone(@RequestBody Map<String, String> body) {
    Long userId = BaseContext.getCurrentId();
    String phone = body.get("phone");
    if (phone == null || phone.isBlank()) {
        return Result.error("手机号不能为空");
    }
    // 检查是否首次绑定
    UserProfileVO profile = userService.getProfileById(userId);
    boolean isFirstBind = (profile.getPhone() == null || profile.getPhone().isEmpty());

    UserUpdateDTO dto = new UserUpdateDTO();
    dto.setPhone(phone);
    userService.updateProfile(userId, dto);

    if (isFirstBind) {
        userService.addExp(userId, 20, "bind_phone");
        log.info("用户 {} 首次绑定手机号，经验+20", userId);
    }
    return Result.success("绑定成功");
}

@Operation(summary = "实名认证（首次认证获得经验）")
@PostMapping("/verify/real-name")
public Result<String> verifyRealName(@RequestBody Map<String, String> body) {
    Long userId = BaseContext.getCurrentId();
    UserProfileVO profile = userService.getProfileById(userId);
    if (profile.getRealNameVerified() != null && profile.getRealNameVerified() == 1) {
        return Result.error("已实名认证，无需重复认证");
    }
    // 标记为已认证
    userService.updateRealNameVerified(userId);

    userService.addExp(userId, 50, "real_name");
    log.info("用户 {} 首次实名认证，经验+50", userId);
    return Result.success("实名认证成功");
}
```

`UserController.java` 中所需的 `Map`、`UserUpdateDTO`、`UserProfileVO` 等 import 均已存在，无需新增。

- [ ] **Step 2: 编译验证**

```bash
cd lyy-video && mvn clean install -pl user-service -DskipTests
```

---

### Task 14: 全局编译 + 集成验证

**文件：** 无

- [ ] **Step 1: 全局编译**

```bash
cd lyy-video && mvn clean install -DskipTests
```

确保所有模块编译通过，无报错。

- [ ] **Step 2: 启动基础设施**

确保 Nacos、MySQL、RabbitMQ 均在本地运行。

- [ ] **Step 3: 按顺序启动服务**

```bash
# 终端1
cd lyy-video && mvn spring-boot:run -pl user-service

# 终端2
cd lyy-video && mvn spring-boot:run -pl video-service

# 终端3
cd lyy-video && mvn spring-boot:run -pl interaction-service

# 终端4
cd lyy-video && mvn spring-boot:run -pl gateway-service
```

- [ ] **Step 4: Apifox 测试用例**

**用例 1：注册新用户 → 验证默认 Lv0**
- POST `http://localhost:8000/api/user/register`
- Body: `{"username":"test001","password":"123456"}`
- 预期：注册成功，数据库 user 表 `level=0, exp=0`

**用例 2：登录 → 验证每日登录 +5 经验**
- POST `http://localhost:8000/api/user/login`
- Body: `{"username":"test001","password":"123456"}`
- 预期：登录成功，检查 `user_exp_record` 表有 `daily_login` 记录，user 表 `exp=5, level=1`

**用例 3：Lv1 用户评论 → 验证可以评论**
- POST `http://localhost:8000/api/interact/comment/add`
- Header: `User-Token: <token>`
- Body: `{"videoId":1,"content":"测试评论"}`
- 预期：评论成功

**用例 4：Lv0 用户评论 → 验证被拒绝**
- 注册新用户，不登录（或登录后手动将 level 改回 0）
- POST `http://localhost:8000/api/interact/comment/add`
- 预期：返回错误 "Lv0 用户暂不支持评论"

**用例 5：Lv0 用户投币 → 验证被拒绝**
- POST `http://localhost:8000/api/interact/coin/1`
- 预期：返回错误 "Lv0 用户暂不支持投币"

**用例 6：Lv1 用户投币 → 验证获得经验**
- POST `http://localhost:8000/api/interact/coin/1`
- 预期：投币成功，`exp` 增加 10

**用例 7：每日投币超过 5 次 → 验证上限**
- 连续调用 6 次投币接口
- 预期：前 5 次成功获得经验，第 6 次不增加经验

**用例 8：每日登录重复 → 验证只加一次**
- 同一用户当天多次登录
- 预期：只有第一次登录获得经验

**用例 9：查询经验记录**
- GET `http://localhost:8000/api/user/exp/records`
- 预期：返回最近经验记录列表

**用例 10：分享上报**
- POST `http://localhost:8000/api/user/exp/share`
- 预期：分享成功，经验 +5（每日仅限 1 次）

**用例 11：首次绑定邮箱**
- PUT `http://localhost:8000/api/user/bind/email`
- Body: `{"email":"test@example.com"}`
- 预期：绑定成功，经验 +20

**用例 12：重复绑定邮箱**
- 再次调用用例 11
- 预期：绑定成功，但经验不增加（首次已领取）

**用例 13：首次绑定手机号**
- PUT `http://localhost:8000/api/user/bind/phone`
- Body: `{"phone":"13800138000"}`
- 预期：绑定成功，经验 +20

**用例 14：首次实名认证**
- POST `http://localhost:8000/api/user/verify/real-name`
- Body: `{"realName":"张三","idCard":"110101199001011234"}`
- 预期：认证成功，经验 +50

---

### Task 15: 边界场景补充

- [ ] **用户不存在时的经验消息：ExpService 静默忽略，不抛异常**
- [ ] **MQ 消息消费失败：catch 异常记录日志，消息不重试（避免死循环）**
- [ ] **经验记录超过 30 条：insert 后自动 deleteOldRecords 清理**
- [ ] **B 端管理后台：AdminUserUpdateDTO 已包含 email、realNameVerified 字段，可正常编辑**
