package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.mapper.*;
import com.xuecheng.content.model.dto.AddCourseDto;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.EditCourseDto;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.*;
import com.xuecheng.content.service.CourseBaseInfoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class CourseBaseInfoServiceImpl implements CourseBaseInfoService {

    @Autowired
    CourseBaseMapper courseBaseMapper;

    @Autowired
    CourseMarketMapper courseMarketMapper;

    @Autowired
    CourseCategoryMapper courseCategoryMapper;

    @Autowired
    CourseMarketServiceImpl courseMarketService;

    @Autowired
    TeachplanMapper teachplanMapper;

    @Autowired
    CourseTeacherMapper courseTeacherMapper;

    @Override
    public PageResult<CourseBase> queryCourseBaseList(PageParams pageParams, QueryCourseParamsDto queryCourseParamsDto) {
        LambdaQueryWrapper<CourseBase> queryWrapper = new LambdaQueryWrapper<>();

        //根据课程名称模糊查询
        queryWrapper.like(StringUtils.isNotEmpty(queryCourseParamsDto.getCourseName()),CourseBase::getName,queryCourseParamsDto.getCourseName());
        //根据课程状态查询
        queryWrapper.eq(StringUtils.isNotEmpty(queryCourseParamsDto.getAuditStatus()),CourseBase::getAuditStatus,queryCourseParamsDto.getAuditStatus());
        //根据课程发布状态查询
        queryWrapper.eq(StringUtils.isNotEmpty(queryCourseParamsDto.getPublishStatus()),CourseBase::getStatus,queryCourseParamsDto.getPublishStatus());
        //分页参数
        Page<CourseBase> page = new Page<>(pageParams.getPageNo(),pageParams.getPageSize());

        //分页查询
        Page<CourseBase> pageResult = courseBaseMapper.selectPage(page, queryWrapper);

        //数据
        List<CourseBase> items = pageResult.getRecords();
        long total = pageResult.getTotal();

        //准备返回数据 List<T> items, long counts, long page, long pageSize

        return new PageResult<CourseBase>(items,total,pageParams.getPageNo(),pageParams.getPageSize());
    }

    @Override
    @Transactional
    public CourseBaseInfoDto createCourseBase(Long companyId, AddCourseDto addCourseDto) {

        //对参数进行合法性校验

        //对数据进行封装，调用mapper进行数据持久化
        CourseBase courseBase = new CourseBase();
        BeanUtils.copyProperties(addCourseDto,courseBase);
        courseBase.setCompanyId(companyId);
        courseBase.setCreateDate(LocalDateTime.now());
        courseBase.setAuditStatus("202002"); //默认审核状态
        courseBase.setStatus("203001"); //默认发布状态
        int insert = courseBaseMapper.insert(courseBase);
        Long courseId = courseBase.getId();

        CourseMarket courseMarket = new CourseMarket();
        BeanUtils.copyProperties(addCourseDto,courseMarket);
        courseMarket.setId(courseId);
        //校验如果课程为收费，价格必须输入
        int insert1 = this.saveCourseMarket(courseMarket);
//        String charge = addCourseDto.getCharge();
//        if (charge.equals("201001")){
//            if (courseMarket.getPrice() == null || courseMarket.getPrice()<=0){
//                XueChengPlusException.cast("课程为收费但价格为空");
//            }
//        }
//        int insert1 = courseMarketMapper.insert(courseMarket);

        if (insert1<=0 || insert<=0){
            throw new RuntimeException("添加课程失败");
        }
        //组装返回结果
        CourseBaseInfoDto courseBaseInfo = getCourseBaseInfo(courseId);
        return courseBaseInfo;
    }
    //根据课程id查询课程基本信息，包括基本信息和营销信息
    @Override
    public CourseBaseInfoDto getCourseBaseInfo(Long courseId){

        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        CourseMarket courseMarket = courseMarketMapper.selectById(courseId);

        if(courseBase == null){
            return null;
        }
        CourseBaseInfoDto courseBaseInfoDto = new CourseBaseInfoDto();
        BeanUtils.copyProperties(courseBase,courseBaseInfoDto);
        if(courseMarket != null){
            BeanUtils.copyProperties(courseMarket,courseBaseInfoDto);
        }

        //查询分类名称
        CourseCategory courseCategoryBySt = courseCategoryMapper.selectById(courseBase.getSt());
        courseBaseInfoDto.setStName(courseCategoryBySt.getName());
        CourseCategory courseCategoryByMt = courseCategoryMapper.selectById(courseBase.getMt());
        courseBaseInfoDto.setMtName(courseCategoryByMt.getName());

        return courseBaseInfoDto;

    }

    @Override
    public CourseBaseInfoDto updateCourseBase(Long companyId, EditCourseDto dto) {
        //校验
        Long id = dto.getId();
        CourseBase courseBase = courseBaseMapper.selectById(id);
        if (courseBase==null){
            XueChengPlusException.cast("课程不存在");
        }
        if (!Objects.equals(companyId, courseBase.getCompanyId())){
            XueChengPlusException.cast("只能修改本机构课程");
        }

        //封装数据
        BeanUtils.copyProperties(dto,courseBase);
        courseBase.setCreateDate(LocalDateTime.now());
        CourseMarket courseMarket = new CourseMarket();
        BeanUtils.copyProperties(dto,courseMarket);

//        String charge = courseMarket.getCharge();
//        if (charge.equals("201001")){
//            if (courseMarket.getPrice() == null || courseMarket.getPrice()<=0){
//                XueChengPlusException.cast("课程为收费但价格为空");
//            }
//        }


        //请求数据库
        int insert1 = this.saveCourseMarket(courseMarket);
        courseBaseMapper.updateById(courseBase);

        CourseBaseInfoDto courseBaseInfo = this.getCourseBaseInfo(id);

        return courseBaseInfo;
    }

    @Override
    public void deleteCourse(Long courseId) {
        courseBaseMapper.deleteById(courseId);
        courseMarketMapper.deleteById(courseId);
        LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Teachplan::getCourseId,courseId);
        teachplanMapper.delete(queryWrapper);
        LambdaQueryWrapper<CourseTeacher> queryWrapper2 = new LambdaQueryWrapper<>();
        queryWrapper2.eq(CourseTeacher::getCourseId,courseId);
        courseTeacherMapper.delete(queryWrapper2);
    }


    //抽取对营销信息的保存
    private int saveCourseMarket(CourseMarket courseMarket){

        String charge = courseMarket.getCharge();
        if (StringUtils.isBlank(charge)){
            XueChengPlusException.cast("收费规则没有选择");
        }
        if (charge.equals("201001")){
            if (courseMarket.getPrice() == null || courseMarket.getPrice()<=0){
                XueChengPlusException.cast("课程为收费但价格为空");
            }
        }
        boolean b = courseMarketService.saveOrUpdate(courseMarket);
        return b ? 1:0;

    }

}
