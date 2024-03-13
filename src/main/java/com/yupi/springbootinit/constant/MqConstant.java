package com.yupi.springbootinit.constant;

public interface MqConstant {
    // 交换机名称
    String BI_EXCHANGE_NAME="bi_exchange";

    // 队列名称
    String BI_QUEUE_NAME="bi_queue";

    // 死信交换机名称
    String BI_DEAD_EXCHANGE_NAME="bi_dead_exchange";

    // 死信队列名称
    String BI_DEAD_QUEUE_NAME="bi_dead_queue";

    // 普通跳转路径
    String BI_ROUTING_KEY="bi_route";

    // 死信跳转路径
    String BI_DEAD_ROUTING_KEY="bi_route";
}
