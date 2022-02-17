package com.nowcoder.community.dao;

import com.nowcoder.community.entity.DiscussPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DiscussPostMapper {

    // 需要分页查询，首页分页，不需要 userId
    // userId 用于用户查看
    // 需要动态 SQL
    List<DiscussPost> selectDiscussPosts(int userId, int offset, int limit);


    // 查询帖子的行数，方便分页功能的实现
    // @Param注解用于给参数取别名，
    // 如果只有一个参数，并且在<if>里使用，则必须加别名.
    int selectDiscussPostRows(@Param("userId") int userId);
}
