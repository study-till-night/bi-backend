package com.yupi.springbootinit.model.dto.user;

import java.io.Serializable;

import io.swagger.annotations.ApiModel;
import lombok.Data;

/**
 * 用户登录请求
 *
 * @author  shu
 *  
 */
@Data
@ApiModel
public class UserLoginRequest implements Serializable {

    private static final long serialVersionUID = 3191241716373120793L;

    private String userAccount;

    private String userPassword;

    private String captchaId;

    private String captchaText;
}
