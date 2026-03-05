package com.rightmanage;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.rightmanage.mapper")
public class RightManageApplication {
    public static void main(String[] args) {
        SpringApplication.run(RightManageApplication.class, args);
    }
}
