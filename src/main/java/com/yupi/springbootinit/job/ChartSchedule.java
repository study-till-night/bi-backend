package com.yupi.springbootinit.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yupi.springbootinit.constant.redisKey.ChartBusinessKey;
import com.yupi.springbootinit.constant.redisKey.ScheduleKey;
import com.yupi.springbootinit.mapper.ChartMapper;
import com.yupi.springbootinit.message.BiMessageProducer;
import com.yupi.springbootinit.model.entity.Chart;
import com.yupi.springbootinit.model.enums.ChartStatusEnum;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ChartSchedule {

    @Resource
    private ChartMapper chartMapper;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private RedisTemplate<String, Long> redisTemplate;

    @Resource
    private BiMessageProducer biMessageProducer;

    /**
     * 每隔30s重新提交失败任务
     */
    @Scheduled(fixedDelay = 30 * 1000)
    public void reCommitFailedTask() {
        RLock lock = redissonClient.getLock(ScheduleKey.RECOMMIT_LOCK_KEY);
        try {
            if (lock.tryLock(0, -1, TimeUnit.SECONDS)) {
                Set<Long> redisChartSets = redisTemplate.opsForSet().members(ChartBusinessKey.FAILED_LIST_KEY);
                ArrayList<Long> chartArrayList = new ArrayList<>();
                // 如果redis中没有存储
                if (redisChartSets == null || redisChartSets.size() == 0) {
                    // 得到失败任务的chartId列表
                    List<Long> tempCharts = chartMapper.selectList(new LambdaQueryWrapper<Chart>()
                                    .eq(Chart::getStatus, ChartStatusEnum.FAILED.getStatus()).select(Chart::getId))
                            .stream().map(Chart::getId).collect(Collectors.toList());
                    chartArrayList.addAll(tempCharts);
                } else chartArrayList.addAll(redisChartSets);
                log.info("开始重新提交失败任务--{}", chartArrayList);
                // 依次加入队列
                chartArrayList.forEach(chartId -> {
                    biMessageProducer.sendMessage(chartId);
                });
                log.info("重新提交任务完成");
            }
        } catch (InterruptedException e) {
            log.error("redisson error on recommiting recommit--lock");
        } finally {
            if (lock.isHeldByCurrentThread())
                lock.unlock();
        }
    }
}
