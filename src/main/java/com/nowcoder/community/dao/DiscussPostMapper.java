package com.nowcoder.community.dao;

import com.nowcoder.community.entity.DiscussPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DiscussPostMapper {

    // 需要分页查询，首页分页实现不需要userId；userId 用于查看发帖用户

    // 需要动态 SQL
    // userId=0 为首页帖子展示，分页处理
    List<DiscussPost> selectDiscussPosts(int userId, int offset, int limit);


    // 查询帖子的行数，方便分页功能的实现
    // @Param注解用于给参数取别名，
    // 如果只有一个参数，并且在<if>里使用，则必须加别名.
    int selectDiscussPostRows(@Param("userId") int userId);


    // 插入新帖子
    int insertDiscussPost(DiscussPost discussPost);


}
