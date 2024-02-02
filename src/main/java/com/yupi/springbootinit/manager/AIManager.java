package com.yupi.springbootinit.manager;

import com.yupi.springbootinit.common.ErrorCode;
import com.yupi.springbootinit.exception.BusinessException;
import com.yupi.springbootinit.exception.ThrowUtils;
import com.yupi.yucongming.dev.client.YuCongMingClient;
import com.yupi.yucongming.dev.common.BaseResponse;
import com.yupi.yucongming.dev.model.DevChatRequest;
import com.yupi.yucongming.dev.model.DevChatResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class AIManager {
    @Resource
    private YuCongMingClient client;

    @Value("${yuapi.client.model-id}")
    private long modelId;

    /**
     * 进行对话
     * @param userMessage    用户输入消息
     * @return
     */
    public  String doChat(String userMessage){
        DevChatRequest request = new DevChatRequest();
        // 设置模型id及请求消息
        request.setModelId(modelId);
        request.setMessage(userMessage);
        // 得到ai回复消息
        BaseResponse<DevChatResponse> AIResponse = client.doChat(request);

        ThrowUtils.throwIf(AIResponse==null, ErrorCode.SYSTEM_ERROR,"AI系统错误");

        return AIResponse.getData().getContent();
    }
}
