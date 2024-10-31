package com.iteck.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iteck.dto.ApiResponse;
import com.iteck.dto.MetaDto;
import com.iteck.service.ExperimentService;

import lombok.AllArgsConstructor;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<?> uploadExperiment(
        @RequestParam("metaDto") String metaDtoJson,
        @RequestPart("file") MultipartFile file
    ) throws IOException {
        
        ObjectMapper objectMapper = new ObjectMapper();
        MetaDto metaDto = objectMapper.readValue(metaDtoJson, MetaDto.class);

        return experimentService.createExperimentData(file, metaDto);
    }


    // TODO: 인자 저장 구조에서 {"인자명" : "인자함량"} 에서 {"인자종류" : {"인자명" : "인자함량"}} 으로 변경되면서  수정해야 함,
    @GetMapping("/import/time")
    public ApiResponse<?> getExperimentComparisonsByTime(
            @RequestParam("fixed") String fixedFactor){
        return experimentService.getTimeListByFixedFactor(fixedFactor);
    }


    // TODO: 인자 저장 구조에서 {"인자명" : "인자함량"} 에서 {"인자종류" : {"인자명" : "인자함량"}} 으로 변경되면서  수정해야 함,
    @GetMapping("/import/cycle")
    public ApiResponse<?> getExperimentComparisonsByCycle(
            @RequestParam("fixed") String fixedFactor){
        return experimentService.getCycleListByFixedFactor(fixedFactor);
    }

    @GetMapping("/meta")
    public ApiResponse<?> getExperimentMetas(@RequestParam String userName) {
        return experimentService.getExperimentMetasByUser(userName);
    }
}