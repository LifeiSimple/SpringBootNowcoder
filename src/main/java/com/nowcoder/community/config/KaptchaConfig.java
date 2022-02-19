package com.nowcoder.community.config;


import com.google.code.kaptcha.Producer;
import com.google.code.kaptcha.impl.DefaultKaptcha;
import com.google.code.kaptcha.util.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

@Configuration
public class KaptchaConfig {

    @Bean
    public Producer kaptchaProducer() {
        // 配置 Kaptcha
        Properties properties = new Properties();
        properties.setProperty("kaptcha.image.width", "100");
        properties.setProperty("kaptcha.image.height", "40");
        properties.setProperty("kaptcha.textproducer.font.size", "32"); // 大小
        properties.setProperty("kaptcha.textproducer.font.color", "0,0,0"); // 颜色
        properties.setProperty("kaptcha.textproducer.char.string", "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYAZ"); // 内容范围
        properties.setProperty("kaptcha.textproducer.char.length", "4"); // 长度4位
        properties.setProperty("kaptcha.noise.impl", "com.google.code.kaptcha.impl.NoNoise"); // 是否加噪点干扰

        DefaultKaptcha kaptcha = new DefaultKaptcha();
        // 配置kaptcha
        Config config = new Config(properties);
        kaptcha.setConfig(config);
        return kaptcha;
    }

}
