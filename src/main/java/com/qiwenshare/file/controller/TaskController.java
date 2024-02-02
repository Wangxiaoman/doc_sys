package com.qiwenshare.file.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.qiwenshare.file.api.IShareFileService;
import com.qiwenshare.file.component.FileDealComp;
import com.qiwenshare.file.domain.ShareFile;
import com.qiwenshare.file.domain.UserFile;
import com.qiwenshare.file.io.QiwenFile;
import com.qiwenshare.file.log.CommonLogger;
import com.qiwenshare.file.service.UserFileService;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
public class TaskController {

    @Resource
    UserFileService userFileService;
    @Resource
    FileDealComp fileDealComp;
    @Resource
    IShareFileService shareFileService;
    @Autowired
    private ElasticsearchClient elasticsearchClient;


    @Scheduled(fixedRate = 1000 * 60 * 60 * 24)
    public void updateElasticSearch() {
        List<UserFile> userfileList = userFileService.list(new QueryWrapper<UserFile>().eq("deleteFlag", 0));
        for (int i = 0; i < userfileList.size(); i++) {
            try {

                QiwenFile ufopFile = new QiwenFile(userfileList.get(i).getFilePath(), userfileList.get(i).getFileName(), userfileList.get(i).getIsDir() == 1);
                fileDealComp.restoreParentFilePath(ufopFile, userfileList.get(i).getUserId());
                if (i % 1000 == 0 || i == userfileList.size() - 1) {
                    CommonLogger.info("目录健康检查进度：" + (i + 1) + "/" + userfileList.size());
                }

            } catch (Exception e) {
                CommonLogger.error(e.getMessage());
            }
        }
        userfileList = userFileService.list(new QueryWrapper<UserFile>().eq("deleteFlag", 0).eq("esFlag", 0));
        if(CollectionUtils.isNotEmpty(userfileList)) {
            List<String> userFileIdList = new ArrayList<>();
            for (UserFile userFile : userfileList) {
                fileDealComp.uploadESByUserFileId(userFile.getUserFileId());
                userFileIdList.add(userFile.getUserFileId());
            }
            // 写完ES更新状态
            userFileService.update(new UpdateWrapper<UserFile>().lambda().set(UserFile::getEsFlag, 1).in(UserFile::getUserFileId, userFileIdList));
        }
    }

    @Scheduled(fixedRate = Long.MAX_VALUE)
    public void updateFilePath() {
        List<UserFile> list = userFileService.list();
        for (UserFile userFile : list) {
            try {
                String path = QiwenFile.formatPath(userFile.getFilePath());
                if (!userFile.getFilePath().equals(path)) {
                    userFile.setFilePath(path);
                    userFileService.updateById(userFile);
                }
            } catch (Exception e) {
                // ignore
            }
        }
    }

    @Scheduled(fixedRate = Long.MAX_VALUE)
    public void updateShareFilePath() {
        List<ShareFile> list = shareFileService.list();
        for (ShareFile shareFile : list) {
            try {
                String path = QiwenFile.formatPath(shareFile.getShareFilePath());
                shareFile.setShareFilePath(path);
                shareFileService.updateById(shareFile);
            } catch (Exception e) {
                //ignore
            }
        }
    }
}
