package com.lyy.interaction.mapper;

import com.lyy.interaction.entity.po.VideoCollect;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CollectMapper {

    /** 收藏视频 */
    int insert(VideoCollect collect);

    /** 取消收藏（按用户+视频） */
    int deleteByUserAndVideo(@Param("userId") Long userId, @Param("videoId") Long videoId);

    /** 按收藏夹ID删除所有收藏记录 */
    int deleteByFolderId(@Param("folderId") Long folderId);

    /** 检查是否已收藏 */
    int exists(@Param("userId") Long userId, @Param("videoId") Long videoId);

    /** 分页查收藏夹内的视频 */
    List<VideoCollect> selectPageByFolderId(@Param("folderId") Long folderId,
                                            @Param("offset") int offset,
                                            @Param("pageSize") int pageSize);

    /** 收藏夹内视频总数 */
    int selectCountByFolderId(@Param("folderId") Long folderId);

    /** 按用户+视频查收藏记录（用于获取 folderId） */
    VideoCollect selectByUserAndVideo(@Param("userId") Long userId, @Param("videoId") Long videoId);

    /** 查询收藏夹内所有视频ID（用于删除时发MQ） */
    List<Long> selectVideoIdsByFolderId(@Param("folderId") Long folderId);
}
