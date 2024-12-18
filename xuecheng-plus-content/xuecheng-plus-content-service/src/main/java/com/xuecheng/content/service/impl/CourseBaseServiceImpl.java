package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuecheng.base.constant.CommonConstant;
import com.xuecheng.base.exception.BusinessException;
import com.xuecheng.base.exception.ErrorCode;
import com.xuecheng.base.utils.SqlUtils;
import com.xuecheng.content.mapper.CourseBaseMapper;
import com.xuecheng.content.mapper.CourseCategoryMapper;
import com.xuecheng.content.mapper.CourseMarketMapper;
import com.xuecheng.content.model.dto.courseBase.CourseBaseAddRequest;
import com.xuecheng.content.model.dto.courseBase.CourseBaseQueryRequest;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.model.po.CourseMarket;
import com.xuecheng.content.model.vo.CourseBaseVO;
import com.xuecheng.content.service.CourseBaseService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * @author Hangz
 * @version 1.0
 * @description TODO
 * @date 2024/10/12 10:16
 */
@Slf4j
@Service
public class CourseBaseServiceImpl extends ServiceImpl<CourseBaseMapper, CourseBase> implements CourseBaseService {
    @Autowired
    CourseBaseMapper courseBaseMapper;

    @Autowired
    CourseMarketMapper courseMarketMapper;

    @Autowired
    CourseCategoryMapper courseCategoryMapper;


    @Transactional
    @Override
    public CourseBaseVO addCourseBase(Long companyId, CourseBaseAddRequest courseBaseAddRequest) {
        //参数的合法性校验
        if (StringUtils.isBlank(courseBaseAddRequest.getName())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "课程名称为空");
        }
        if (StringUtils.isBlank(courseBaseAddRequest.getMt())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "课程分类为空");
        }
        if (StringUtils.isBlank(courseBaseAddRequest.getSt())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "课程分类为空");
        }
        if (StringUtils.isBlank(courseBaseAddRequest.getGrade())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "课程等级为空");
        }
        if (StringUtils.isBlank(courseBaseAddRequest.getTeachmode())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "教育模式为空");
        }
        if (StringUtils.isBlank(courseBaseAddRequest.getUsers())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "适应人群为空");
        }
        if (StringUtils.isBlank(courseBaseAddRequest.getCharge())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "收费规则为空");
        }
        //向课程基本信息表course_base写入数据
        CourseBase courseBaseNew = new CourseBase();
        //将传入的页面的参数放到courseBaseNew对象
//        courseBaseNew.setName(dto.getName());
//        courseBaseNew.setDescription(dto.getDescription());
        //上边的从原始对象中get拿数据向新对象set，比较复杂
        BeanUtils.copyProperties(courseBaseAddRequest, courseBaseNew);//只要属性名称一致就可以拷贝
        courseBaseNew.setCompanyId(companyId);
        courseBaseNew.setCreateDate(LocalDateTime.now());
        //审核状态默认为未提交
        courseBaseNew.setAuditStatus("202002");
        //发布状态为未发布
        courseBaseNew.setStatus("203001");
        //插入数据库
        int insert = courseBaseMapper.insert(courseBaseNew);
        if (insert <= 0) {
            throw new RuntimeException("添加课程失败");
        }
        //向课程营销系courese_market写入数据
        CourseMarket courseMarketNew = new CourseMarket();
        //将页面输入的数据拷贝到courseMarketNew
        BeanUtils.copyProperties(courseBaseAddRequest, courseMarketNew);
        //课程的id
        Long courseId = courseBaseNew.getId();
        courseMarketNew.setId(courseId);
        //保存营销信息
        saveCourseMarket(courseMarketNew);
        //从数据库查询课程的详细信息，包括两部分
        return getCourseBaseVO(courseId);
    }

    /**
     * 获取查询包装类
     *
     * @param courseBaseQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<CourseBase> getQueryWrapper(CourseBaseQueryRequest courseBaseQueryRequest) {
        QueryWrapper<CourseBase> queryWrapper = new QueryWrapper<>();
        if (courseBaseQueryRequest == null) {
            return queryWrapper;
        }
        String name = courseBaseQueryRequest.getName();
        String auditStatus = courseBaseQueryRequest.getAuditStatus();
        String status = courseBaseQueryRequest.getStatus();
        String sortField = courseBaseQueryRequest.getSortField();
        String sortOrder = courseBaseQueryRequest.getSortOrder();
        // 拼接查询条件
        queryWrapper.like(StringUtils.isNotBlank(name), "name", name);
        queryWrapper.eq(StringUtils.isNotBlank(auditStatus), "audit_status", auditStatus);
        queryWrapper.eq(StringUtils.isNotBlank(status), "status", status);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    //查询课程信息
    public CourseBaseVO getCourseBaseVO(long courseId) {
        //从课程基本信息表查询
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        if (courseBase == null) {
            return null;
        }
        //从课程营销表查询
        CourseMarket courseMarket = courseMarketMapper.selectById(courseId);
        //组装在一起
        CourseBaseVO courseBaseVO = new CourseBaseVO();
        BeanUtils.copyProperties(courseBase, courseBaseVO);
        if (courseMarket != null) {
            BeanUtils.copyProperties(courseMarket, courseBaseVO);
        }
        //通过 courseCategoryMapper 查询分类信息，将分类名称放在 courseBaseVO 对象
        //todo：课程分类的名称设置到 courseBaseVO
        return courseBaseVO;

    }

    //单独写一个方法保存营销信息，逻辑：存在则更新，不存在则添加
    private int saveCourseMarket(CourseMarket courseMarketNew) {
        //参数的合法性校验
        String charge = courseMarketNew.getCharge();
        if (StringUtils.isEmpty(charge)) {
            throw new RuntimeException("收费规则为空");
        }
        //如果课程收费，价格没有填写也需要抛出异常
        if (charge.equals("201001")) {
            if (courseMarketNew.getPrice() == null || courseMarketNew.getPrice().floatValue() <= 0) {
//               throw new RuntimeException("课程的价格不能为空并且必须大于0");
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "课程的价格不能为空并且必须大于0");
            }
        }
        //从数据库查询营销信息,存在则更新，不存在则添加
        Long id = courseMarketNew.getId();//主键
        CourseMarket courseMarket = courseMarketMapper.selectById(id);
        if (courseMarket == null) {
            //插入数据库
            int insert = courseMarketMapper.insert(courseMarketNew);
            return insert;
        } else {
            //将courseMarketNew拷贝到courseMarket
            BeanUtils.copyProperties(courseMarketNew, courseMarket);
            courseMarket.setId(courseMarketNew.getId());
            //更新
            int i = courseMarketMapper.updateById(courseMarket);
            return i;
        }
    }
}
