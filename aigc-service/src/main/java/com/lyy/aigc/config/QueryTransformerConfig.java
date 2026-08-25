package com.lyy.aigc.config;

// 新文件：aigc-service/src/main/java/com/lyy/aigc/config/QueryTransformerConfig.java

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class QueryTransformerConfig implements QueryTransformer {

    private final ChatModel chatModel;

    @NotNull
    @Override
    public Query transform(Query query) {
        String originalQuery = query.text();
        String rewrittenQuery = chatModel.call("""
                你是一个查询重写助手。请将用户的问题重写为更适合在知识库中检索的形式。
                要求：
                1. 保留原始问题的核心意图
                2. 补充隐含的上下文信息
                3. 使用更精确、更具体的关键词
                4. 直接输出重写后的问题，不要有任何解释
                
                用户原始问题：%s
                重写后的问题：
                """.formatted(originalQuery));

        log.info("[QueryTransformer] 原始query: {}, 重写query: {}", originalQuery, rewrittenQuery);

        return query.mutate()
                .text(rewrittenQuery.trim())
                .build();
    }
}
