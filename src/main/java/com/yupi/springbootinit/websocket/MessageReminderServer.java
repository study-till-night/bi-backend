package com.yupi.springbootinit.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.CrossOrigin;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 消息盒子 使用ws进行实时传输
 */
@ServerEndpoint("/wServer/{userId}")
// !!!需要注册为组件
@Component
@Slf4j
public class MessageReminderServer {

    /**
     * concurrent包的线程安全Set，用来存放每个客户端对应的WebSocket对象 是所有server实例共有的。
     */
    private static final ConcurrentHashMap<Long, MessageReminderServer> webSocketMap = new ConcurrentHashMap<>();

    /**
     * 与某个客户端的连接会话，需要通过它来给客户端发送数据
     */
    private Session session;
    /**
     * 接收userId
     */
    private Long userId;

    /**
     * 发送消息到指定客户端
     */
    public static void sendInfo(String message, Long userId) throws IOException {
        log.info("发送消息到:" + userId + "，报文:" + message);
        // 若接收用户已与服务器建立ws连接 则向其传输消息
        if (userId != null && webSocketMap.containsKey(userId)) {
            webSocketMap.get(userId).sendMessage(message);
        } else {
            log.error("用户" + userId + ",不在线！");
        }
    }

    /**
     * 连接建立成功调用的方法
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("userId") Long userId) {
        this.session = session;
        this.userId = userId;
        webSocketMap.remove(userId);
        //加入set中
        webSocketMap.put(userId, this);
        log.info("用户{}连接成功", userId);
        try {
            sendMessage("连接成功");
        } catch (IOException e) {
            log.error("用户:" + userId + ",网络异常!!!!!!");
        }
    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose() {
        //从set中删除
        webSocketMap.remove(userId);
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param message 客户端发送过来的消息
     */
    @OnMessage
    public void onMessage(String message, Session session) {
        log.info("用户消息:" + userId + ",报文:" + message);
    }

    /**
     * @param session
     * @param error
     */
    @OnError
    public void onError(Session session, Throwable error) {
        log.error("用户错误:" + this.userId + ",原因:" + error.getMessage());
        error.printStackTrace();
    }

    /**
     * 实现服务器主动推送
     */
    public void sendMessage(String message) throws IOException {
        this.session.getBasicRemote().sendText(message);
    }
}