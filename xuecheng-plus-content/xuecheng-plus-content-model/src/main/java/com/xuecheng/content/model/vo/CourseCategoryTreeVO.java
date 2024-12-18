package com.xuecheng.content.model.vo;

import com.xuecheng.content.model.po.CourseCategory;
import lombok.Data;

import java.util.List;

/**
 * @author Hangz
 * @version 1.0
 * @description 课程类型树状结构
 */
@Data
public class CourseCategoryTreeVO extends CourseCategory implements java.io.Serializable {

    //子节点
    List<CourseCategoryTreeVO> childrenTreeNodes;

}
