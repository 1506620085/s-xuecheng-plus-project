package com.xuecheng.media.service;

import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.model.dto.UploadFileResultDto;
import com.xuecheng.media.model.po.MediaFiles;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author Hang
* @description 针对表【media_files(媒资信息)】的数据库操作Service
* @createDate 2024-12-02 16:42:18
*/
public interface MediaFilesService extends IService<MediaFiles> {

    UploadFileResultDto uploadFile(Long companyId, UploadFileParamsDto uploadFileParamsDto, String localFilePath);


    MediaFiles addMediaFileToDb(UploadFileParamsDto uploadFileParamsDto, String fileMd5, Long companyId, String bucketFiles, String objectName);
}
