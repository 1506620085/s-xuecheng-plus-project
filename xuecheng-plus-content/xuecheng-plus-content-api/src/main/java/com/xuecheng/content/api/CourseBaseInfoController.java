package com.xuecheng.content.api;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuecheng.base.model.BaseResponse;
import com.xuecheng.base.model.ResultUtils;
import com.xuecheng.content.model.dto.courseBase.CourseBaseAddRequest;
import com.xuecheng.content.model.dto.courseBase.CourseBaseQueryRequest;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.model.vo.CourseBaseVO;
import com.xuecheng.content.service.CourseBaseService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Hangz
 * @version 1.0
 * @description TODO
 * @date 2024/10/11 15:44
 */
@Api(value = "课程信息管理接口", tags = "课程信息管理接口")
@RestController
public class CourseBaseInfoController {

    @Autowired
    CourseBaseService courseBaseService;

    @ApiOperation("课程查询接口")
    @PostMapping("/course/list")
    public BaseResponse<Page<CourseBase>> list(@RequestBody CourseBaseQueryRequest courseBaseQueryRequest) {
        long current = courseBaseQueryRequest.getCurrent();
        long size = courseBaseQueryRequest.getPageSize();
        Page<CourseBase> courseBasePage = courseBaseService.page(new Page<>(current, size),
                courseBaseService.getQueryWrapper(courseBaseQueryRequest));
        return ResultUtils.success(courseBasePage);


    }

    @ApiOperation("新增课程")
    @PostMapping("/course")
    public BaseResponse<CourseBaseVO> addCourseBase(@RequestBody CourseBaseAddRequest courseBaseAddRequest) {
        //获取到用户所属机构的id
        Long companyId = 1232141425L;
        return ResultUtils.success(courseBaseService.addCourseBase(companyId, courseBaseAddRequest));

    }

}
