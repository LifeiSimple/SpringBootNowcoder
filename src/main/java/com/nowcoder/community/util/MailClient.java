package com.nowcoder.community.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

@Component
public class MailClient {

    private static final Logger logger = LoggerFactory.getLogger(MailClient.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    /**
     * 发送邮件
     *
     * @param to 邮件接收方
     * @param subject 邮件主题
     * @param content 邮件内容
     */
    public void sendMail(String to, String subject, String content) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            // MimeMessageHelper 来帮助创建邮箱信息
            MimeMessageHelper helper = new MimeMessageHelper(message);
            // 邮件发送方
            helper.setFrom(from);

            // 邮件接收方
            helper.setTo(to);
            // 邮件标题
            helper.setSubject(subject);
            // 邮件内容，html:true 表示允许填充HTML格式内容
            helper.setText(content, true);
            // 发送邮件
            mailSender.send(helper.getMimeMessage());

        } catch (MessagingException e) {
            e.printStackTrace();
            logger.error("发送邮件失败："+e.getMessage());
        }
    }

}
