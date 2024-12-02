package com.xuecheng.media.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.media.mapper.MediaFilesMapper;
import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.service.MediaFilesService;
import io.minio.MinioClient;
import io.minio.UploadObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileInputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * @author Hang
 * @description 针对表【media_files(媒资信息)】的数据库操作Service实现
 * @createDate 2024-12-02 16:42:18
 */
@Service
@Slf4j
public class MediaFilesServiceImpl extends ServiceImpl<MediaFilesMapper, MediaFiles>
        implements MediaFilesService {

    // 普通文件桶
    @Value("${minio.bucket.files}")
    private String bucketFiles;

    // 视频文件桶
    @Value("${minio.bucket.videoFiles}")
    private String bucketVideoFiles;

    @Resource
    private MinioClient minioClient;

    @Resource
    private MediaFilesMapper mediaFilesMapper;


    @Override
    public UploadFileParamsDto uploadFile(Long companyId, UploadFileParamsDto uploadFileParamsDto, String localFilePath) {

        return null;
    }


    /**
     * 将文件写入minIO
     *
     * @param localFilePath
     * @param bucket
     * @param objectName
     * @param mimeType
     * @return
     */
    public boolean addMediaFilesToMinIO(String localFilePath, String bucket, String objectName, String mimeType) {
        try {
            minioClient.uploadObject(
                    UploadObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .filename(localFilePath)
                            .contentType(mimeType)
                            .build());
            log.debug("上传文件到 minio 成功, bucket:{}, objectName:{}", bucket, objectName);
            return true;
        } catch (Exception e) {
            log.error("上传文件到minio出错,bucket:{},objectName:{},错误原因:{}", bucket, objectName, e.getMessage(), e);
            XueChengPlusException.cast("上传文件到文件系统失败");
            return false;
        }
    }


    @Transactional
    public MediaFiles addMediaFileToDb(UploadFileParamsDto uploadFileParamsDto, String fileMd5, Long companyId, String bucket, String objectName) {
        MediaFiles mediaFiles = mediaFilesMapper.selectById(fileMd5);
        if (mediaFiles != null) {
            XueChengPlusException.cast("文件系统已拥有该文件");
            return mediaFiles;
        }
        // todo 保存到数据库
        return null;
    }

    /**
     * 获取当前日期，格式为 "yyyy/MM/dd"
     *
     * @return 格式化的日期字符串：年/月/日
     */
    private String getDefaultFolderPath() {
        // 获取当前日期
        LocalDate currentDate = LocalDate.now();
        // 定义日期格式
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
        // 格式化日期
        return currentDate.format(formatter);
    }

    /**
     * 获取文件 MD5 值
     *
     * @param file
     * @return 文件 MD5 值
     */
    private String getFileMd5(File file) {
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            return DigestUtils.md5Hex(fileInputStream);
        } catch (Exception e) {
            throw new XueChengPlusException(e.toString());
        }
    }

    /**
     * 通过文件扩展名获取媒体类型
     *
     * @param extension
     * @return 媒体类型字符串
     */
    private String getMimeType(String extension) {
        if (extension == null) {
            extension = "";
        }
        //根据扩展名取出 mimeType
        ContentInfo extensionMatch = ContentInfoUtil.findExtensionMatch(extension);
        //通用 mimeType，字节流
        String mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        if (extensionMatch != null) {
            mimeType = extensionMatch.getMimeType();
        }
        return mimeType;
    }


}




