package com.iteck.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iteck.dto.ApiResponse;
import com.iteck.dto.MetaDto;
import com.iteck.service.ExperimentService;

import lombok.AllArgsConstructor;

import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.beans.PropertyEditorSupport;
import java.io.*;
import java.util.Map;

@RestController
@RequestMapping("/exp")
@CrossOrigin("*")
@AllArgsConstructor
public class ExperimentController {
    private final ExperimentService experimentService;

    @InitBinder
    public void initBinder(WebDataBinder binder) { // 프론트의 폼 데이터와 백의 Dto를 매핑하는 용도. Dto 에  Map과 같은 복잡한 구조가 있을 때는 자동 변환이 힘들어 사용.
        binder.registerCustomEditor(Map.class, "factors", new PropertyEditorSupport() {
            private final ObjectMapper objectMapper = new ObjectMapper();
            @Override
            public void setAsText(String text) {
                try {
                    Map<String, String> map = objectMapper.readValue(text, new TypeReference<Map<String, String>>() {});
                    setValue(map);
                } catch (IOException e) {
                    setValue(null);
                }
            }
        });
    }

    @PostMapping("/upload")
    public ApiResponse<?> uploadExperimentFile(
            @ModelAttribute MetaDto metaDto,
            @RequestParam("file") MultipartFile file) throws IOException {
        return experimentService.createExperimentData(file, metaDto);
    }

    @GetMapping("/meta")
    public ApiResponse<?> getExperimentMetas(@RequestParam String userName) {
        return experimentService.getExperimentMetasByUser(userName);
    }
    @GetMapping("/chunk")
    public ApiResponse<?> getExperimentChunks(@RequestParam String expId) {
        return experimentService.getExperimentChunksByExperimentId(expId);
    }
}