package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.mapper.CourseTeacherMapper;
import com.xuecheng.content.mapper.TeachplanMapper;
import com.xuecheng.content.mapper.TeachplanMediaMapper;
import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.CourseTeacher;
import com.xuecheng.content.model.po.Teachplan;
import com.xuecheng.content.model.po.TeachplanMedia;
import com.xuecheng.content.service.TeachplanService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class TeachplanServiceImpl implements TeachplanService {


    @Autowired
    TeachplanMapper teachplanMapper;

    @Autowired
    TeachplanMediaMapper teachplanMediaMapper;

    @Autowired
    CourseTeacherMapper courseTeacherMapper;
    @Override
    public List<TeachplanDto> findTeachplanTree(Long courseId) {
        return teachplanMapper.selectTreeNodes(courseId);
    }

    //新增、修改
    @Override
    public void saveTeachplan(SaveTeachplanDto dto) {
        Long id = dto.getId();
        Teachplan teachplan = teachplanMapper.selectById(id);
        if (id==null){
            teachplan = new Teachplan();
            BeanUtils.copyProperties(dto,teachplan);
            //计算默认 orderby
            teachplan.setOrderby(getTeachplanCount(dto.getCourseId(),dto.getParentid())+1);

            teachplanMapper.insert(teachplan);
        }else {
            BeanUtils.copyProperties(dto,teachplan);
            teachplanMapper.updateById(teachplan);
        }

    }

    @Override
    public void deleteTeachplan(Long Id) {
        Teachplan teachplan = teachplanMapper.selectById(Id);
        if (teachplan.getParentid()==0){
            //章 删除第一级别的章时要求章下边没有小节方可删除
            int count = this.getTeachplanCount(teachplan.getCourseId(), Id);
            if (count==0){
                teachplanMapper.deleteById(Id);
            }else {
                XueChengPlusException.cast("本章中包含其他小节，无法删除");
            }
        }else {
            //删除第二级别的小节的同时需要将其它关联的视频信息也删除。
            teachplanMapper.deleteById(Id);
            LambdaQueryWrapper<TeachplanMedia> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(TeachplanMedia::getTeachplanId,Id);
            teachplanMediaMapper.delete(queryWrapper);
        }

    }

    @Override
    public List<CourseTeacher> getCourseTeacher(Long courseId) {
        LambdaQueryWrapper<CourseTeacher> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CourseTeacher::getCourseId,courseId);
        return courseTeacherMapper.selectList(queryWrapper);
    }


    //计算新添加计划的orderby

    private int getTeachplanCount(long courseId,long parentId){
        LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Teachplan::getCourseId,courseId);
        queryWrapper.eq(Teachplan::getParentid,parentId);
        Integer count = teachplanMapper.selectCount(queryWrapper);
        return count;
    }



}
