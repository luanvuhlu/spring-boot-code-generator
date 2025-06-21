package com.example.demo.controller;

import com.example.demo.controller.base.BaseProductController;
import com.example.demo.service.ProductService;
import org.springframework.web.bind.annotation.*;

/**
 * Custom REST controller for Product API endpoints.
 * 
 * This controller extends BaseProductController and provides the main API endpoints
 * at /api/products. It demonstrates how to create a custom controller that overrides
 * the default generated controller.
 * 
 * The @Primary annotation ensures this controller takes precedence over the default
 * generated controller for dependency injection.
 */
@RestController
@RequestMapping("/api/products")
public class CustomProductController extends BaseProductController {

    public CustomProductController(ProductService productService) {
        super(productService);
    }
}
