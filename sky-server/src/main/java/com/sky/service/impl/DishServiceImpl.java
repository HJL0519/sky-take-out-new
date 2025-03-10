package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.entity.Setmeal;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private DishFlavorMapper dishFlavorMapper;

    @Autowired
    private SetmealDishMapper setmealDishMapper;

    @Autowired
    private SetmealMapper setmealMapper;

    //新增菜品与对应的口味
    //@Transactional事务方法保证事务是原则性的，要么全成功要么全失败
    @Override
    @Transactional
    public void saveWithFlavor(DishDTO dishDTO) {

        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO,dish);

        //向菜品表插入1条数据
        dishMapper.insert(dish);

        //获取insert语句生成的主键值
        Long dishId = dish.getId();

        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && flavors.size() >0){
            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dishId);
            });
            //向口味表中插入n条数据
            dishFlavorMapper.insertBatch(flavors);
        }

    }

    @Override
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {

        PageHelper.startPage(dishPageQueryDTO.getPage(),dishPageQueryDTO.getPageSize());
        Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);
        long total = page.getTotal();
        List<DishVO> records = page.getResult();
        return new PageResult(total,records);
    }

    @Override
    @Transactional
    public void deteleBatch(List<Long> ids) {
        //判断当前菜品是否能被删除--是否存在起售中的菜品
        for (Long id : ids) {
            Dish dish = dishMapper.getById(id);
            if (dish.getStatus() == StatusConstant.ENABLE){
                //菜品处于售卖中，不能删除
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
            }
        }

        //判断当前菜品是否能被删除--是否关联了套餐
        List<Long> setmealIds = setmealDishMapper.getSetmealIdsByDishIds(ids);
        if (setmealIds != null && setmealIds.size() > 0){
            //当前菜品被关联了套餐，不能删除
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }

        //删除菜品表中的菜品数据
        // for (Long id : ids) {
        //     dishMapper.deleteById(id);
        //     //删除口味表中菜品关联的口味数据
        //     dishFlavorMapper.daleteByDishId(id);
        // }

        //根据菜品数据集合批量删除菜品数据
        dishMapper.deleteByIds(ids);

        //根据菜品数据集合批量删除关联的口味数据
        dishFlavorMapper.deleteByDishIds(ids);


    }

    @Override
    public DishVO getById(Long id) {
        Dish dish = dishMapper.getByIdWithFlavor(id);
        List<DishFlavor> dishFlavors = dishFlavorMapper.getByIdWithFlavor(id);
        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish,dishVO);
        BeanUtils.copyProperties(dishFlavors,dishVO);
        return dishVO;
    }

    @Override
    public void updateWithFlavor(DishDTO dishDTO) {

        //修改菜品表基本信息
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO,dish);
        dishMapper.update(dish);

        //口味的操作有很多种可能性，故先把数据库中的全删除，再把修改后的数据插入
        dishFlavorMapper.daleteByDishId(dishDTO.getId());

        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && flavors.size() >0){
            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dishDTO.getId());
            });
            //向口味表中插入n条数据
            dishFlavorMapper.insertBatch(flavors);
        }
    }

    @Override
    @Transactional
    public void startOrStop(Integer status,Long id) {
        Dish dish = new Dish();
        dish.setId(id);
        dish.setStatus(status);
        dishMapper.update(dish);

        //如果菜品停售，则包含该菜品的套餐也停售
        if (status == StatusConstant.DISABLE){
            List<Long> dishIds = new ArrayList<>();
            dishIds.add(id);
            List<Long> setmealIdsByDishIds = setmealDishMapper.getSetmealIdsByDishIds(dishIds);
            if (setmealIdsByDishIds != null && setmealIdsByDishIds.size() >0){
                for (Long setmealIdsByDishId : setmealIdsByDishIds) {
                    Setmeal setmeal = Setmeal.builder()
                            .id(setmealIdsByDishId)
                            .status(StatusConstant.DISABLE)
                            .build();
                    setmealMapper.update(setmeal);
                }
            }
        }
    }
}
