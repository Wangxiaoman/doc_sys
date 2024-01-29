package com.qiwenshare.file.api;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.qiwenshare.file.domain.UserFile;
import com.qiwenshare.file.vo.file.FileListVO;

public interface IUserFileService extends IService<UserFile> {
    List<UserFile> selectUserFileByNameAndPath(String fileName, String filePath, String userId);
    List<UserFile> selectSameUserFile(String fileName, String filePath, String extendName, String userId);

    IPage<FileListVO> userFileList(String userId, String filePath, Long beginCount, Long pageCount);
    void updateFilepathByUserFileId(String userFileId, String newfilePath, String userId);
    void userFileCopy(String userId, String userFileId, String newfilePath);
    int pdfConvertPdf(String userFileId);
    
    IPage<FileListVO> getFileByFileType(Integer fileTypeId, Long currentPage, Long pageCount, String userId);
    List<UserFile> selectUserFileListByPath(String filePath, String userId);
    List<UserFile> selectFilePathTreeByUserId(String userId);
    void deleteUserFile(String userFileId, String sessionUserId);

    List<UserFile> selectUserFileByLikeRightFilePath(@Param("filePath") String filePath, @Param("userId") String userId);
    
}
