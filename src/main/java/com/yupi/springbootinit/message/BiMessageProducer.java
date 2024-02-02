package com.yupi.springbootinit.message;

import com.yupi.springbootinit.constant.MqConstant;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class BiMessageProducer {
    @Resource
    private RabbitTemplate rabbitTemplate;

    public void sendMessage(String message) {
        rabbitTemplate.convertAndSend(MqConstant.BI_EXCHANGE_NAME, MqConstant.BI_ROUTING_KEY, message);
    }

    public void sendMessage(Object message) {
        rabbitTemplate.convertAndSend(MqConstant.BI_EXCHANGE_NAME, MqConstant.BI_ROUTING_KEY, message);
    }
}
