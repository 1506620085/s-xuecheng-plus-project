package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.mapper.TeachplanMapper;
import com.xuecheng.content.mapper.TeachplanMediaMapper;
import com.xuecheng.content.model.dto.BindTeachplanMediaDto;
import com.xuecheng.content.model.po.Teachplan;
import com.xuecheng.content.model.po.TeachplanMedia;
import com.xuecheng.content.service.TeachplanMediaService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * @author Hang
 * @description 针对表【teachplan_media】的数据库操作Service实现
 * @createDate 2024-12-18 10:22:36
 */
@Service
public class TeachplanMediaServiceImpl extends ServiceImpl<TeachplanMediaMapper, TeachplanMedia>
        implements TeachplanMediaService {

    @Resource
    private TeachplanMapper teachplanMapper;

    @Override
    public TeachplanMedia  associationMedia(BindTeachplanMediaDto bindTeachplanMediaDto) {
        if (bindTeachplanMediaDto == null) {
            XueChengPlusException.cast("传入参数为空");
        }
        String mediaId = bindTeachplanMediaDto.getMediaId();
        String fileName = bindTeachplanMediaDto.getFileName();
        Long teachplanId = bindTeachplanMediaDto.getTeachplanId();
        Teachplan teachplan = teachplanMapper.selectById(teachplanId);
        if (teachplan == null) {
            XueChengPlusException.cast("教学计划不存在");
        }
        Integer grade = teachplan.getGrade();
        if (grade != 2) {
            XueChengPlusException.cast("只允许第二级教学计划绑定媒资文件");
        }
        //课程id
        Long courseId = teachplan.getCourseId();
        //先删除原来该教学计划绑定的媒资
        this.remove(new LambdaQueryWrapper<TeachplanMedia>().eq(TeachplanMedia::getTeachplanId, teachplanId));

        //再添加教学计划与媒资的绑定关系
        TeachplanMedia teachplanMedia = new TeachplanMedia();
        teachplanMedia.setMediaId(mediaId);
        teachplanMedia.setMediaFilename(fileName);
        teachplanMedia.setTeachplanId(teachplanId);
        teachplanMedia.setCourseId(courseId);
        teachplanMedia.setCreateDate(LocalDateTime.now());

        // 保存绑定关系到数据库中
        boolean save = this.save(teachplanMedia);
        return teachplanMedia;
    }
}




