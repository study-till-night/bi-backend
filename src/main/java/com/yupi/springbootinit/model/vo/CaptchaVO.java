package com.yupi.springbootinit.model.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;
import org.springframework.web.bind.annotation.ModelAttribute;

@ApiModel
@Data
@Builder
public class CaptchaVO {
    //验证码id
    @ApiModelProperty(name = "验证码id")
    private String captchaId;
    //验证码图片base64编码
    @ApiModelProperty(name = "验证码图片base64编码")
    private String captchaImage;
}
