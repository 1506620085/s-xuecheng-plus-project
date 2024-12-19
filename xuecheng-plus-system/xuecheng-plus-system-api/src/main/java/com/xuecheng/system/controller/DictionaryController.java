package com.xuecheng.system.controller;

import com.xuecheng.base.model.BaseResponse;
import com.xuecheng.base.model.ResultUtils;
import com.xuecheng.system.model.po.Dictionary;
import com.xuecheng.system.service.DictionaryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * 数据字典 前端控制器
 * </p>
 *
 * @author itcast
 */
@Slf4j
@RestController
public class DictionaryController {

    @Autowired
    private DictionaryService dictionaryService;

    @GetMapping("/dictionary/all")
    public BaseResponse<List<Dictionary>> queryAll() {
        return ResultUtils.success(dictionaryService.queryAll());
    }

    @GetMapping("/dictionary/code/{code}")
    public BaseResponse<Dictionary> getByCode(@PathVariable String code) {
        return ResultUtils.success(dictionaryService.getByCode(code));
    }
}
