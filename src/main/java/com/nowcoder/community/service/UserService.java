package com.nowcoder.community.service;

import com.nowcoder.community.dao.UserMapper;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.MailClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
public class UserService implements CommunityConstant {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MailClient mailClient;

    @Autowired
    private TemplateEngine templateEngine;

    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    // 根据用户id查询用户User
    public User findUserById(int id) {
        return userMapper.selectById(id);
    }


    /**
     * 处理用户注册
     * 通过Map<String, Object> 来处理多种情况，
     * 如果注册有问题，map 中包含对问题的描述
     * 如果没有问题，返回一个空 map
     *
     * @param user
     * @return
     */
    public Map<String, Object> register(User user) {
        Map<String, Object> map = new HashMap<>();

        // 注册时的空值处理，用户填入参数可能为空
        if (user == null) {
            throw new IllegalArgumentException("参数不能为空！");
        }

        if (StringUtils.isBlank(user.getUsername())) {
            map.put("usernameMsg", "账号不能为空");
            return map;
        }
        if (StringUtils.isBlank(user.getPassword())) {
            map.put("passwordMsg", "密码不能为空");
            return map;
        }
        if (StringUtils.isBlank(user.getEmail())) {
            map.put("emailMsg", "邮箱不能为空");
            return map;
        }

        // 如果注册关键数据都不为空，则可以开始进行注册处理
        // 先验证账号是否已经存在
        User u = userMapper.selectByName(user.getUsername()); // 如果查到的账号存在，u!=null；如果不存在，则u==null
        if (u != null) {
            map.put("usernameMsg", "该账号已存在!");
            return map;
        }
        // 验证邮箱
        u = userMapper.selectByEmail(user.getEmail());
        if (u != null) {
            map.put("emailMsg", "该邮箱已被注册!");
            return map;
        }

        // 经过验证后，可以开始注册用户
        // 注册用户
        user.setSalt(CommunityUtil.generateUUID().substring(0, 5));
        user.setPassword(CommunityUtil.md5(user.getPassword() + user.getSalt()));
        user.setType(0);
        user.setStatus(0); // status=0 用户默认为未激活状态，成功激活后才将状态设为1
        user.setActivationCode(CommunityUtil.generateUUID()); // 用户激活码，从工具类CommunityUtil中获取
        user.setHeaderUrl(String.format("http://images.nowcoder.com/head/%dt.png", new Random().nextInt(1000)));
        user.setCreateTime(new Date());
        userMapper.insertUser(user); // user 参数插入前没有id，通过mybatis插入成功后返回id

        // 激活邮件
        Context context = new Context();
        context.setVariable("email", user.getEmail()); // 传给前端
        // http://localhost:8080/community/activation/101/code
        String url = domain + contextPath + "/activation/" + user.getId() + "/" + user.getActivationCode();
        context.setVariable("url", url); // 传给前端

        String content = templateEngine.process("/mail/activation", context);
        mailClient.sendMail(user.getEmail(), "激活账号", content);

        return map;
    }

    /**
     * 用户激活判断
     *
     * 激活的几种情况
     * 1. 激活成功
     * 2. 激活成功后，重复激活
     * 3. 激活码伪造的，激活失败
     */
    public int activation(int userid, String code) {
        // 处理逻辑：
        // 从数据库查到用户，然后判断用户的激活码是否正确
        User user = userMapper.selectById(userid);
        if (user.getStatus() == 1) {
            return ACTIVATION_REPEAT;
        } else if (user.getActivationCode().equals(code)) {
            // 用户成功激活，将用户状态修改为1
            userMapper.updateStatus(userid, 1);
            return ACTIVATION_SUCCESS;
        } else {
            return ACTIVATION_FAILURE;
        }
    }

}
