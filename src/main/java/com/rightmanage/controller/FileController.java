package com.rightmanage.controller;

import com.rightmanage.common.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 文件上传控制器
 */
@RestController
@RequestMapping("/file")
public class FileController {

    // 文件上传目录（相对于项目根目录）
    @Value("${file.upload.path:./uploads}")
    private String uploadPath;

    // 访问URL前缀
    @Value("${file.access.prefix:/api/files}")
    private String accessPrefix;

    /**
     * 文件上传接口
     */
    @PostMapping("/upload")
    public Result<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("上传文件不能为空");
        }

        // 校验文件类型
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            return Result.error("文件名无效");
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        // 只允许 excel、pdf、wps 格式
        if (!".xlsx".equals(extension) && !".xls".equals(extension)
                && !".pdf".equals(extension) && !".wps".equals(extension)) {
            return Result.error("仅支持 xlsx、xls、pdf、wps 格式文件");
        }

        // 限制文件大小 10MB
        if (file.getSize() > 10 * 1024 * 1024) {
            return Result.error("文件大小不能超过10MB");
        }

        try {
            // 创建上传目录
            File uploadDir = new File(uploadPath);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            // 生成新文件名（避免重名）
            String newFileName = UUID.randomUUID().toString().replace("-", "") + extension;
            
            // 按日期创建子目录
            String datePath = new SimpleDateFormat("yyyyMMdd").format(new Date());
            File dateDir = new File(uploadPath, datePath);
            if (!dateDir.exists()) {
                dateDir.mkdirs();
            }

            // 保存文件
            File targetFile = new File(dateDir, newFileName);
            file.transferTo(targetFile);

            // 返回文件访问路径
            String fileUrl = accessPrefix + "/" + datePath + "/" + newFileName;
            String fileName = originalFilename;

            Map<String, String> result = new HashMap<>();
            result.put("url", fileUrl);
            result.put("name", fileName);

            return Result.success(result);
        } catch (IOException e) {
            e.printStackTrace();
            return Result.error("文件上传失败：" + e.getMessage());
        }
    }
}
