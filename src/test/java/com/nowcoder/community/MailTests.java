package com.nowcoder.community;

import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.MailClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;


@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = NowcoderApplication.class)
public class MailTests {
    @Autowired
    private MailClient mailClient;

    @Autowired
    private TemplateEngine templateEngine;

    @Test
    public void testTextMail() {
        mailClient.sendMail("wangjiaxiang97@foxmail.com", "TEST", "Welcome");
    }

    @Test
    public void testHtmlMail() {
        Context context = new Context();
        context.setVariable("username", "sunday");

        String s = templateEngine.process("/mail/demo", context);
        System.out.println(s);

        mailClient.sendMail("wangjiaxiang97@foxmail.com", "HTML", s);
    }

    @Test
    public void resetPassword() {
        // 发送邮件
        Context context = new Context();
        context.setVariable("email", "wangjiaxiang97@foxmail.com");
        String code = CommunityUtil.generateUUID().substring(0, 4);
        context.setVariable("verifyCode", code);
        String content = templateEngine.process("/mail/forget", context);
        mailClient.sendMail("wangjiaxiang97@foxmail.com", "找回密码", content);
    }


}
