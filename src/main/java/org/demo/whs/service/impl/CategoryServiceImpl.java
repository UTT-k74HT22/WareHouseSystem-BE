package org.demo.whs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.mapper.CategoryMapper;
import org.demo.whs.repository.CategoryRepository;
import org.demo.whs.service.CategoryService;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
}
