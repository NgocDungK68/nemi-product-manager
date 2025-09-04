package com.nemi.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/test")
    public String test() {
        return "Test successful";
    }

    @GetMapping("/service-api/test")
    public String testService() {
        return "Test service api successful";
    }
}
