package com.xuecheng.media.model.dto;

import lombok.Data;

/**
 * 上传普通文件请求参数
 */
@Data
public class UploadFileParamsDto {

    /**
     * 文件名称
     */
    private String filename;

    /**
     * 文件类型（图片、文档，视频）
     */
    private String fileType;

    /**
     * 标签
     */
    private String tags;

    /**
     * 文件大小
     */
    private Long fileSize;

    /**
     * 上传人
     */
    private String username;

    /**
     * 备注
     */
    private String remark;
}
