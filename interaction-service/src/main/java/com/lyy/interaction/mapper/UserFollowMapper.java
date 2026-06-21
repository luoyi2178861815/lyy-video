package com.lyy.interaction.mapper;

import com.lyy.interaction.entity.po.UserFollow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserFollowMapper {

    /** 关注（插入记录） */
    int insert(UserFollow follow);

    /** 取关（按 follower + followee 删除） */
    int delete(@Param("followerId") Long followerId, @Param("followeeId") Long followeeId);

    /** 检查是否已关注 */
    int exists(@Param("followerId") Long followerId, @Param("followeeId") Long followeeId);

    /** 分页查关注列表（我关注的人） */
    List<UserFollow> selectFollowingPage(@Param("userId") Long userId,
                                          @Param("offset") int offset,
                                          @Param("pageSize") int pageSize);

    /** 关注总数 */
    int selectFollowingCount(@Param("userId") Long userId);

    /** 分页查粉丝列表（关注我的人） */
    List<UserFollow> selectFansPage(@Param("userId") Long userId,
                                    @Param("offset") int offset,
                                    @Param("pageSize") int pageSize);

    /** 粉丝总数 */
    int selectFansCount(@Param("userId") Long userId);
}
