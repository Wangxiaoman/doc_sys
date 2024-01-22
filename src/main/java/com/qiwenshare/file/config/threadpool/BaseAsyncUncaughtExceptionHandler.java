package com.qiwenshare.file.config.threadpool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;

import com.qiwenshare.file.log.CommonLogger;

import java.lang.reflect.Method;
import java.util.Arrays;

@Slf4j
public class BaseAsyncUncaughtExceptionHandler implements AsyncUncaughtExceptionHandler {
    @Override
    public void handleUncaughtException(Throwable throwable, Method method, Object... objects) {
        CommonLogger.error("捕获线程异常method[{}] params{}", method, Arrays.toString(objects));
        CommonLogger.error("线程异常");
    }
}
