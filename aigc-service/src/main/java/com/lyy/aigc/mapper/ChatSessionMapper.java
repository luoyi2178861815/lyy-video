package com.lyy.aigc.mapper;

import com.lyy.aigc.entity.po.ChatSession;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatSessionMapper {

    void insert(ChatSession chatSession);
}
