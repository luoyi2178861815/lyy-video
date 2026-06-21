package com.lyy.interaction.fallback;

import com.lyy.common.result.Result;
import com.lyy.interaction.entity.vo.CommentUserVO;
import com.lyy.interaction.feign.UserFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class UserFeignFallback implements UserFeignClient {
    @Override
    public Result<List<CommentUserVO>> getProfilesByIds(List<Long> ids) {

        return null;
    }

    // 降级策略：默认返回 Lv1（允许投币），让后续逻辑判断
    @Override
    public Result<Integer> getUserLevel(Long userId) {
        log.error("获取用户等级失败,默认允许投币：userId = {}", userId);
        return Result.success(1);
    }
}
