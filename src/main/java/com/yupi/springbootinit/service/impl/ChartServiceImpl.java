package com.yupi.springbootinit.service.impl;

import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.springbootinit.common.ErrorCode;
import com.yupi.springbootinit.constant.redisKey.LimiterKey;
import com.yupi.springbootinit.exception.ThrowUtils;
import com.yupi.springbootinit.manager.AIManager;
import com.yupi.springbootinit.manager.RedisLimiterManager;
import com.yupi.springbootinit.mapper.ChartMapper;
import com.yupi.springbootinit.message.BiMessageProducer;
import com.yupi.springbootinit.model.dto.chart.GenChartByAiRequest;
import com.yupi.springbootinit.model.entity.Chart;
import com.yupi.springbootinit.model.enums.ChartStatusEnum;
import com.yupi.springbootinit.model.vo.AiResponseVo;
import com.yupi.springbootinit.service.ChartService;
import com.yupi.springbootinit.service.UserService;
import com.yupi.springbootinit.utils.ExcelUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 *
 */
@Service
public class ChartServiceImpl extends ServiceImpl<ChartMapper, Chart>
        implements ChartService {

    @Resource
    private UserService userService;

    @Resource
    private AIManager aiManager;

    @Resource
    private RedisLimiterManager limiterManager;

    // 自定义线程池
    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Resource
    private BiMessageProducer biMessageProducer;

    /**
     * 使用线程池异步获取ai回复
     *
     * @param excelData           excel数据
     * @param genChartByAiRequest 用户请求体
     */
    @Override
    @Transactional
    public AiResponseVo getAiResult(MultipartFile excelData, GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
        // 合法的文件后缀
        final List<String> VALID_SUFFIX = Arrays.asList("xlsx", "xls");
        // 检验文件
        String filename = excelData.getOriginalFilename();
        String fileType = FileUtil.getSuffix(filename);
        // 不是合法后缀 返回空
        ThrowUtils.throwIf(!VALID_SUFFIX.contains(fileType), ErrorCode.PARAMS_ERROR, "文件类型不符合要求");

        Long userId = userService.getLoginUser(request).getId();
        //  进行限流
        ThrowUtils.throwIf(!limiterManager.doRateLimit(LimiterKey.GEN_CHART_LIMITER + userId), ErrorCode.TOO_MANY_REQUESTS);

        // 压缩后的csv字符串
        String csvStr = ExcelUtils.excelToCsv(excelData);
        ThrowUtils.throwIf(csvStr == null, ErrorCode.PARAMS_ERROR, "上传文件失败");

        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();
        //  异步流程    先将生成的图表保存到数据库
        Chart chart = Chart.builder().chartData(csvStr)
                .chartType(chartType)
                .userId(userId)
                .goal(goal)
                .name(name)
                .build();
        boolean saveRes = this.save(chart); //  保存后mybatis会将主键赋值给对象id属性
        ThrowUtils.throwIf(!saveRes, ErrorCode.SYSTEM_ERROR);

        // 开始真正执行任务 将任务提交给线程池
        //  todo    判断第三方接口负载情况 较为空闲采用同步    压力大采用异步 ----反向压力
        //  todo    任务提交到队列中失败  则使用定时任务重新把图表加入到工作队列中
        //  todo    任务处理超时自动标记为失败
        CompletableFuture<Void> completableFuture = CompletableFuture.runAsync(() -> {
            // 对图表状态修改为’执行中‘
            boolean updateRes = this.update(new UpdateWrapper<Chart>().eq("id", chart.getId()).set("status", ChartStatusEnum.RUNNING.getStatus()));
            if (!updateRes) {
                handleChartUpdateError(chart.getId(), "更新图表为执行中状态失败");
                return;
            }
            // 拼接用户发送给ai的请求
            String userMessage = "分析需求:\n" +
                    String.format("%s,请使用%s\n", goal, chartType) +
                    "原始数据:\n" +
                    csvStr + "\n";
            // 得到AI回复
            String response = aiManager.doChat(userMessage);
            // 进行分割
            String[] splitResponse = response.split("【【【【【");
            String chartCode = splitResponse[1].trim(); //  echarts代码
            String genConclusion = splitResponse[2].trim(); //  结论
            boolean execRes = this.update(new UpdateWrapper<Chart>().eq("id", chart.getId())
                    .set("status", ChartStatusEnum.SUCCEED.getStatus()).set("genChart", chartCode).set("genResult", genConclusion));
            if (!execRes)
                handleChartUpdateError(chart.getId(), "更新图表为成功状态失败");
        }, threadPoolExecutor);
        // 设置60s超时
        completableFuture.orTimeout(60, TimeUnit.SECONDS).getNow(null);
        // 返回vo对象   不包含请求结果
        return AiResponseVo.builder().chartId(chart.getId()).build();
    }


    /**
     * 使用MQ异步获取ai回复
     *
     * @param excelData           excel数据
     * @param genChartByAiRequest 用户请求体
     */
    @Override
    public AiResponseVo getAiResultByMq(MultipartFile excelData, GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
        // 合法的文件后缀
        final List<String> VALID_SUFFIX = Arrays.asList("xlsx", "xls");
        // 检验文件
        String filename = excelData.getOriginalFilename();
        String fileType = FileUtil.getSuffix(filename);
        // 不是合法后缀 返回空
        ThrowUtils.throwIf(!VALID_SUFFIX.contains(fileType), ErrorCode.PARAMS_ERROR, "文件类型不符合要求");

        Long userId = userService.getLoginUser(request).getId();
        //  进行限流
        ThrowUtils.throwIf(!limiterManager.doRateLimit(LimiterKey.GEN_CHART_LIMITER + userId), ErrorCode.TOO_MANY_REQUESTS);

        // 压缩后的csv字符串
        String csvStr = ExcelUtils.excelToCsv(excelData);
        ThrowUtils.throwIf(csvStr == null, ErrorCode.PARAMS_ERROR, "上传文件失败");

        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();
        //  异步流程    先将生成的图表保存到数据库
        Chart chart = Chart.builder().chartData(csvStr)
                .chartType(chartType)
                .userId(userId)
                .goal(goal)
                .name(name)
                .build();
        boolean saveRes = this.save(chart); //  保存后mybatis会将主键赋值给对象id属性
        ThrowUtils.throwIf(!saveRes, ErrorCode.SYSTEM_ERROR);

        // 开始真正执行任务 将任务提交给MQ 传递图表id
        Long chartId = chart.getId();
        biMessageProducer.sendMessage(chartId);
        // 返回vo对象   不包含请求结果
        return AiResponseVo.builder().chartId(chartId).build();
    }

    // 封装失败状态处理
    public void handleChartUpdateError(long chartId, String execMessage) {
        Chart updateChartResult = new Chart();
        updateChartResult.setId(chartId);
        updateChartResult.setStatus(ChartStatusEnum.FAILED.getStatus());
        updateChartResult.setExecMessage(execMessage);
        boolean updateResult = this.updateById(updateChartResult);
        if (!updateResult) {
            log.error("更新图表失败状态失败" + chartId + "," + execMessage);
        }
    }
}