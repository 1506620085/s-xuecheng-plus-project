package com.xuecheng.content.api;

import com.xuecheng.base.model.BaseResponse;
import com.xuecheng.content.model.dto.BindTeachplanMediaDto;
import com.xuecheng.content.model.po.TeachplanMedia;
import com.xuecheng.content.service.TeachplanMediaService;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
public class TeachplanController {

    @Resource
    private TeachplanMediaService teachplanMediaService;

    @ApiOperation(value = "课程计划和媒资信息绑定")
    @PostMapping("/teachplan/association/media")
    public BaseResponse<TeachplanMedia> associationMedia(@RequestBody BindTeachplanMediaDto bindTeachplanMediaDto) {
        TeachplanMedia teachplanMedia = teachplanMediaService.associationMedia(bindTeachplanMediaDto);
        return BaseResponse.success(teachplanMedia);
    }
}
