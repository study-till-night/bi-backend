package com.yupi.springbootinit.message;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.rabbitmq.client.Channel;
import com.yupi.springbootinit.common.ErrorCode;
import com.yupi.springbootinit.constant.MqConstant;
import com.yupi.springbootinit.constant.redisKey.ChartBusinessKey;
import com.yupi.springbootinit.exception.BusinessException;
import com.yupi.springbootinit.manager.AIManager;
import com.yupi.springbootinit.model.entity.Chart;
import com.yupi.springbootinit.model.enums.ChartStatusEnum;
import com.yupi.springbootinit.service.impl.ChartServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class BiMessageConsumer {

    @Resource
    private ChartServiceImpl chartService;

    @Resource
    private AIManager aiManager;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 处理图表生成任务的消费者
     *
     * @param chartId 图表id
     * @param channel 通道
     * @param tag     AcknowledgeMode.NONE：不确认 AcknowledgeMode.AUTO：自动确认 AcknowledgeMode.MANUAL：手动确认
     *                                              todo 拒绝掉的任务会进入死信队列 可在死信队列中将图表状态修改为失败
     */
    @RabbitListener(queues = {MqConstant.BI_QUEUE_NAME}, ackMode = "MANUAL")
    @Transactional
    public void consumeChartGenTask(Long chartId, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag) {
        // 获取图表id
        if (ObjectUtils.isEmpty(chartId)) {
            handleAckReject(channel, tag, false, false, "生成图表任务时 消息拒绝失败");
        }
        // 获取先前存入数据库的图表
        Chart chart = chartService.getById(chartId);
        if (ObjectUtils.isEmpty(chart)) {
            handleAckReject(channel, tag, false, false, "生成图表任务时 消息拒绝失败");
            return;
        }
        // 对图表状态修改为’执行中‘
        boolean updateRes = chartService.update(new UpdateWrapper<Chart>().eq("id", chart.getId()).set("status", ChartStatusEnum.RUNNING.getStatus()));
        if (!updateRes) {
            // chartService.handleChartUpdateError(chartId, "更新图表为执行中状态失败");
            handleAckReject(channel, tag, false, false, "生成图表任务时 消息拒绝失败");
            return;
        }
        String goal = chart.getGoal();
        String chartData = chart.getChartData();
        String chartType = chart.getChartType();
        // 拼接用户发送给ai的请求
        String userMessage = "分析需求:\n" +
                String.format("%s,请使用%s\n", goal, chartType) +
                "原始数据:\n" +
                chartData + "\n";
        // 得到AI回复
        String response = aiManager.doChat(userMessage);
        // 进行分割
        String[] splitResponse = response.split("【【【【【");
        if (splitResponse.length != 3) {
            // chartService.handleChartUpdateError(chartId, "AI 生成内容失败");
            handleAckReject(channel, tag, false, false, "生成图表任务时 消息拒绝失败");
            return;
        }
        String chartCode = splitResponse[1].trim(); //  echarts代码
        String genConclusion = splitResponse[2].trim(); //  结论
        // 进行更新
        boolean execRes = chartService.update(new UpdateWrapper<Chart>().eq("id", chart.getId())
                .set("status", ChartStatusEnum.SUCCEED.getStatus()).set("genChart", chartCode).set("genResult", genConclusion));
        if (!execRes) {
            handleAckReject(channel, tag, false, false, "生成图表任务时 消息拒绝失败");
            return;
            // chartService.handleChartUpdateError(chart.getId(), "更新图表为成功状态失败");
        }
        // 消息确认
        try {
            channel.basicAck(tag, false);
        } catch (IOException e) {
            log.error("生成图表任务时 消息确认失败");
        }
    }

    /**
     * 处理被拒绝的消息
     *
     * @param chartId 图表id
     */
    @RabbitListener(queues = {MqConstant.BI_DEAD_QUEUE_NAME})
    public void consumeRejectedChartTask(Long chartId, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag) {
        log.info("处理失败图表--{} tag-- {}", chartId, tag);
        // 将图表状态设置为失败
        chartService.update(new LambdaUpdateWrapper<Chart>()
                .eq(Chart::getId, chartId).set(Chart::getStatus, ChartStatusEnum.FAILED.getStatus()));
        // 将失败图表id存储到redis集合
        SetOperations<String, Object> setOperations = redisTemplate.opsForSet();
        setOperations.add(ChartBusinessKey.FAILED_LIST_KEY, chartId);
        redisTemplate.expire(ChartBusinessKey.FAILED_LIST_KEY, 30, TimeUnit.SECONDS);
        try {
            channel.basicAck(tag, false);
        } catch (IOException e) {
            log.error("死信队列重新提交任务时消息确认失败");
        }
    }

    /**
     * 拒绝消息封装
     *
     * @param channel  通道
     * @param tag      消息标签
     * @param multiple 是否批量
     * @param requeue  是否重新入队
     * @param logMsg   拒绝失败时日志输出信息
     */
    public void handleAckReject(Channel channel, long tag, boolean multiple, boolean requeue, String logMsg) {
        try {
            channel.basicNack(tag, multiple, requeue);
        } catch (IOException e) {
            log.error(logMsg);
            // 抛出异常不能向前端传递错误信息 只是终止程序
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, logMsg);
        }
    }
}