package com.yupi.springbootinit.model.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiResponseVo {

    private String genChart;

    private String genResult;

    private Long chartId;
}
