package com.lyy.user.controller.user;

import com.lyy.common.context.BaseContext;
import com.lyy.common.result.Result;
import com.lyy.user.entity.po.ExpRecord;
import com.lyy.user.service.ExpService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/user")
@Slf4j
public class ExpController {
    @Autowired
    private ExpService expService;

    //查询我的经验
    @GetMapping("/getMyExp")
    public Result<List<ExpRecord>> getExp() {
        Long userId = BaseContext.getCurrentId();
        log.info("查询用户{}的经验", userId);
        List<ExpRecord> recentRecords = expService.getRecentRecords(userId);
        return Result.success(recentRecords);
    }

}
