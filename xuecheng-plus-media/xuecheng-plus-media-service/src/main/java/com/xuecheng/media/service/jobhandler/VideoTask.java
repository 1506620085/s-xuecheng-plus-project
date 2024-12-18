package com.xuecheng.media.service.jobhandler;

import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.utils.Mp4VideoUtil;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.service.MediaFilesService;
import com.xuecheng.media.service.MediaProcessService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
public class VideoTask {

    @Resource
    private MediaProcessService mediaProcessService;

    @Resource
    private MediaFilesService mediaFilesService;

    @Value("${videoprocess.ffmpegpath}")
    private String ffmpegpath;

    @Value("${minio.bucket.videoFiles}")
    private String bucket;

    @XxlJob("videoJobHandler")
    public void videoJobHandler() throws Exception {
        // 分片参数
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();
        List<MediaProcess> mediaProcessList = null;
        int size = 0;
        try {
            // 取出 cpu 核心数作为一次处理数据的条数
            int processors = Runtime.getRuntime().availableProcessors();
            //一次处理视频数量不要超过 cpu 核心数
            mediaProcessList = mediaProcessService.getMediaProcessList(shardIndex, shardTotal, processors);
            size = mediaProcessList.size();
            log.debug("取出待处理视频任务{}条", size);
            if (size <= 0) {
                return;
            }
        } catch (Exception e) {
            log.error(e.toString());
            XueChengPlusException.cast(e.toString());
        }
        // 启动 size 个线程的线程池
        ExecutorService executorService = Executors.newFixedThreadPool(size);
        // 计数器
        CountDownLatch countDownLatch = new CountDownLatch(size);
        // 遍历开启任务
        mediaProcessList.forEach(mediaProcess -> {
            executorService.execute(() -> {
                try {
                    //任务id
                    Long taskId = mediaProcess.getId();
                    Boolean taskBoolean = mediaProcessService.startTask(taskId);
                    if (!taskBoolean) {
                        return;
                    }
                    // 桶
                    String bucket = mediaProcess.getBucket();
                    // 存储路径
                    String filePath = mediaProcess.getFilePath();
                    // 原始视频的md5值
                    String fileId = mediaProcess.getFileId();
                    // 原始文件名称
                    String filename = mediaProcess.getFilename();
                    // 将要处理的文件下载到服务器上
                    // Tips: 因为该服务需要处理文件建议部署到文件服务器上，这样就不会占用太多带宽，只占用本地 I/O
                    File originalFile = mediaFilesService.downloadFileFromMinIO(mediaProcess.getBucket(), mediaProcess.getFilePath());
                    if (originalFile == null) {
                        log.debug("下载待处理文件失败,originalFile:{}", mediaProcess.getBucket().concat(mediaProcess.getFilePath()));
                        mediaProcessService.saveProcessFinishStatus(taskId, "3", fileId, null, "下载待处理文件失败");
                        return;
                    }
                    //处理结束的视频文件
                    File mp4File = null;
                    try {
                        mp4File = File.createTempFile("mp4", ".mp4");
                    } catch (IOException e) {
                        log.error("创建mp4临时文件失败");
                        mediaProcessService.saveProcessFinishStatus(taskId, "3", fileId, null, "创建mp4临时文件失败");
                        return;
                    }
                    // 视频处理结果
                    String result = "";
                    try {
                        Mp4VideoUtil mp4VideoUtil = new Mp4VideoUtil(ffmpegpath, originalFile.getAbsolutePath(), mp4File.getName(), mp4File.getAbsolutePath());
                        //开始视频转换，成功将返回 success
                        result = mp4VideoUtil.generateMp4();
                        if (!result.equals("success")) {
                            XueChengPlusException.cast("处理视频失败,视频地址:{" + bucket + filePath + "},错误信息:{" + result + "}");
                        }
                    } catch (Exception e) {
                        log.error("处理视频文件:{},出错:{}", mediaProcess.getFilePath(), e.getMessage());
                        mediaProcessService.saveProcessFinishStatus(taskId, "3", fileId, null, result);
                        XueChengPlusException.cast(e.getMessage());
                    }
                    //将mp4上传至minio
                    //mp4在minio的存储路径
                    String objectName = getFilePath(fileId, ".mp4");
                    //访问url
                    String url = "/" + bucket + "/" + objectName;
                    try {
                        mediaFilesService.addMediaFilesToMinIO(mp4File.getAbsolutePath(), bucket, objectName, "video/mp4");
                        //将url存储至数据，并更新状态为成功，并将待处理视频记录删除存入历史
                        mediaProcessService.saveProcessFinishStatus(mediaProcess.getId(), "2", fileId, url, null);
                    } catch (Exception e) {
                        log.error("上传视频失败或入库失败,视频地址:{},错误信息:{}", bucket + objectName, e.getMessage());
                        //最终还是失败了
                        mediaProcessService.saveProcessFinishStatus(mediaProcess.getId(), "3", fileId, null, "处理后视频上传或入库失败");
                    }
                } finally {
                    countDownLatch.countDown();
                }
            });


        });


    }

    private String getFilePath(String fileMd5, String fileExt) {
        return fileMd5.substring(0, 1) + "/" + fileMd5.substring(1, 2) + "/" + fileMd5 + "/" + fileMd5 + fileExt;
    }
}
