package com.lyy.user.mapper;

import com.lyy.user.entity.po.ExpRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ExpRecordMapper {

    int insert(ExpRecord record);

    List<ExpRecord> selectByUserId(@Param("userId") Long userId,
                                   @Param("limit") int limit);

    /** 统计用户当天某个 reason 的记录数 */
    int countTodayByReason(@Param("userId") Long userId,
                           @Param("reason") String reason);

    /** 统计用户所有时间某个 reason 的记录数（用于首次绑定判断） */
    int countTotalByReason(@Param("userId") Long userId,
                           @Param("reason") String reason);

    /** 删除超出30条的旧记录 */
    int deleteOldRecords(@Param("userId") Long userId,
                         @Param("keepCount") int keepCount);
}
