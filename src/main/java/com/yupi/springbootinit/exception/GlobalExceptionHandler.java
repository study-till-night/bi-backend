package com.yupi.springbootinit.exception;

import com.yupi.springbootinit.common.BaseResponse;
import com.yupi.springbootinit.common.ErrorCode;
import com.yupi.springbootinit.common.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.impl.FileSizeLimitExceededException;
import org.apache.tomcat.util.http.fileupload.impl.SizeLimitExceededException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import java.util.concurrent.TimeoutException;

/**
 * 全局异常处理器
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    /**
     * 业务异常处理
     *
     * @param e
     * @return
     */
    @ExceptionHandler(BusinessException.class)
    public BaseResponse<?> businessExceptionHandler(BusinessException e) {
        log.error("BusinessException {}", e.getMessage());
        return ResultUtils.error(e.getCode(), e.getMessage());
    }

    /**
     * 文件超出大小
     *
     * @param e
     */
    @ExceptionHandler({MultipartException.class})
    public BaseResponse<?> fileOverSizeHandler(MaxUploadSizeExceededException e) {
        log.error("MaxUploadSizeExceededException {}", e.getMessage());
        String msg = ErrorCode.PARAMS_ERROR.getMessage();
        if (e.getCause().getCause() instanceof FileSizeLimitExceededException) {
            log.error(e.getMessage());
            msg += "[单文件大小不得超过100M]";
        } else if (e.getCause().getCause() instanceof SizeLimitExceededException) {
            log.error(e.getMessage());
            msg += "[总上传文件大小不得超过200M]";
        } else {
            msg += "请检查文件类型及大小是否符合规范";
        }
        return ResultUtils.error(ErrorCode.PARAMS_ERROR, msg);
    }

    /**
     * 线程处理超时
     *
     * @param e
     * @return
     */
    @ExceptionHandler(TimeoutException.class)
    public void threadTimeOutExceptionHandler(TimeoutException e) {
        log.error("TimeoutException {}", e.getMessage());
    }


    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<?> runtimeExceptionHandler(RuntimeException e) {
        log.error("RuntimeException {}", e.getMessage());
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "系统错误");
    }
}
