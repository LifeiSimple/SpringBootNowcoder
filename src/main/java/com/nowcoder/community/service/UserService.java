package com.nowcoder.community.service;

import com.nowcoder.community.dao.LoginTicketMapper;
import com.nowcoder.community.dao.UserMapper;
import com.nowcoder.community.entity.LoginTicket;
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

    @Autowired
    LoginTicketMapper loginTicketMapper;


    // 根据用户id查询用户User
    public User findUserById(int id) {
        return userMapper.selectById(id);
    }

    /**
     * 处理用户忘记密码
     * 通过Map<String, Object> 来处理多种情况，
     * 如果注册有问题，map 中包含对问题的描述
     * 如果没有问题，返回一个空 map
     */
    public Map<String, Object> forget(String email, String code, String newpassword, String correct_code) {
        Map<String, Object> map = new HashMap<>();

        if (StringUtils.isBlank(email)) {
            map.put("emailMsg", "邮箱不能为空");
            return map;
        }
        // 先验证邮箱是否存在
        User u = userMapper.selectByEmail(email); // 如果查到的账号存在，u!=null；如果不存在，则u==null
        if (u == null) {
            map.put("emailMsg", "该邮箱不存在!");
            return map;
        }
        if (StringUtils.isBlank(code)) {
            map.put("codeMsg", "验证码不能为空");
            return map;
        }
        if (StringUtils.isBlank(newpassword)) {
            map.put("passwordMsg", "密码不能为空");
            return map;
        }
        if (!correct_code.equals(code)) {
            map.put("codeMsg", "验证码错误");
            return map;
        } else {
            u.setPassword(CommunityUtil.md5(newpassword + u.getSalt()));
            userMapper.updatePassword(u.getId(), u.getPassword());
        }
        return map;
    }

    // 生成验证码发送并返回验证码
    public String generateCode(String email) {
        User user = userMapper.selectByEmail(email);
        // 生成验证码
        String code = CommunityUtil.generateUUID().substring(0, 4);
        // 把验证码装填进网页，然后通过邮箱发送
        Context context = new Context();
        context.setVariable("email", user.getEmail()); // 传给前端
        context.setVariable("code", code); // 传给前端
        String content = templateEngine.process("/mail/forget", context);
        mailClient.sendMail(user.getEmail(), "忘记密码", content);
        return code;
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

        // 设置默认头像
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
     * <p>
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

    /**
     * 用户登录
     *
     * @param username       用户名
     * @param password       用户密码
     * @param expiredSeconds 过期时间
     * @return Map 包装的多种用户登录处理结果状态
     */
    public Map<String, Object> login(String username, String password, int expiredSeconds) {
        Map<String, Object> map = new HashMap<>();

        // 空值处理
        if (StringUtils.isBlank(username)) {
            map.put("usernameMsg", "账号不能为空!");
            return map;
        }
        if (StringUtils.isBlank(password)) {
            map.put("passwordMsg", "密码不能为空!");
            return map;
        }

        // 验证账号
        User user = userMapper.selectByName(username);
        if (user == null) {
            map.put("usernameMsg", "该账号不存在!");
            return map;
        }

        // 验证状态，看是不是账号注册了，但是账号没有激活
        if (user.getStatus() == 0) {
            map.put("usernameMsg", "该账号未激活!");
            return map;
        }

        // 验证密码，账号注册激活了，判断密码是否正确
        password = CommunityUtil.md5(password + user.getSalt());
        if (!user.getPassword().equals(password)) {
            map.put("passwordMsg", "密码不正确!");
            return map;
        }

        // 以上判断均过，则说明账号正确，可以登录
        // 生成登录凭证
        LoginTicket loginTicket = new LoginTicket();
        loginTicket.setUserId(user.getId());
        loginTicket.setTicket(CommunityUtil.generateUUID()); // 随机生成 ticket
        loginTicket.setStatus(0);
        loginTicket.setExpired(new Date(System.currentTimeMillis() + expiredSeconds * 1000));
        loginTicketMapper.insertLoginTicket(loginTicket);

        map.put("ticket", loginTicket.getTicket());
        return map;
    }

    /**
     * 退出登录
     *
     * @param ticket 用户凭证
     */
    public void logout(String ticket) {
        loginTicketMapper.updateStatus(ticket, 1);
    }

    /**
     * 修改用户密码
     *
     * @param user        用户
     * @param newpassword 新密码
     */
    public void updatePassword(User user, String newpassword) {
        userMapper.updatePassword(user.getId(), newpassword);
    }


    // 供拦截器调用 LoginTicketInterceptor.preHandler
    public LoginTicket findLoginTicket(String ticket) {

        return loginTicketMapper.selectByTicket(ticket);
    }


    // 用户更新头像
    public int updateHeader(int userId, String headerUrl) {
        return userMapper.updateHeader(userId, headerUrl);
    }

    // 修改密码
    public Map<String, Object> updatePassword(int userId, String oldPassword, String newPassword) {
        Map<String, Object> map = new HashMap<>();

        // 空值处理
        if (StringUtils.isBlank(oldPassword)) {
            map.put("oldPasswordMsg", "原密码不能为空!");
            return map;
        }
        if (StringUtils.isBlank(newPassword)) {
            map.put("newPasswordMsg", "新密码不能为空!");
            return map;
        }

        // 验证原始密码
        User user = userMapper.selectById(userId);
        oldPassword = CommunityUtil.md5(oldPassword + user.getSalt());
        if (!user.getPassword().equals(oldPassword)) {
            map.put("oldPasswordMsg", "原密码输入有误!");
            return map;
        }

        // 更新密码
        newPassword = CommunityUtil.md5(newPassword + user.getSalt());
        userMapper.updatePassword(userId, newPassword);

        return map;
    }

}
