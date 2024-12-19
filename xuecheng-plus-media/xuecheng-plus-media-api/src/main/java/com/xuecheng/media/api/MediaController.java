package com.xuecheng.media.api;

import com.xuecheng.media.model.dto.uploadFIle.UploadFileRequest;
import com.xuecheng.media.model.vo.UploadFileVO;
import com.xuecheng.media.service.MediaFilesService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;

/**
 *
 */
@Api(value = "媒资文件管理接口", tags = "媒资文件管理接口")
@RestController
@RequestMapping("/media")
@Slf4j
public class MediaController {

    @Resource
    private MediaFilesService mediaFilesService;

    /**
     * 上传图片
     *
     * @param filedata
     * @return
     */
    @ApiOperation("上传图片接口")
    @PostMapping(value = "/upload/coursefile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UploadFileVO uploadFile(@RequestPart("filedata") MultipartFile filedata) throws IOException {
        //准备上传文件的信息
        UploadFileRequest uploadFileParamsDto = new UploadFileRequest();
        //原始文件名称
        uploadFileParamsDto.setFilename(filedata.getOriginalFilename());
        //文件大小
        uploadFileParamsDto.setFileSize(filedata.getSize());
        //文件类型
        uploadFileParamsDto.setFileType("001001");
        //创建一个临时文件
        File tempFile = File.createTempFile("minio", ".temp");
        filedata.transferTo(tempFile);
        Long companyId = 1232141425L;
        //文件路径
        String localFilePath = tempFile.getAbsolutePath();
        //调用service上传图片
        UploadFileVO uploadFileResultDto = mediaFilesService.uploadFile(companyId, uploadFileParamsDto, localFilePath);
        // 上传完成删除临时文件
        boolean delete = tempFile.delete();
        if (!delete) {
            log.error("删除临时文件失败");
        }
        return uploadFileResultDto;
    }
}
