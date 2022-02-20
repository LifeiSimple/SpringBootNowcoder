package com.nowcoder.community.controller;

import com.google.code.kaptcha.Producer;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.imageio.ImageIO;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

@Controller
public class LoginController implements CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    @Value("${server.servlet.context-path}")
    private String contextPath;


    @Autowired
    private UserService userService;

    @Autowired
    private Producer kaptchaProducer;

    // 访问注册页面，访问一个页面
    @RequestMapping(path = "/register", method = RequestMethod.GET)
    public String getRegisterPage() {

        return "/site/register";
    }

    // 访问登录页面，访问一个页面
    @RequestMapping(path = "/login", method = RequestMethod.GET)
    public String getLoginPage() {

        return "/site/login";
    }

    // 访问修改密码页面，访问一个页面
    @RequestMapping(path = "/forget", method = RequestMethod.GET)
    public String forget() {
        return "/site/forget";
    }

    /**
     * 生成验证码
     *
     * @param response
     * @param session
     */
    @RequestMapping(path = "/kaptcha", method = RequestMethod.GET)
    public void getKaptcha(HttpServletResponse response, HttpSession session) {
        // 生成验证码
        String text = kaptchaProducer.createText();
        BufferedImage image = kaptchaProducer.createImage(text);

        // 将验证码存入 session
        session.setAttribute("kaptcha", text);
        // 将图片输出给 浏览器，response 由 springMVC 自动管理维护
        response.setContentType("image/png");
        try {
            OutputStream outputStream = response.getOutputStream();
            ImageIO.write(image, "png", outputStream);
        } catch (IOException e) {
            logger.error("响应验证码失败：" + e.getMessage());
        }
    }

    /**
     * 处理登录请求
     *
     * @param username   用户名
     * @param password   密码
     * @param code       验证码
     * @param rememberme 记住我
     * @param model
     * @param session    验证码存放于 session 中
     * @param response
     * @return
     */
    @RequestMapping(path = "/login", method = RequestMethod.POST)
    public String login(String username, String password, String code, boolean rememberme,
                        Model model, HttpSession session, HttpServletResponse response) {
        // 检查验证码
        String kaptcha = (String) session.getAttribute("kaptcha");
        // 验证码不区分大小写
        if (StringUtils.isBlank(kaptcha) || StringUtils.isBlank(code) || !kaptcha.equalsIgnoreCase(code)) {
            model.addAttribute("codeMsg", "验证码不正确!");
            return "/site/login";
        }

        // 检查账号,密码
        // 存放过期时间，如果没有勾选上记住我，仍然保存到客户端，只是时间短一点
        // 如果勾选上记住我，保存时间长一点
        int expiredSeconds = rememberme ? REMEMBER_EXPIRED_SECONDS : DEFAULT_EXPIRED_SECONDS;

        Map<String, Object> map = userService.login(username, password, expiredSeconds);
        if (map.containsKey("ticket")) {
            Cookie cookie = new Cookie("ticket", map.get("ticket").toString());
            cookie.setPath(contextPath); // cookie 生效的路径
            cookie.setMaxAge(expiredSeconds); // cookie 有效时间
            response.addCookie(cookie); // 添加 cookie
            return "redirect:/index";
        } else {
            model.addAttribute("usernameMsg", map.get("usernameMsg"));
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            return "/site/login";
        }
    }


    /**
     * 退出登录
     */
    @RequestMapping(path = "/logout", method = RequestMethod.GET)
    public String logout(@CookieValue("ticket") String ticket) {
        userService.logout(ticket);
        return "redirect:/login"; // 重定向去 /login 默认是 GET 请求
    }


    // 发送验证码
    @RequestMapping(path = "/forget/emailcode", method = RequestMethod.GET)
    public String generateCode(Model model, String email, String code, String newpassword,
                               HttpSession session) {
        String correct_code = userService.generateCode(email);
        session.setAttribute("email", correct_code);
        session.setMaxInactiveInterval(60 * 5);

        return "/site/forget";
    }

    /**
     * todo
     * 忘记密码
     *
     * @param model
     * @param email
     * @param code
     * @param newpassword
     * @return
     */
    @RequestMapping(path = "/forget", method = RequestMethod.POST)
    public String forget(Model model, String email, String code, String newpassword) {
        String correct_code = "1234";
        Map<String, Object> map = userService.forget(email, code, newpassword, correct_code);
        if (map == null || map.isEmpty()) {
            return "/site/login";
        } else {
            model.addAttribute("emailMsg", map.get("emailMsg"));
            model.addAttribute("codeMsg", map.get("codeMsg"));
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            return "/site/forget";
        }
    }

    // 注册激活

    /**
     * 进行注册，成功会跳转到注册中间页面，显示发送了激活邮件，
     * 后续需要在邮件中点击激活链接进行激活
     *
     * @param model
     * @param user
     * @return
     */
    @RequestMapping(path = "/register", method = RequestMethod.POST)
    public String register(Model model, User user) {
        Map<String, Object> map = userService.register(user);
        if (map == null || map.isEmpty()) {
            model.addAttribute("msg", "注册成功，我们已经向您的邮箱发送了一封激活邮件，请尽快激活！");
            model.addAttribute("target", "/index");
            return "/site/operate-result";
        } else {
            model.addAttribute("usernameMsg", map.get("usernameMsg"));
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            model.addAttribute("emailMsg", map.get("emailMsg"));

            return "/site/register";
        }
    }


    /**
     * 激活邮箱中的激活链接，点击激活链接完成激活操作
     *
     * @param model
     * @param userId 用户id
     * @param code   激活码
     * @return
     */
    // http://localhost:8080/community/activation/101/code 请求路径格式
    @RequestMapping(path = "/activation/{userId}/{code}", method = RequestMethod.GET)
    // @PathVariable 从路径中取值
    public String activation(Model model, @PathVariable("userId") int userId, @PathVariable("code") String code) {
        int result = userService.activation(userId, code);
        if (result == ACTIVATION_SUCCESS) {
            model.addAttribute("msg", "注册成功，您的账号已经可以正常使用！");
            model.addAttribute("target", "/login");
        } else if (result == ACTIVATION_REPEAT) {
            model.addAttribute("msg", "无效操作，该账号已经激活过了！");
            model.addAttribute("target", "/login");
        } else {
            model.addAttribute("msg", "激活失败，您提供的激活码不正确！");
            model.addAttribute("target", "/login");
        }
        return "/site/operate-result";
    }


}