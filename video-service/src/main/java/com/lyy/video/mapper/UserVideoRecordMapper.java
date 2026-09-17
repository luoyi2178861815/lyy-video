package com.lyy.video.mapper;

import com.lyy.video.entity.po.UserVideoRecord;
import com.lyy.video.entity.vo.WatchHistoryVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserVideoRecordMapper {

    Integer getProgress(@Param("userId") Long userId,
                        @Param("videoId") Long videoId);

    UserVideoRecord getRecord(@Param("userId") Long userId,
                              @Param("videoId") Long videoId);

    void insertUserVideoRecord(UserVideoRecord userVideoRecord);

    void updateByRecordId(UserVideoRecord userVideoRecord);

    List<WatchHistoryVO> listWatchHistory(@Param("userId") Long userId,
                                          @Param("offset") int offset,
                                          @Param("pageSize") int pageSize);

    long countWatchHistory(@Param("userId") Long userId);

    int deleteWatchHistory(@Param("userId") Long userId,
                           @Param("videoId") Long videoId);
}
