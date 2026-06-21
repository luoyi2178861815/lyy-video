package com.lyy.interaction.service;

import com.lyy.interaction.entity.dto.CollectAddDTO;
import com.lyy.interaction.entity.dto.FolderAddDTO;
import com.lyy.interaction.entity.dto.FolderUpdateDTO;
import com.lyy.interaction.entity.vo.CollectFolderDetailVO;
import com.lyy.interaction.entity.vo.CollectFolderVO;

import java.util.List;

public interface CollectService {

    /** 创建收藏夹 */
    CollectFolderVO createFolder(Long userId, FolderAddDTO dto);

    /** 删除收藏夹（同时清理收藏记录 + MQ更新视频收藏数） */
    void deleteFolder(Long userId, Long folderId);

    /** 编辑收藏夹 */
    void updateFolder(Long userId, FolderUpdateDTO dto);

    /** 我的收藏夹列表 */
    List<CollectFolderVO> getFolderList(Long userId);

    /** 查看收藏夹内视频（分页 + 视频信息） */
    CollectFolderDetailVO getFolderDetail(Long folderId, int pageNum, int pageSize);

    /** 收藏视频 */
    void collectVideo(Long userId, CollectAddDTO dto);

    /** 取消收藏 */
    void uncollectVideo(Long userId, Long videoId);

    /** 判断用户是否已收藏该视频 */
    boolean isCollectedByUser(Long userId, Long videoId);
}

