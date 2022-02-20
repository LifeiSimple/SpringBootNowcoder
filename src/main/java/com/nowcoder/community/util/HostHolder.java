package com.nowcoder.community.util;

import com.nowcoder.community.entity.User;
import org.springframework.stereotype.Component;

// 起到一个容器的信息，用于代替 session 对象
// ThreadLoacl 取值存值 是通过一个 map 来实现的，每次取值存值前都获取当前线程对象，以线程为 key 存取值
@Component
public class HostHolder {
    private ThreadLocal<User> users = new ThreadLocal<>();

    public void setUser(User user) {
        users.set(user);
    }

    public User getUser() {
        return users.get();
    }

    // 清理
    public void clear() {
        users.remove();
    }

}
