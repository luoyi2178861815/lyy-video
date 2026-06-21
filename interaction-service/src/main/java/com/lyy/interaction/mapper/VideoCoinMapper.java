package com.lyy.interaction.mapper;

import com.lyy.interaction.entity.po.VideoCoin;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface VideoCoinMapper {

    int insert(VideoCoin coin);

    int exists(@Param("userId") Long userId, @Param("videoId") Long videoId);
}
