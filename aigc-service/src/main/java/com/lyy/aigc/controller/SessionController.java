package com.lyy.aigc.controller;

import com.lyy.aigc.entity.vo.ChatSessionVO;
import com.lyy.aigc.entity.vo.MessageVO;
import com.lyy.aigc.service.ChatSessionService;
import com.lyy.aigc.entity.vo.SessionVO;
import com.lyy.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/session")
@RequiredArgsConstructor
public class SessionController {

    private final ChatSessionService chatSessionService;

    /**
     * 新建会话
     */
    @PostMapping
    public SessionVO createSession(@RequestParam(value = "n", defaultValue = "3") Integer num) {
        return this.chatSessionService.createSession(num);
    }
    /**
     * 查询单个历史对话详情
     *
     * @return 对话记录列表
     */
    @GetMapping("/{sessionId}")
    public List<MessageVO> queryBySessionId(@PathVariable("sessionId") String sessionId) {
        return this.chatSessionService.queryBySessionId(sessionId);
    }
    /**
     * 查询历史会话列表
     */
    @GetMapping("/history")
    public Map<String, List<ChatSessionVO>> queryHistorySession() {
        return this.chatSessionService.queryHistorySession();
    }
    /*
     * 删除会话
    * */
    @DeleteMapping("/{sessionId}")
    public Result<String> deleteSession(@PathVariable("sessionId") String sessionId) {
        this.chatSessionService.deleteSession(sessionId);
        return Result.success("删除成功");
    }
}