package org.demo.whs.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/v1/product")
@RestController
@RequiredArgsConstructor
@Slf4j
public class ProductController {

    public final ProductService productService;

    public ResponseEntity<String> getProducts() {
        log.info("Fetching products");
        // Placeholder response
        return ResponseEntity.ok("List of products");
    }

}
