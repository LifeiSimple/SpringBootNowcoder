package com.nowcoder.community.util;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.DigestUtils;

import java.util.UUID;


/**
 * 工具类
 * 用于用户注册
 */
public class CommunityUtil {

    // 生成随机字符串，注册用的激活码
    public static String generateUUID() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }



    // MD5加密
    // hello -> abc123def456
    // hello + 3e4a8 -> abd123fdf341d
    // 加盐salt
    public static String md5(String key) {
        if (StringUtils.isBlank(key)) {
            return null;
        }
        return DigestUtils.md5DigestAsHex(key.getBytes());
    }
}
