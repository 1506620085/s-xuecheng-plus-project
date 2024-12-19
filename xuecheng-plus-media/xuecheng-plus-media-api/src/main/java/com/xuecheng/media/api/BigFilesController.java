package com.xuecheng.media.api;

import com.xuecheng.base.model.BaseResponse;
import com.xuecheng.base.model.ResultUtils;
import com.xuecheng.media.model.dto.uploadFIle.UploadFileRequest;
import com.xuecheng.media.service.MediaFilesService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;

/**
 * 大文件上传接口
 */
@Api(value = "大文件上传接口", tags = "大文件上传接口")
@RestController
public class BigFilesController {

    @Resource
    private MediaFilesService mediaFilesService;

    @ApiOperation(value = "文件上传前检查文件")
    @PostMapping("/checkFile")
    public BaseResponse<Boolean> checkFile(@RequestParam("fileMd5") String fileMd5) {
        Boolean checkFileBoolean = mediaFilesService.checkFile(fileMd5);
        return ResultUtils.success(checkFileBoolean);
    }

    @ApiOperation(value = "分块文件上传前的检测")
    @PostMapping("/checkChunk")
    public BaseResponse<Boolean> checkChunk(@RequestParam("fileMd5") String fileMd5,
                                            @RequestParam("chunk") int chunk) {
        Boolean checkChunkBoolean = mediaFilesService.checkChunk(fileMd5, chunk);
        return ResultUtils.success(checkChunkBoolean);
    }

    @ApiOperation(value = "上传分块文件")
    @PostMapping("/uploadChunk")
    public BaseResponse<Boolean> uploadChunk(@RequestParam("file") MultipartFile file,
                                             @RequestParam("fileMd5") String fileMd5,
                                             @RequestParam("chunk") int chunk) throws Exception {
        // 创建临时文件
        File minioTemp = File.createTempFile("minio", ".temp");
        file.transferTo(minioTemp);
        String absolutePath = minioTemp.getAbsolutePath();
        Boolean uploadChunkBoolean = mediaFilesService.uploadChunk(absolutePath, fileMd5, chunk);
        return ResultUtils.success(uploadChunkBoolean);
    }

    @ApiOperation(value = "合并分块文件")
    @PostMapping("/mergeFile")
    public BaseResponse<Boolean> mergeFile(@RequestParam("fileMd5") String fileMd5,
                                           @RequestParam("fileName") String fileName,
                                           @RequestParam("chunkTotal") int chunkTotal) {
        Long companyId = 1232141425L;
        //准备上传文件的信息
        UploadFileRequest uploadFileRequest = new UploadFileRequest();
        //原始文件名称
        uploadFileRequest.setFilename(fileName);
        //文件大小
        uploadFileRequest.setTags("视频文件");
        //文件类型
        uploadFileRequest.setFileType("001002");
        Boolean mergeFileBoolean = mediaFilesService.mergeFile(companyId, fileMd5, chunkTotal, uploadFileRequest);
        return ResultUtils.success(mergeFileBoolean);
    }
}
