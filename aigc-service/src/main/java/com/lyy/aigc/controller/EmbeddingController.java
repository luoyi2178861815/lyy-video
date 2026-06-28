package com.lyy.aigc.controller;

import com.lyy.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/embedding")

public class EmbeddingController {
    @Autowired
    private VectorStore vectorStore;
    @Autowired
    private EmbeddingModel embeddingModel;
    /*
    * 保存向量存储
    * */
    @PostMapping
    public void saveVectorStore(@RequestParam("messages") List<String> messages) {
        log.info("Saving vector store with messages: {}", messages);
        var documents = messages.stream().map(message -> Document.builder().text(message).build()).toList();
        vectorStore.add(documents);
    }
    /*
    * 搜索向量存储
    * */
    @GetMapping("/search")
    public Result<List<Document>> searchVectorByMessage (@RequestParam("message") String message){
        List<Document> documents = vectorStore.similaritySearch(message);
        return Result.success(documents);
    }
    /*搜索全部向量存储*/
    @GetMapping
    public Result<List<Document>> searchAllVector() {
        List<Document> documents = vectorStore.similaritySearch(SearchRequest.builder().topK(999).query("").build());
        return Result.success(documents);
    }
    /*
    * 删除向量存储
    * */
    @DeleteMapping
    public void deleteVectorStore(@RequestParam("message") List<String> ids) {
        vectorStore.delete(ids);
    }


/*
* 文本转向量
* */
    @GetMapping("/embed")
    public EmbeddingResponse embed(@RequestParam("message") String message){
        return embeddingModel.embedForResponse(List.of(message));
    }

}
