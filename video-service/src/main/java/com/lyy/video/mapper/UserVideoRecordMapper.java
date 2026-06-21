package com.lyy.video.mapper;

import com.lyy.video.entity.po.UserVideoRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserVideoRecordMapper {

    Integer getProgress(@Param("userId") Long userId,
                        @Param("videoId") Long videoId);

    UserVideoRecord getRecord(@Param("userId") Long userId,
                              @Param("videoId") Long videoId);

    void insertUserVideoRecord(UserVideoRecord userVideoRecord);

    void updateByRecordId(UserVideoRecord userVideoRecord);
}
