package com.yupi.springbootinit.constant;

/**
 * 用户常量
 *
 * @author  shu
 *  
 */
public interface UserConstant {

    /**
     * 用户登录态键
     */
    String USER_LOGIN_STATE = "user_login";

    /**
     * 用户当前验证码id
     */
    String USER_CAPTCHA_ID="captcha_id";

    //  region 权限

    /**
     * 默认角色
     */
    String DEFAULT_ROLE = "user";

    /**
     * 管理员角色
     */
    String ADMIN_ROLE = "admin";

    /**
     * 被封号
     */
    String BAN_ROLE = "ban";

    // endregion
}
