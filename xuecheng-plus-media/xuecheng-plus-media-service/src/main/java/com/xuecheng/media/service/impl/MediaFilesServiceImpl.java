package com.xuecheng.media.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.media.mapper.MediaFilesMapper;
import com.xuecheng.media.mapper.MediaProcessMapper;
import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.model.dto.UploadFileResultDto;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.service.MediaFilesService;
import io.minio.*;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
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
import java.io.InputStream;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Formatter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private MediaProcessMapper mediaProcessMapper;

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
        //添加到待处理任务表
        addWaitingTask(mediaFiles);
        return mediaFiles;
    }

    /**
     * 添加待处理任务
     * @param mediaFiles 媒资文件信息
     */
    private void addWaitingTask(MediaFiles mediaFiles){
        String filename = mediaFiles.getFilename();
        //文件扩展名
        String exension = filename.substring(filename.lastIndexOf("."));
        //文件mimeType
        String mimeType = getMimeType(exension);
        //如果是avi视频添加到视频待处理表
        if(mimeType.equals("video/x-msvideo")){
            MediaProcess mediaProcess = new MediaProcess();
            BeanUtils.copyProperties(mediaFiles,mediaProcess);
            mediaProcess.setStatus("1");//未处理
            mediaProcess.setFailCount(0);//失败次数默认为0
            mediaProcessMapper.insert(mediaProcess);
        }
    }

    /**
     * @param fileMd5 文件的md5
     * @return com.xuecheng.base.model.RestResponse<java.lang.Boolean> false不存在，true存在
     * @description 检查文件是否存在
     * @author Hangz
     */
    @Override
    public Boolean checkFile(String fileMd5) {
        MediaFiles mediaFiles = mediaFilesMapper.selectById(fileMd5);
        if (mediaFiles != null) {
            // 桶
            String bucket = mediaFiles.getBucket();
            // 存储目录
            String filePath = mediaFiles.getFilePath();
            try {
                GetObjectResponse object = minioClient.getObject(
                        GetObjectArgs.builder()
                                .bucket(bucket)
                                .object(filePath)
                                .build()
                );
                if (object != null) {
                    return true;
                }
            } catch (Exception e) {
                XueChengPlusException.cast(e.toString());
            }
        }
        return false;
    }

    /**
     * @param fileMd5    文件的md5
     * @param chunkIndex 分块序号
     * @return com.xuecheng.base.model.RestResponse<java.lang.Boolean> false不存在，true存在
     * @description 检查分块是否存在
     * @author Hangz
     */
    @Override
    public Boolean checkChunk(String fileMd5, int chunkIndex) {
        //得到分块文件目录
        String chunkFileFolderPath = getChunkFileFolderPath(fileMd5);
        try {
            GetObjectResponse object = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketVideoFiles)
                            .object(chunkFileFolderPath + chunkIndex)
                            .build()
            );
            return object != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取分块文件目录
     *
     * @param fileMd5
     * @return
     */
    private String getChunkFileFolderPath(String fileMd5) {
        return fileMd5.substring(0, 1) + "/" + fileMd5.substring(1, 2) + "/" + fileMd5 + "/" + "chunk" + "/";
    }

    /**
     * @param filePath
     * @param fileMd5  文件md5
     * @param chunk    分块序号
     * @return com.xuecheng.base.model.RestResponse
     * @description 上传分块
     * @author Hangz
     */
    @Override
    public Boolean uploadChunk(String filePath, String fileMd5, int chunk) {
        //分块文件的路径
        String chunkFileFolderPath = getChunkFileFolderPath(fileMd5) + chunk;
        //获取mimeType
        String mimeType = getMimeType(null);
        return addMediaFilesToMinIO(filePath, bucketVideoFiles, chunkFileFolderPath, mimeType);
    }

    /**
     * @param companyId           机构id
     * @param fileMd5             文件md5
     * @param chunkTotal          分块总和
     * @param uploadFileParamsDto 文件信息
     * @return com.xuecheng.base.model.RestResponse
     * @description 合并分块
     * @author Hangz
     */
    @Override
    public Boolean mergeFile(Long companyId, String fileMd5, int chunkTotal, UploadFileParamsDto uploadFileParamsDto) {
        //分块文件的路径
        String chunkFileFolderPath = getChunkFileFolderPath(fileMd5);
        // 合并分块
        List<ComposeSource> sourceObjectList = Stream.iterate(0, i -> ++i).limit(chunkTotal).map(i ->
                ComposeSource.builder()
                        .bucket(bucketVideoFiles)
                        .object(chunkFileFolderPath + i)
                        .build()
        ).collect(Collectors.toList());
        //文件名称
        String fileName = uploadFileParamsDto.getFilename();
        //文件扩展名
        String extName = fileName.substring(fileName.lastIndexOf("."));
        //合并文件路径
        String mergeFilePath = getFilePathByMd5(fileMd5, extName);
        try {
            minioClient.composeObject(
                    ComposeObjectArgs.builder()
                            .bucket(bucketVideoFiles)
                            .object(mergeFilePath)
                            .sources(sourceObjectList)
                            .build());
            log.debug("合并文件成功:{}", mergeFilePath);
        } catch (Exception e) {
            log.debug("合并文件失败,fileMd5:{},异常:{}", fileMd5, e.getMessage(), e);
            XueChengPlusException.cast(e.toString());
        }

        String mergeFileMd5 = null;
        long fileSize = 0;
        try {
            // 获取文件信息
            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketVideoFiles)
                            .object(mergeFilePath)
                            .build()
            );
            fileSize = stat.size(); // 获取文件大小

            // 获取文件输入流
            InputStream fileStream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketVideoFiles)
                            .object(mergeFilePath)
                            .build()
            );
            log.debug("获取文件大小成功:{}", fileSize);
            uploadFileParamsDto.setFileSize(fileSize);

            // 计算文件 MD5
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fileStream.read(buffer)) != -1) {
                md.update(buffer, 0, bytesRead);
            }
            fileStream.close();
            // 转换 MD5 为字符串
            mergeFileMd5 = byteToHex(md.digest());
            log.debug("获取文件 MD5 成功:{}", mergeFileMd5);
        } catch (Exception e) {
            log.debug("获取文件状态失败,fileMd5:{},异常:{}", fileMd5, e.getMessage(), e);
            XueChengPlusException.cast(e.toString());
        }
        // 验证文件 md5
        if (!fileMd5.equals(mergeFileMd5)) {
            XueChengPlusException.cast("文件合并校验失败");
        }
        // 上传数据库
        currentProxy.addMediaFileToDb(uploadFileParamsDto, fileMd5, companyId, bucketVideoFiles, mergeFilePath);
        // 清除分块文件
        clearChunkFiles(chunkFileFolderPath, chunkTotal);
        return true;
    }

    /**
     * 清除分块文件
     *
     * @param chunkFileFolderPath 分块文件路径
     * @param chunkTotal          分块文件总数
     */
    private void clearChunkFiles(String chunkFileFolderPath, int chunkTotal) {
        try {
            List<DeleteObject> deleteObjects = Stream.iterate(0, i -> ++i)
                    .limit(chunkTotal)
                    .map(i -> new DeleteObject(chunkFileFolderPath.concat(Integer.toString(i))))
                    .collect(Collectors.toList());

            RemoveObjectsArgs removeObjectsArgs = RemoveObjectsArgs.builder().bucket(bucketVideoFiles).objects(deleteObjects).build();
            Iterable<Result<DeleteError>> results = minioClient.removeObjects(removeObjectsArgs);
            results.forEach(r -> {
                DeleteError deleteError = null;
                try {
                    deleteError = r.get();
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("清楚分块文件失败,objectname:{}", deleteError.objectName(), e);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            log.error("清楚分块文件失败,chunkFileFolderPath:{}", chunkFileFolderPath, e);
        }
    }

    /**
     * 将字节数组转换为十六进制字符串表示形式
     *
     * @param bytes
     * @return
     */
    private static String byteToHex(byte[] bytes) {
        Formatter formatter = new Formatter();
        for (byte b : bytes) {
            formatter.format("%02x", b);
        }
        String result = formatter.toString();
        formatter.close();
        return result;
    }

    /**
     * 得到合并后的文件的地址
     *
     * @param fileMd5 文件id即md5值
     * @param fileExt 文件扩展名
     * @return
     */
    private String getFilePathByMd5(String fileMd5, String fileExt) {
        return fileMd5.substring(0, 1) + "/" + fileMd5.substring(1, 2) + "/" + fileMd5 + "/" + fileMd5 + fileExt;
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




