package com.xuecheng.content.service.impl;

import com.xuecheng.content.mapper.CourseCategoryMapper;
import com.xuecheng.content.model.dto.CourseCategoryTreeDto;
import com.xuecheng.content.service.CourseCategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Service
@Slf4j
public class CourseCategoryServiceImpl implements CourseCategoryService {

    @Autowired
    CourseCategoryMapper courseCategoryMapper;
    @Override
    public List<CourseCategoryTreeDto> queryTreeNodes(String id) {
        //得到了根节点下边的所有子节点
        List<CourseCategoryTreeDto> categoryTreeDtos = courseCategoryMapper.selectTreeNodes(id);


        List<CourseCategoryTreeDto> courseCategoryTreeDtos = new ArrayList<>();
        HashMap<String,CourseCategoryTreeDto> nodeMap = new HashMap<>();
        //将数据封装到list中
        categoryTreeDtos.stream().forEach(item->{
            nodeMap.put(item.getId(), item);
            if (item.getParentid().equals(id)){
                courseCategoryTreeDtos.add(item);
            }
            String parentid = item.getParentid();
            CourseCategoryTreeDto parentNode = nodeMap.get(parentid);
            if (parentNode!=null) {
                List childrenTreeNodes = parentNode.getChildrenTreeNodes();
                if (childrenTreeNodes == null) {
                    parentNode.setChildrenTreeNodes(new ArrayList<CourseCategoryTreeDto>());
                }
                parentNode.getChildrenTreeNodes().add(item);
            }


        });

        //返回list中只包括了根节点的直接下属结点
        return courseCategoryTreeDtos;
    }
}
