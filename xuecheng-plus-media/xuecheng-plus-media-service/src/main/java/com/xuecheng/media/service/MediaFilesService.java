package com.xuecheng.media.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.model.dto.UploadFileResultDto;
import com.xuecheng.media.model.po.MediaFiles;

import java.io.File;

/**
 * @author Hang
 * @description 针对表【media_files(媒资信息)】的数据库操作Service
 * @createDate 2024-12-02 16:42:18
 */
public interface MediaFilesService extends IService<MediaFiles> {

    UploadFileResultDto uploadFile(Long companyId, UploadFileParamsDto uploadFileParamsDto, String localFilePath);

    /**
     * 将文件信息添加到文件表
     *
     * @param uploadFileParamsDto
     * @param fileMd5
     * @param companyId
     * @param bucketFiles
     * @param objectName
     * @return
     */
    MediaFiles addMediaFileToDb(UploadFileParamsDto uploadFileParamsDto, String fileMd5, Long companyId, String bucketFiles, String objectName);


    /**
     * @param fileMd5 文件的md5
     * @return com.xuecheng.base.model.RestResponse<java.lang.Boolean> false不存在，true存在
     * @description 检查文件是否存在
     * @author Hangz
     */
    Boolean checkFile(String fileMd5);

    /**
     * @param fileMd5    文件的md5
     * @param chunkIndex 分块序号
     * @return com.xuecheng.base.model.RestResponse<java.lang.Boolean> false不存在，true存在
     * @description 检查分块是否存在
     * @author Hangz
     */
    Boolean checkChunk(String fileMd5, int chunkIndex);

    /**
     * @param filePath
     * @param fileMd5  文件md5
     * @param chunk    分块序号
     * @return com.xuecheng.base.model.RestResponse
     * @description 上传分块
     * @author Hangz
     */
    Boolean uploadChunk(String filePath, String fileMd5, int chunk);

    /**
     * @param companyId           机构id
     * @param fileMd5             文件md5
     * @param chunkTotal          分块总和
     * @param uploadFileParamsDto 文件信息
     * @return com.xuecheng.base.model.RestResponse
     * @description 合并分块
     * @author Hangz
     */
    Boolean mergeFile(Long companyId, String fileMd5, int chunkTotal, UploadFileParamsDto uploadFileParamsDto);

    /**
     * 从minio下载文件
     *
     * @param bucket     桶
     * @param objectName 对象名称
     * @return 下载后的文件
     */
    File downloadFileFromMinIO(String bucket, String objectName);

    /**
     * 将文件写入minIO
     *
     * @param localFilePath
     * @param bucket
     * @param objectName
     * @param mimeType
     * @return
     */
    public boolean addMediaFilesToMinIO(String localFilePath, String bucket, String objectName, String mimeType);
}
