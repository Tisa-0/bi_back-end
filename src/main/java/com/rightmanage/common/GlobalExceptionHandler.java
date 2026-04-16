package com.rightmanage.common;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLIntegrityConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private String safeMessage(Throwable e) {
        String message = e.getMessage();
        if (message == null || message.trim().isEmpty()) {
            return "系统异常：" + e.getClass().getSimpleName();
        }
        return message;
    }

    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        e.printStackTrace();
        return Result.error(safeMessage(e));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public Result<?> handleAccessDeniedException(AccessDeniedException e) {
        return Result.error(403, "没有权限访问该资源");
    }

    @ExceptionHandler(RuntimeException.class)
    public Result<?> handleRuntimeException(RuntimeException e) {
        e.printStackTrace();
        return Result.error(safeMessage(e));
    }

    /**
     * 处理数据库唯一键冲突异常
     */
    @ExceptionHandler(SQLIntegrityConstraintViolationException.class)
    public Result<?> handleSQLIntegrityConstraintViolationException(SQLIntegrityConstraintViolationException e) {
        String message = e.getMessage();
        if (message != null && message.contains("uk_flow_code")) {
            return Result.error("流程编码已存在，请使用其他编码");
        }
        if (message != null && message.contains("Duplicate entry")) {
            return Result.error("数据重复，请检查后重新提交");
        }
        e.printStackTrace();
        return Result.error("数据保存失败：" + message);
    }
}
