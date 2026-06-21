package com.lyy.interaction.mapper;

import com.lyy.interaction.entity.po.MqMessageLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface MqMessageLogMapper {


    int insert(MqMessageLog log);
    
    @Update("UPDATE mq_message_log SET status = #{status}, update_time = NOW() WHERE message_id = #{messageId}")
    int updateStatus(@Param("messageId") String messageId, @Param("status") Integer status);
    
    @Update("UPDATE mq_message_log SET status = 2, retry_count = retry_count + 1, " +
            "error_msg = #{errorMsg}, update_time = NOW() WHERE message_id = #{messageId}")
    int updateFailure(@Param("messageId") String messageId, @Param("errorMsg") String errorMsg);

}
