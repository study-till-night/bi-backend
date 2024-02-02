package com.yupi.springbootinit.message.config;

import com.yupi.springbootinit.constant.MqConstant;
import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;

@Configuration
public class BiMqConfig {

    @Bean("biDirectExchange")
    public DirectExchange biDirectExchange(){
        return new DirectExchange(MqConstant.BI_EXCHANGE_NAME);
    }

    @Bean("biBusinessQueue")
    public Queue biBusinessQueue(){
        HashMap<String, Object> params = new HashMap<>();
        //声明当前队列绑定的死信交换机
        params.put("x-dead-letter-exchange", MqConstant.BI_DEAD_EXCHANGE_NAME);
        //声明当前队列的死信路由 key
        params.put("x-dead-letter-routing-key", MqConstant.BI_DEAD_ROUTING_KEY);

        return QueueBuilder.durable(MqConstant.BI_QUEUE_NAME).withArguments(params).build();
    }
    // 声明死信交换机
    @Bean("biDeadExchange")
    public DirectExchange biDeadExchange(){
        return new DirectExchange(MqConstant.BI_DEAD_EXCHANGE_NAME);
    }

    // 声明死信队列
    @Bean("biDeadQueue")
    public Queue biDeadQueue(){
        return QueueBuilder.durable(MqConstant.BI_DEAD_QUEUE_NAME).build();
    }

    // 连接普通队列与直连交换机
    @Bean
    public Binding bindingBiQueueToExchange(@Qualifier("biBusinessQueue") Queue queue,
                                            @Qualifier("biDirectExchange") DirectExchange exchange){
        return BindingBuilder.bind(queue).to(exchange).with(MqConstant.BI_ROUTING_KEY);
    }
    // 连接死信队列与直死信交换机
    @Bean
    public Binding bindingDeadQueueToDeadExchange(@Qualifier("biDeadQueue") Queue queue,
                                            @Qualifier("biDeadExchange") DirectExchange exchange){
        return BindingBuilder.bind(queue).to(exchange).with(MqConstant.BI_DEAD_ROUTING_KEY);
    }
}
