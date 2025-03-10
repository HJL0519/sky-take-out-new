package com.sky.mapper;

import com.sky.annotation.AutoFill;
import com.sky.entity.Setmeal;
import com.sky.enumeration.OperationType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SetmealMapper {

    @Select("select count(id) from setmeal where category_id = #{category_id}")
    Integer getByCategoryId(Long id);

    @AutoFill(value = OperationType.UPDATE)
    void update(Setmeal setmeal);
}
