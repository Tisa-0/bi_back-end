package com.rightmanage;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@MapperScan("com.rightmanage.mapper")
@EnableAsync // 启用异步支持，用于业务执行模块的异步调用
public class RightManageApplication {
    public static void main(String[] args) {
        SpringApplication.run(RightManageApplication.class, args);
    }
}
