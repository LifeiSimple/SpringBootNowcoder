package com.nowcoder.community;

import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.UserService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = NowcoderApplication.class)
public class MyTest {
    @Autowired
    UserService userService;

    @Test
    public void generateCode() {
        User user = new User();
        user.setEmail("wangjiaxiang97@foxmail.com");
        String code = userService.generateCode(user.getEmail());

        System.out.println(code);
    }


}
