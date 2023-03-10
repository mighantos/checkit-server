package com.github.checkit.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ChangeController.MAPPING)
public class ChangeController extends BaseController {

    public static final String MAPPING = "/";

    @GetMapping("/hello")
    public String helloWorld() {
        return "hello world";
    }
}
