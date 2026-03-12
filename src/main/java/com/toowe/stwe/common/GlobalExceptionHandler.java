package com.toowe.stwe.common;

import com.toowe.fisher.model.resp.R;
import com.toowe.fisher.model.resp.Resp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 统一异常
 *
 * @author maoxiaomeng
 */
@Slf4j
@Order(1)
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public R<String> handleException(Exception exception) {
        System.out.println("exception = " + exception);
        exception.printStackTrace();
        return Resp.error(exception.getMessage());
    }

}
