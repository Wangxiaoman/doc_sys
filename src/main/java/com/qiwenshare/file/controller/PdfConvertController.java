package com.qiwenshare.file.controller;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.alibaba.fastjson2.JSONObject;
import com.qiwenshare.common.anno.MyLog;
import com.qiwenshare.common.exception.QiwenException;
import com.qiwenshare.common.result.RestResult;
import com.qiwenshare.file.api.IFileService;
import com.qiwenshare.file.api.IUserFileService;
import com.qiwenshare.file.component.FileDealComp;
import com.qiwenshare.file.domain.FileBean;
import com.qiwenshare.file.domain.UserFile;
import com.qiwenshare.file.log.CommonLogger;
import com.qiwenshare.file.service.PdfConvertTextService;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "pdf", description = "该接口为pdf转换接口")
@RestController
@RequestMapping("/pdf")
public class PdfConvertController {
    public static final String CURRENT_MODULE = "PDF转换模块";
    @Resource
    FileDealComp fileDealComp;
    @Resource
    IFileService fileService;
    @Resource
    IUserFileService userFileService;
    
    @PostMapping(value = "/word")
    @MyLog(operation = "PDF转换为WORD", module = CURRENT_MODULE)
    @ResponseBody
    public RestResult<?> pdfConvertWord(@RequestBody JSONObject requetParam){
        String userFileId = requetParam.getString("userFileId");
        if(StringUtils.isBlank(userFileId)) {
            throw new QiwenException(400001, "用户文件ID异常");
        }
        try {
            int result = userFileService.pdfConvertPdf(userFileId);
            if (result > 0) {
                return RestResult.success().data("转换成功");
            } else {
                return RestResult.success().data("转换失败");
            }
        }catch(Exception ex) {
            CommonLogger.error("pdf convert word error,ex:",ex);
            throw new QiwenException(999998, "pdf转换失败");
        }
    }
}
