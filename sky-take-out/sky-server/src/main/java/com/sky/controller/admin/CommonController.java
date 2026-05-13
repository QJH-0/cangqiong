package com.sky.controller.admin;

import com.sky.constant.MessageConstant;
import com.sky.properties.AliOssProperties;
import com.sky.result.Result;
import com.sky.utils.AliOssUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.asm.MemberSubstitution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/admin/common")
@Api(tags = "通用接口")
@Slf4j
public class CommonController {
    @Autowired
    private AliOssUtil aliOssUtil;

    /**
     * @RequestBody 注解作用
     * @RequestBody 注解主要用于处理 HTTP 请求体中的数据，将请求体中的 JSON、XML 等格式的数据反序列化为 Java 对象。
     * 比如前端以 JSON 格式发送用户信息数据到后端，后端接收对应实体类参数时，通常会用 @RequestBody 注解来表明要对请求体内容进行反序列化操作。
     * MultipartFile 参数处理机制
     * 在上述代码中，MultipartFile 是 Spring 框架用于处理文件上传的类。
     * 当浏览器进行文件上传时，会以 multipart/form - data 这种特殊的请求格式发送数据。
     * Spring 会自动将上传的文件绑定到 MultipartFile 类型的参数上，并不需要使用 @RequestBody 注解。
     * @param file
     * @return
     */
    @PostMapping("/upload")
    @ApiOperation("文件上传")
    public Result<String> upload(MultipartFile file){
        log.info("文件上传{}",file);

        try {
            String originalFilename = file.getOriginalFilename();
            //截取原始文件名的后缀
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String objectName = UUID.randomUUID().toString() + extension;
            String filePath = aliOssUtil.upload(file.getBytes(), objectName);
            return Result.success(filePath);

        } catch (IOException e) {
            log.info("文件上传失败{}",e);
        }
        return Result.error(MessageConstant.UPLOAD_FAILED);}
}
