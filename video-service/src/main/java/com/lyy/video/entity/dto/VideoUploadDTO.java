package com.lyy.video.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoUploadDTO {

    private MultipartFile videoFile;
    private MultipartFile coverFile;
    private String videoUrl;
    private String coverUrl;
    private String title;
    private String introduction;
    private Integer statementCode;
    private Integer partitionCode;
    private List<String> tags;
}