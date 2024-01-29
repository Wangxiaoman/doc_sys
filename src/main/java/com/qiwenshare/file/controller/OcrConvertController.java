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
import com.qiwenshare.file.service.OcrConvertTextService;

import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "ocr", description = "该接口为ocr转换接口")
@RestController
@RequestMapping("/ocr")
public class OcrConvertController {
    
    public static final String CURRENT_MODULE = "OCR识别模块";
    @Resource
    private OcrConvertTextService ocrService;
    @Resource
    FileDealComp fileDealComp;
    @Resource
    IFileService fileService;
    @Resource
    IUserFileService userFileService;
    
    @PostMapping(value = "/fileid/predict")
    @MyLog(operation = "图片识别", module = CURRENT_MODULE)
    @ResponseBody
    public RestResult<?> FileConvertText(@RequestBody JSONObject requetParam){
        String userFileId = requetParam.getString("userFileId");
        if(StringUtils.isBlank(userFileId)) {
            throw new QiwenException(400001, "用户文件ID异常");
        }
        
        try {
            UserFile userFile = userFileService.getById(userFileId);
            FileBean fileBean = fileService.getById(userFile.getFileId());
            String fileUrl = fileBean.getFileUrl();
            String fileBase64 = fileDealComp.getBase64ByFile(fileUrl,fileBean.getStorageType());
            ocrService.pdfConvertText(fileBase64, userFile);
            return RestResult.success().data("生产新txt文件，文件名称为:"+userFile.getFileName()+".txt");
        }catch(Exception ex) {
            CommonLogger.error("file ocr to text error,ex:",ex);
            throw new QiwenException(999998, "ocr识别异常");
        }
    }
    
    @PostMapping(value = "/file/predict")
    @MyLog(operation = "原图识别", module = CURRENT_MODULE)
    @ResponseBody
    public RestResult<JSONObject> imageFileConvertText(@RequestParam("file") MultipartFile file){
        try {
            JSONObject jo = ocrService.ocrMultiFile(file);
            return RestResult.success().data(jo);
        }catch(Exception ex) {
            CommonLogger.error("file ocr to text error,ex:",ex);
            throw new QiwenException(999998, "ocr识别异常");
        }
    }
}
