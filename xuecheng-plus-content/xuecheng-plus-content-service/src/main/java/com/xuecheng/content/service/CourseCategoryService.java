package com.xuecheng.content.service;

import com.xuecheng.content.model.dto.CourseCategoryTreeDto;

import java.util.List;

/**
 * @author Hangz
 * @version 1.0
 * @description TODO
 * @date 2024/10/12 14:49
 */
public interface CourseCategoryService {
 /**
  * 课程分类树形结构查询
  *
  * @return
  */
 public List<CourseCategoryTreeDto> queryTreeNodes(String id);
}
