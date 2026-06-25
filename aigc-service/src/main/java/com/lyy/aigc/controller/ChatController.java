package com.lyy.aigc.controller;

import com.lyy.aigc.entity.dto.ChatDTO;
import com.lyy.aigc.entity.vo.ChatEventVO;
import com.lyy.aigc.service.ChatService;
import com.lyy.common.context.BaseContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/chat")
@Slf4j

public class ChatController {

    @Autowired
    private ChatService chatService;

    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ChatEventVO> chat(@RequestBody(required = false) ChatDTO chatDTO){
        if (chatDTO == null || chatDTO.getQuestion() == null || chatDTO.getQuestion().isBlank()) {
            return Flux.error(new IllegalArgumentException("请求体不能为空，且必须包含 question 字段"));
        }
        Long currentId = BaseContext.getCurrentId();
        System.out.println(currentId);
        return chatService.chat(chatDTO);
    }
    @PostMapping("/stop")
    public void stop(@RequestParam("sessionId") String sessionId) {
        this.chatService.stop(sessionId);
        log.info("Stopped chat session: {}", sessionId);
    }
}
