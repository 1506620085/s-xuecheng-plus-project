package com.xuecheng.media.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.media.mapper.MediaFilesMapper;
import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.model.dto.UploadFileResultDto;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.service.MediaFilesService;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.minio.UploadObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.BeanUtils;
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

    @Resource
    MediaFilesService currentProxy;

    @Override
    public UploadFileResultDto uploadFile(Long companyId, UploadFileParamsDto uploadFileParamsDto, String localFilePath) {
        File file = new File(localFilePath);
        if (!file.exists()) {
            XueChengPlusException.cast("文件不存在");
        }
        //文件名称
        String filename = uploadFileParamsDto.getFilename();
        //文件扩展名
        String extension = filename.substring(filename.lastIndexOf("."));
        // 文件 mimeType
        String mimeType = getMimeType(extension);
        // 文件 MD5 值
        String fileMd5 = getFileMd5(file);
        //文件的默认目录
        String defaultFolderPath = getDefaultFolderPath();
        //存储到minio中的对象名(带目录)
        String objectName = defaultFolderPath + "/" + filename;
        // 判断文件是否上传成功
        boolean uploadSuccess = false;
        try {
            uploadSuccess = addMediaFilesToMinIO(localFilePath, bucketFiles, objectName, mimeType);
            //文件大小
            uploadFileParamsDto.setFileSize(file.length());
            MediaFiles mediaFiles = currentProxy.addMediaFileToDb(uploadFileParamsDto, fileMd5, companyId, bucketFiles, objectName);
            //准备返回数据
            UploadFileResultDto uploadFileResultDto = new UploadFileResultDto();
            BeanUtils.copyProperties(mediaFiles, uploadFileResultDto);
            return uploadFileResultDto;
        } catch (Exception e) {
            // 如果数据库保存失败，删除 MinIO 中的文件
            if (uploadSuccess) {
                try {
                    removeMediaFilesFromMinIO(bucketFiles, objectName);
                } catch (Exception deleteException) {
                    log.error("删除 MinIO 文件失败: {}, {}", bucketFiles, objectName, deleteException);
                }
            }
            XueChengPlusException.cast("上传文件过程出现错误，上传失败"); // 抛出原始异常
            return null;
        }
    }

    private void removeMediaFilesFromMinIO(String bucket, String objectName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .build());
            log.debug("删除上传至 minio 中的文件成功, bucket:{}, objectName:{}", bucket, objectName);
        } catch (Exception e) {
            XueChengPlusException.cast("删除失败");
        }
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


    /**
     * 将文件信息添加到文件表
     *
     * @param uploadFileParamsDto
     * @param fileMd5
     * @param companyId
     * @param bucket
     * @param objectName
     * @return
     */
    @Transactional
    public MediaFiles addMediaFileToDb(UploadFileParamsDto uploadFileParamsDto, String fileMd5, Long companyId, String bucket, String objectName) {
        MediaFiles mediaFiles = mediaFilesMapper.selectById(fileMd5);
        if (mediaFiles != null) {
            XueChengPlusException.cast("文件系统已拥有该文件");
            return mediaFiles;
        }
        // 初始化 MediaFiles 对象
        mediaFiles = new MediaFiles();
        // 保存到数据库
        BeanUtils.copyProperties(uploadFileParamsDto, mediaFiles);
        mediaFiles.setId(fileMd5);
        mediaFiles.setCompanyId(companyId);
        mediaFiles.setFileId(fileMd5);
        mediaFiles.setUrl("/" + bucket + "/" + objectName);
        mediaFiles.setFilePath(objectName);
        mediaFiles.setBucket(bucket);
        mediaFiles.setAuditStatus("002003");
        mediaFiles.setStatus("1");
        int insert = mediaFilesMapper.insert(mediaFiles);
        if (insert < 0) {
            log.debug("保存文件信息到数据库成功,{}", mediaFiles.toString());
            XueChengPlusException.cast("保存文件信息失败");
        }
        return mediaFiles;
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




