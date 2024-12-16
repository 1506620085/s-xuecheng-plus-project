package com.xuecheng.media.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xuecheng.media.model.po.MediaProcess;

import java.util.List;

/**
 * @author Hang
 * @description 针对表【media_process】的数据库操作Service
 * @createDate 2024-12-16 21:03:31
 */
public interface MediaProcessService extends IService<MediaProcess> {

    /**
     * @param shardIndex 分片序号
     * @param shardTotal 分片总数
     * @param count      获取记录数
     * @return java.util.List<com.xuecheng.media.model.po.MediaProcess>
     * @description 获取待处理任务
     * @author Hangz
     */
    List<MediaProcess> getMediaProcessList(int shardIndex, int shardTotal, int count);

    /**
     * 开启一个任务
     *
     * @param id 任务id
     * @return true开启任务成功，false开启任务失败
     */
    Boolean startTask(long id);

    /**
     * @param taskId   任务id
     * @param status   任务状态
     * @param fileId   文件id
     * @param url      url
     * @param errorMsg 错误信息
     * @return void
     * @description 保存任务结果
     * @author Mr.M
     * @date 2022/10/15 11:29
     */
    void saveProcessFinishStatus(Long taskId, String status, String fileId, String url, String errorMsg);
}
