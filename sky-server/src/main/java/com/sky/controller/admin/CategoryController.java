package com.sky.controller.admin;

import com.sky.dto.CategoryDTO;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.entity.Category;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.CategoryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("adminCategoryController ")
@Slf4j
@RequestMapping("/admin/category")
@Api(tags = "分类相关接口")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;


    //新增分类
    @PostMapping
    @ApiOperation("新增分类")
    public Result save(@RequestBody CategoryDTO categoryDTO){
        log.info("新增分类,{}",categoryDTO);
        categoryService.save(categoryDTO);
        return Result.success();
    }

    //分页展示
    @GetMapping("/page")
    @ApiOperation("分页展示")
    public Result<PageResult> page(CategoryPageQueryDTO categoryPageQueryDTO){
        log.info("分页展示,{}",categoryPageQueryDTO);
        PageResult pageResult = categoryService.page(categoryPageQueryDTO);
        return Result.success(pageResult);
    }

    //启用禁用菜品分类
    @PostMapping("/status/{status}")
    @ApiOperation("启用禁用菜品分类")
    public Result startOrStop(@PathVariable Integer status,Long id){
        log.info("启用禁用菜品分类,{},{}",status,id);
        categoryService.startOrStop(status,id);
        return Result.success();
    }

    //根据类型查询分类
    @GetMapping("/list")
    @ApiOperation("根据类型查询分类")
    public Result<List<Category>> list(Integer type){
        List<Category> list = categoryService.list(type);
        return Result.success(list);
    }

    //修改分类
    @PutMapping
    @ApiOperation("修改分类")
    public Result updateCategory(@RequestBody CategoryDTO categoryDTO){
        log.info("修改分类,{}",categoryDTO);
        categoryService.updateCategory(categoryDTO);
        return Result.success();
    }

    //根据id删除分类
    @DeleteMapping
    @ApiOperation("根据id删除分类")
    public Result deleteCategory(Long id){
        log.info("根据id删除分类,{}",id);
        categoryService.deleteCategoty(id);
        return Result.success();
    }
}
