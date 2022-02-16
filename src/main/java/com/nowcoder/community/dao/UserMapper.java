package com.nowcoder.community.dao;

import com.nowcoder.community.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Mapper
public interface UserMapper {

    // 根据id查询User
    User selectById(int id);
    // 根据用户名查询User
    User selectByName(String username);
    // 根据邮箱查询User
    User selectByEmail(String email);
    // 增加一个用户，返回插入用户的行数
    int insertUser(User user);
    // 修改用户，返回修改生效数目
    int updateStatus(int id, int status);
    // 更新头像
    int updateHeader(int id, String headerUrl);
    // 更新用户密码
    int updatePassword(int id, String password);

}
