package com.yupi.springbootinit.controller;

import com.yupi.springbootinit.constant.UserConstant;
import com.yupi.springbootinit.websocket.MessageReminderServer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@RestController
public class TestController {

    @GetMapping("/test")
    public void test(HttpServletRequest request) {
        try {
            MessageReminderServer.sendInfo("发送msg", 1731666544179572737L);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
