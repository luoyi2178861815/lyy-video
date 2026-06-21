package com.lyy.user.mapper;

import com.lyy.user.entity.po.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface UserMapper {
    int insert(User user);
    User selectById(@Param("id") Long id);
    User selectByUsername(@Param("username") String username);
    int updateById(User user);
    int deleteById(@Param("id") Long id);

    // B端管理后台：条件查询与分页
    List<User> selectUserList(@Param("offset") int offset,
                              @Param("pageSize") int pageSize,
                              @Param("keyword") String keyword);

    int selectUserCount(@Param("keyword") String keyword);
    List<User> selectByIds(@Param("ids") List<Long> ids);

    void updateUserLikeCount(@Param("userId") Long userId,
                             @Param("increment") Integer increment);

    void updateFollowCount(@Param("userId") Long userId,
                           @Param("increment") Integer increment);

    void updateFansCount(@Param("userId") Long userId,
                         @Param("increment") Integer increment);

    void incrementExp(@Param("userId") Long userId,
                      @Param("increment") Integer increment);

    Integer selectLevelById(@Param("userId") Long userId);
}
