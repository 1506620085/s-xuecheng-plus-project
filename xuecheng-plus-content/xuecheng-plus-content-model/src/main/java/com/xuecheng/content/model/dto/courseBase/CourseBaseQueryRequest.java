package com.xuecheng.content.model.dto.courseBase;

import com.xuecheng.base.model.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
public class CourseBaseQueryRequest extends PageRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 课程名称
     */
    private String name;

    /**
     * 审核状态
     */
    private String auditStatus;

    /**
     * 课程发布状态 未发布  已发布 下线
     */
    private String status;
}
