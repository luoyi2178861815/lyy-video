package com.lyy.interaction.service.impl;

import com.lyy.common.constant.MqConstant;
import com.lyy.common.dto.CollectMessage;
import com.lyy.common.exception.BusinessException;
import com.lyy.common.exception.VideoNotFoundException;
import com.lyy.common.result.Result;
import com.lyy.interaction.entity.dto.CollectAddDTO;
import com.lyy.interaction.entity.dto.FolderAddDTO;
import com.lyy.interaction.entity.dto.FolderUpdateDTO;
import com.lyy.interaction.entity.po.CollectFolder;
import com.lyy.interaction.entity.po.VideoCollect;
import com.lyy.interaction.entity.vo.CollectFolderDetailVO;
import com.lyy.interaction.entity.vo.CollectFolderVO;
import com.lyy.interaction.entity.vo.CollectVideoVO;
import com.lyy.interaction.feign.VideoFeignClient;
import com.lyy.interaction.mapper.CollectFolderMapper;
import com.lyy.interaction.mapper.CollectMapper;
import com.lyy.interaction.service.CollectService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CollectServiceImpl implements CollectService {

    @Autowired
    private CollectFolderMapper folderMapper;
    @Autowired
    private CollectMapper collectMapper;
    @Autowired
    private VideoFeignClient videoFeignClient;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    // ==================== 收藏夹 CRUD ====================

    @Override
    public CollectFolderVO createFolder(Long userId, FolderAddDTO dto) {
        CollectFolder folder = new CollectFolder();
        folder.setUserId(userId);
        folder.setName(dto.getName());
        folder.setDescription(dto.getDescription() != null ? dto.getDescription() : "");
        folder.setIsPublic(dto.getIsPublic() != null ? dto.getIsPublic() : 0);
        folder.setVideoCount(0);
        folder.setCoverUrl("");
        folder.setCreateTime(LocalDateTime.now());
        folder.setUpdateTime(LocalDateTime.now());
        folderMapper.insert(folder);
        return toFolderVO(folder);
    }

    @Override
    @Transactional
    public void deleteFolder(Long userId, Long folderId) {
        CollectFolder folder = folderMapper.selectById(folderId);
        if (folder == null) {
            throw new BusinessException("收藏夹不存在");
        }
        if (!folder.getUserId().equals(userId)) {
            throw new BusinessException("只能删除自己的收藏夹");
        }
        List<Long> videoIds = collectMapper.selectVideoIdsByFolderId(folderId);
        collectMapper.deleteByFolderId(folderId);
        folderMapper.deleteById(folderId);
        for (Long videoId : videoIds) {
            CollectMessage msg = new CollectMessage();
            msg.setVideoId(videoId);
            msg.setIncrement(-1);
            rabbitTemplate.convertAndSend(MqConstant.VIDEO_COLLECT_EXCHANGE, MqConstant.VIDEO_COLLECT_ROUTING_KEY, msg);
        }
    }

    @Override
    public void updateFolder(Long userId, FolderUpdateDTO dto) {
        CollectFolder folder = folderMapper.selectById(dto.getId());
        if (folder == null) {
            throw new BusinessException("收藏夹不存在");
        }
        if (!folder.getUserId().equals(userId)) {
            throw new BusinessException("只能编辑自己的收藏夹");
        }
        CollectFolder update = new CollectFolder();
        update.setId(dto.getId());
        update.setName(dto.getName());
        update.setDescription(dto.getDescription());
        update.setIsPublic(dto.getIsPublic());
        folderMapper.updateById(update);
    }

    @Override
    public List<CollectFolderVO> getFolderList(Long userId) {
        List<CollectFolder> folders = folderMapper.selectByUserId(userId);
        if (folders.isEmpty()) {
            // 懒加载：创建默认收藏夹
            createDefaultFolder(userId);
            folders = folderMapper.selectByUserId(userId);
        }
        return folders.stream().map(this::toFolderVO).collect(Collectors.toList());
    }

    @Override
    public CollectFolderDetailVO getFolderDetail(Long folderId, int pageNum, int pageSize) {
        CollectFolder folder = folderMapper.selectById(folderId);
        if (folder == null) {
            throw new BusinessException("收藏夹不存在");
        }
        int offset = (pageNum - 1) * pageSize;
        List<VideoCollect> collects = collectMapper.selectPageByFolderId(folderId, offset, pageSize);
        int total = collectMapper.selectCountByFolderId(folderId);

        List<CollectVideoVO> videos = Collections.emptyList();
        if (!collects.isEmpty()) {
            // 收集视频ID，批量查视频信息
            List<Long> videoIds = collects.stream()
                    .map(VideoCollect::getVideoId)
                    .distinct()
                    .collect(Collectors.toList());
            Map<Long, Map<String, Object>> videoMap = fetchVideoMap(videoIds);
            videos = collects.stream().map(c -> {
                Map<String, Object> v = videoMap.get(c.getVideoId());
                return CollectVideoVO.builder()
                        .videoId(c.getVideoId())
                        .title(v != null ? (String) v.get("title") : null)
                        .coverUrl(v != null ? (String) v.get("coverUrl") : null)
                        .duration(v != null ? (Integer) v.get("duration") : null)
                        .playCount(v != null ? toLong(v.get("playCount")) : null)
                        .likeCount(v != null ? toLong(v.get("likeCount")) : null)
                        .commentCount(v != null ? toLong(v.get("commentCount")) : null)
                        .collectTime(c.getCreateTime())
                        .build();
            }).collect(Collectors.toList());
        }

        return CollectFolderDetailVO.builder()
                .folderId(folder.getId())
                .folderName(folder.getName())
                .description(folder.getDescription())
                .isPublic(folder.getIsPublic())
                .total(total)
                .videos(videos)
                .build();
    }

    // ==================== 收藏/取消收藏 ====================

    @Transactional
    public void collectVideo(Long userId, CollectAddDTO dto) {
        // 验证视频存在并获取封面
        Map<Long, Map<String, Object>> videoMap = fetchVideoMap(Collections.singletonList(dto.getVideoId()));
        Map<String, Object> videoInfo = videoMap.get(dto.getVideoId());
        if (videoInfo == null) {
            throw new VideoNotFoundException("视频不存在或已删除");
        }
        String coverUrl = (String) videoInfo.get("coverUrl"); 
        // 检查是否已收藏
        if (collectMapper.exists(userId, dto.getVideoId()) > 0) {
            throw new BusinessException("已收藏过该视频");
        }
        // 确定收藏夹
        Long folderId = dto.getFolderId();
        if (folderId == null) {
            CollectFolder defaultFolder = getOrCreateDefaultFolder(userId);
            folderId = defaultFolder.getId();
        } else {
            CollectFolder folder = folderMapper.selectById(folderId);
            if (folder == null || !folder.getUserId().equals(userId)) {
                throw new BusinessException("收藏夹不存在");
            }
        }
        // 插入收藏记录
        VideoCollect collect = new VideoCollect();
        collect.setUserId(userId);
        collect.setVideoId(dto.getVideoId());
        collect.setFolderId(folderId);
        collect.setCreateTime(LocalDateTime.now());
        collectMapper.insert(collect);
        // 更新收藏夹 video_count + 封面
        folderMapper.updateCountAndCover(folderId, 1, coverUrl);
        // MQ 更新视频收藏数
        CollectMessage msg = new CollectMessage();
        msg.setVideoId(dto.getVideoId());
        msg.setIncrement(1);
        rabbitTemplate.convertAndSend(MqConstant.VIDEO_COLLECT_EXCHANGE, MqConstant.VIDEO_COLLECT_ROUTING_KEY, msg);
    }

    @Override
    @Transactional
    public void uncollectVideo(Long userId, Long videoId) {
        VideoCollect record = collectMapper.selectByUserAndVideo(userId, videoId);
        if (record == null) {
            throw new BusinessException("未收藏该视频");
        }
        collectMapper.deleteByUserAndVideo(userId, videoId);
        folderMapper.decrVideoCount(record.getFolderId());
        CollectMessage msg = new CollectMessage();
        msg.setVideoId(videoId);
        msg.setIncrement(-1);
        rabbitTemplate.convertAndSend(MqConstant.VIDEO_COLLECT_EXCHANGE, MqConstant.VIDEO_COLLECT_ROUTING_KEY, msg);
    }

    /**
     * 判断用户是否已收藏该视频
     */
    @Override
    public boolean isCollectedByUser(Long userId, Long videoId) {
        int isCollected = collectMapper.exists(userId, videoId);
        return isCollected > 0;

    }

    // ==================== 私有方法 ====================

    /** 获取或创建默认收藏夹 */
    private CollectFolder getOrCreateDefaultFolder(Long userId) {
        CollectFolder folder = folderMapper.selectDefaultByUserId(userId);
        if (folder == null) {
            return createDefaultFolder(userId);
        }
        return folder;
    }

    /** 创建默认收藏夹 */
    private CollectFolder createDefaultFolder(Long userId) {
        CollectFolder folder = new CollectFolder();
        folder.setUserId(userId);
        folder.setName("默认收藏夹");
        folder.setDescription("");
        folder.setIsPublic(0);
        folder.setVideoCount(0);
        folder.setCoverUrl("");
        folder.setCreateTime(LocalDateTime.now());
        folder.setUpdateTime(LocalDateTime.now());
        folderMapper.insert(folder);
        return folder;
    }

    /** Feign 批量查视频信息，失败降级 */
    private Map<Long, Map<String, Object>> fetchVideoMap(List<Long> videoIds) {
        try {
            Result<List<Map<String, Object>>> result = videoFeignClient.getVideosByIds(videoIds);
            if (result != null && result.getData() != null) {
                return result.getData().stream()
                        .collect(Collectors.toMap(
                                v -> ((Number) v.get("id")).longValue(),
                                v -> v,
                                (a, b) -> a));
            }
        } catch (Exception e) {
            // 降级：视频信息留空
        }
        return Collections.emptyMap();
    }

    private CollectFolderVO toFolderVO(CollectFolder f) {
        return CollectFolderVO.builder()
                .id(f.getId())
                .name(f.getName())
                .description(f.getDescription())
                .isPublic(f.getIsPublic())
                .videoCount(f.getVideoCount())
                .coverUrl(f.getCoverUrl())
                .createTime(f.getCreateTime())
                .build();
    }

    private Long toLong(Object obj) {
        if (obj == null) return 0L;
        if (obj instanceof Long) return (Long) obj;
        if (obj instanceof Integer) return ((Integer) obj).longValue();
        return 0L;
    }
}
