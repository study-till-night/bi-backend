package com.yupi.springbootinit.service;

import com.yupi.springbootinit.model.dto.chart.GenChartByAiRequest;
import com.yupi.springbootinit.model.entity.Chart;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.springbootinit.model.vo.AiResponseVO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

/**
 *
 */
public interface ChartService extends IService<Chart> {
    /**
     * 获取ai回复
     * @param excelData excel数据
     * @param genChartByAiRequest   用户请求体
     * @return
     */
    AiResponseVO getAiResult(MultipartFile excelData, GenChartByAiRequest genChartByAiRequest, HttpServletRequest request);

    /**
     * 使用MQ异步获取ai回复
     *
     * @param excelData           excel数据
     * @param genChartByAiRequest 用户请求体
     * @return
     */
    public AiResponseVO getAiResultByMq(MultipartFile excelData, GenChartByAiRequest genChartByAiRequest, HttpServletRequest request);
}
