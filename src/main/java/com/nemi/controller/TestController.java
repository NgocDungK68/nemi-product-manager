package com.nemi.controller;

import com.nemi.entity.PosEntity;
import com.nemi.repository.PosRepository;
import com.nemi.service.PosReAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class TestController {
    private final PosReAuthService posReAuthService;
    private final PosRepository posRepository;

    @GetMapping("/test")
    public String test() {
        return "Test successful";
    }

    @GetMapping("/public-api/test")
    public String test2() {
        return "Test successful";
    }

    @GetMapping("/service-api/test")
    public String testService() {
        return "Test service api successful";
    }

    @PostMapping("/public-api/testReAuth/{posId}")
    public String testReAuth(@PathVariable String posId) {
        PosEntity posEntity = posRepository.findById(posId).orElse(null);
        return posReAuthService.buildReAuthLink(posEntity);
    }
}
