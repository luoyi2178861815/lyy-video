package com.lyy.user.mapper;

import com.lyy.user.entity.po.Admin;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AdminMapper {

    /** 新增管理员 */
    int insert(Admin admin);

    /** 按登录账号查询 */
    Admin selectByUsername(@Param("username") String username);

    /** 统计管理员总数，供首次启动播种判断 */
    int countAll();
}
