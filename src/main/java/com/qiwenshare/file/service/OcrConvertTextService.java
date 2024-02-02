package com.qiwenshare.file.service;

import java.io.File;
import java.util.Base64;
import java.util.UUID;

import javax.annotation.Resource;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.qiwenshare.common.util.DateUtil;
import com.qiwenshare.common.util.security.SessionUtil;
import com.qiwenshare.file.api.IFileService;
import com.qiwenshare.file.component.FileDealComp;
import com.qiwenshare.file.domain.FileBean;
import com.qiwenshare.file.domain.UserFile;
import com.qiwenshare.file.mapper.UserFileMapper;
import com.qiwenshare.file.util.QiwenFileUtil;
import com.qiwenshare.ufop.util.UFOPUtils;

import cn.hutool.core.util.IdUtil;

@Service
public class OcrConvertTextService {
    
    @Value("${ocr.url}")
    private String ocrUrl;
    @Resource
    UserFileMapper userFileMapper;
    @Resource
    private IFileService fileService;
    @Value("${ufop.storage-type}")
    private Integer storageType;
    @Resource
    private FileDealComp fileDealComp;
    @Resource
    private RestTemplate restTemplate;
    
    public JSONObject ocr(File imageFile) throws Exception {
        String baseStr = Base64.getEncoder().encodeToString(FileUtils.readFileToByteArray(imageFile));
        return ocrBase64Str(baseStr);
    }
    
    public JSONObject ocrMultiFile(MultipartFile file) throws Exception {
        String baseStr = Base64.getEncoder().encodeToString(file.getBytes());
        return ocrBase64Str(baseStr);
    }
    
    public JSONObject ocrBase64Str(String baseStr) throws Exception {
        JSONObject requestParam = new JSONObject();
        JSONArray array = new JSONArray();
        array.add(baseStr);
        requestParam.put("images", array);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<String>(JSON.toJSONString(requestParam),headers);
        String answer = restTemplate.postForObject(ocrUrl, entity, String.class);
        return JSON.parseObject(answer);
    }
    
    // 生产新的txt文件
    public void pdfConvertText(String baseStr, UserFile originUserFile) throws Exception {
        String uuid = UUID.randomUUID().toString().replaceAll("-", "");
        JSONObject ocrJson = ocrBase64Str(baseStr);
        if(ocrJson.get("status").equals("000")){
            StringBuffer sb = new StringBuffer();
            JSONArray ja = ocrJson.getJSONArray("results");
            if(!ja.isEmpty()) {
                for(int i=0;i<ja.size();i++) {
                    JSONArray jaa = ja.getJSONArray(i);
                    if(!jaa.isEmpty()) {
                        for(int j=0;j<jaa.size();j++) {
                            JSONObject jo = jaa.getJSONObject(j);
                            String text = jo.getString("text");
                            if(StringUtils.isNotBlank(text)) {
                                sb.append(text);
                            }
                        }
                    }
                }
            }
            
            if(sb.capacity() == 0) {
                return;
            }
            
            String extendName = "txt";
            String content = sb.toString();
            // 生产一个新文件的路径
            String fileUrl = UFOPUtils.getUploadFileUrl(uuid, extendName);
            String wordFilePath = UFOPUtils.getStaticPath() + fileUrl;
            
            long fileSize = QiwenFileUtil.writeTxt(wordFilePath, content);
            
            // 写入file数据
            FileBean fileBean = new FileBean();
            fileBean.setFileId(IdUtil.getSnowflakeNextIdStr());
            fileBean.setFileSize(fileSize);
            fileBean.setFileUrl(fileUrl);
            fileBean.setStorageType(storageType);
            fileBean.setIdentifier(uuid);
            fileBean.setCreateTime(DateUtil.getCurrentTime());
            fileBean.setCreateUserId(SessionUtil.getSession().getUserId());
            fileBean.setFileStatus(1);
            boolean saveFlag = fileService.save(fileBean);
            UserFile userFile = new UserFile();
            if (saveFlag) {
                content = content.length() > 300 ? content.substring(0,300) : content;
                fileDealComp.uploadESByUserFileIdWithContent(userFile.getUserFileId(), content);
                // 写入userFile
                userFile.setUserFileId(IdUtil.getSnowflakeNextIdStr());
                userFile.setUserId(SessionUtil.getUserId());
                userFile.setFileName(originUserFile.getFileName());
                userFile.setFilePath(originUserFile.getFilePath());
                userFile.setDeleteFlag(0);
                userFile.setIsDir(0);
                userFile.setExtendName(extendName);
                userFile.setUploadTime(DateUtil.getCurrentTime());
                userFile.setFileId(fileBean.getFileId());
                userFile.setCreateTime(DateUtil.getCurrentTime());
                userFile.setCreateUserId(SessionUtil.getUserId());
                userFile.setEsFlag(1);
                String fileName = fileDealComp.getRepeatFileName(userFile, userFile.getFilePath());
                userFile.setFileName(fileName);
                userFileMapper.insert(userFile);
            }
        }
    }
}
