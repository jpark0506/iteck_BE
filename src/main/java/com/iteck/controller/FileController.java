package com.iteck.controller;

import com.iteck.dto.ApiResponse;
import com.iteck.service.FileService;

import lombok.AllArgsConstructor;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;

@RestController
@RequestMapping("/api")
@CrossOrigin("*")
@AllArgsConstructor
public class FileController {
    private final FileService fileService;
    @PostMapping("/upload")
    public ApiResponse<?> uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
        return fileService.createExperimentData(file);
    }

}