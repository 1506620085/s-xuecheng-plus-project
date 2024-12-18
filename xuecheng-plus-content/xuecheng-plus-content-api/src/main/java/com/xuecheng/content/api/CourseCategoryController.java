package com.xuecheng.content.api;

import com.xuecheng.base.model.BaseResponse;
import com.xuecheng.base.model.ResultUtils;
import com.xuecheng.content.model.vo.CourseCategoryTreeVO;
import com.xuecheng.content.service.CourseCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author Hangz
 * @version 1.0
 * @description 课程分类相关接口
 * @date 2024/10/12 11:54
 */
@RestController
public class CourseCategoryController {

    @Autowired
    CourseCategoryService courseCategoryService;

    @GetMapping("/course-category/tree-nodes")
    public BaseResponse<List<CourseCategoryTreeVO>> queryTreeNodes() {
        return ResultUtils.success(courseCategoryService.queryTreeNodes("1"));
    }


}
