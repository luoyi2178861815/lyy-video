package com.lyy.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * 文件上传工具类
 * 将文件保存到本地指定目录，并返回保存后的路径
 */
@Slf4j
@Component
public class FileUploadUtil {

    // 视频文件存储的根目录（可根据配置文件注入）
    private static final String VIDEO_STORAGE_PATH = "C:\\Users\\21788\\Desktop\\bilbili\\lyy-video\\videos";

    /**
     * 保存上传的文件（使用原文件名，若重名则自动添加UUID避免覆盖）
     *
     * @param file 上传的 MultipartFile
     * @return 文件保存后的完整绝对路径（如：C:\Users\...\videos\abc.mp4）
     * @throws IOException 保存失败时抛出
     */
    public String saveFile(MultipartFile file) throws IOException {
        return saveFile(file, false);
    }

    /**
     * 保存上传的文件，可选择是否保留原始文件名（推荐保留，但添加唯一标识）
     *
     * @param file             上传的文件
     * @param keepOriginalName 是否保留原始文件名（若true，则文件名 = UUID + 原文件名；若false，则仅用UUID+扩展名）
     * @return 完整保存路径
     * @throws IOException 保存失败
     */
    public String saveFile(MultipartFile file, boolean keepOriginalName) throws IOException {
        // 1. 确保存储目录存在
        Path storagePath = Paths.get(VIDEO_STORAGE_PATH);
        if (!Files.exists(storagePath)) {
            Files.createDirectories(storagePath);
        }

        // 2. 获取原始文件名和扩展名
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        // 3. 生成唯一文件名（避免覆盖）
        String uniqueName;
        if (keepOriginalName) {
            // 保留原文件名，但前方加UUID防止重名覆盖
            String nameWithoutExt = originalFilename != null ? originalFilename.substring(0, originalFilename.lastIndexOf(".")) : "file";
            uniqueName = UUID.randomUUID().toString() + "_" + nameWithoutExt + extension;
        } else {
            uniqueName = UUID.randomUUID().toString() + extension;
        }

        // 4. 构建目标路径并保存文件
        Path targetPath = storagePath.resolve(uniqueName);
        file.transferTo(targetPath.toFile());

        return targetPath.toAbsolutePath().toString();
    }

    /**
     * 保存文件，并自动在原始文件名前添加时间戳（保证唯一性）
     *
     * @param file 上传的文件
     * @return 完整路径
     */
    public String saveFileWithTimestamp(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            originalFilename = originalFilename.substring(0, originalFilename.lastIndexOf("."));
        }
        String timestamp = String.valueOf(System.currentTimeMillis());
        String newFileName = originalFilename + "_" + timestamp + extension;

        Path storagePath = Paths.get(VIDEO_STORAGE_PATH);
        if (!Files.exists(storagePath)) {
            Files.createDirectories(storagePath);
        }
        Path targetPath = storagePath.resolve(newFileName);
        file.transferTo(targetPath.toFile());
        return targetPath.toAbsolutePath().toString();
    }

    /**
     * 获取存储目录的绝对路径（可用于展示）
     */
    public String getStoragePath() {
        return Paths.get(VIDEO_STORAGE_PATH).toAbsolutePath().toString();
    }
}