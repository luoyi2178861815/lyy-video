package com.lyy.interaction.mapper;

import com.lyy.interaction.entity.po.CollectFolder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CollectFolderMapper {

    /** 插入收藏夹 */
    int insert(CollectFolder folder);

    /** 根据ID查收藏夹 */
    CollectFolder selectById(@Param("id") Long id);

    /** 查用户的所有收藏夹 */
    List<CollectFolder> selectByUserId(@Param("userId") Long userId);

    /** 查用户的默认收藏夹（第一个创建的） */
    CollectFolder selectDefaultByUserId(@Param("userId") Long userId);

    /** 更新收藏夹（动态字段） */
    int updateById(CollectFolder folder);

    /** 删除收藏夹 */
    int deleteById(@Param("id") Long id);

    /** 更新收藏夹视频数和封面 */
    int updateCountAndCover(@Param("id") Long id,
                            @Param("increment") int increment,
                            @Param("coverUrl") String coverUrl);

    /** 自增视频数 */
    int incrVideoCount(@Param("id") Long id);

    /** 自减视频数 */
    int decrVideoCount(@Param("id") Long id);
}
