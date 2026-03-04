package org.demo.whs.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.service.CategoryService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/v1/categories")
@RestController
@RequiredArgsConstructor
@Slf4j
@Validated
public class CategoryController {

    private final CategoryService categoryService;
}
