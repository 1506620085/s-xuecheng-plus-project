package com.xuecheng.content.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.model.dto.courseBase.CourseBaseAddRequest;
import com.xuecheng.content.model.dto.courseBase.CourseBaseQueryRequest;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.model.vo.CourseBaseVO;

/**
 * @author Hangz
 * @version 1.0
 * @description 课程信息管理接口
 * @date 2024/10/12 10:14
 */
public interface CourseBaseService extends IService<CourseBase> {

    /**
     * 新增课程
     *
     * @param companyId            机构id
     * @param courseBaseAddRequest 课程信息
     * @return 课程详细信息
     */
    public CourseBaseVO addCourseBase(Long companyId, CourseBaseAddRequest courseBaseAddRequest);

    /**
     * 获取查询条件
     *
     * @param courseBaseQueryRequest
     * @return
     */
    QueryWrapper<CourseBase> getQueryWrapper(CourseBaseQueryRequest courseBaseQueryRequest);
}
