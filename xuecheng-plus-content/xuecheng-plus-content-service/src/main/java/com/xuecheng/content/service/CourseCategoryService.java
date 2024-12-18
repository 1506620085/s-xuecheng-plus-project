package com.xuecheng.content.service;

import com.xuecheng.content.model.vo.CourseCategoryTreeVO;

import java.util.List;

/**
 * @author Hangz
 * @version 1.0
 * @description 课程类型服务类
 * @date 2024/10/12 14:49
 */
public interface CourseCategoryService {

    /**
     * 课程分类树形结构查询
     *
     * @return
     */
    List<CourseCategoryTreeVO> queryTreeNodes(String id);
}
