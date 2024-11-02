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
import java.util.concurrent.CompletableFuture;

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
    // NOTICE : 제 로컬 환경에서는 MetaDto 형식으로 받게끔 js 코드를 구현해놔서 이렇게 변경했습니다!
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<?> uploadExperiment(
            @RequestPart("metaDto") MetaDto metaDto,   // JSON 데이터
            @RequestPart("file") MultipartFile file    // 파일
    ) throws IOException {
        return experimentService.createExperimentData(file, metaDto);
    }

    /* TODO: 실험 데이터 삭제
    @PostMapping(value = "delete")
    public ApiResponse<?> uploadExperiment(@RequestParam String title) throws IOException {
        return experimentService.deleteExperimentData(title);
    }*/

    @GetMapping("/import/time")
    public CompletableFuture<ApiResponse<?>> getExperimentComparisonsByTime(
            @RequestParam("kind") String factorKind,
            @RequestParam("fixed") String fixedFactor,
            @RequestParam(value = "yFactor", defaultValue = "voltage") String yFactor
            ){
        return experimentService.getTimeListByFixedFactor(factorKind, fixedFactor, yFactor);
    }

    @GetMapping("/import/cycle")
    public CompletableFuture<ApiResponse<?>> getExperimentComparisonsByCycle(
            @RequestParam("kind") String factorKind,
            @RequestParam("fixed") String fixedFactor,
            @RequestParam(value = "yFactor", defaultValue = "dchgToChg") String yFactor
            ){
       return experimentService.getCycleListByFixedFactor(factorKind, fixedFactor, yFactor);
   }
   @GetMapping("/import/voltage")
    public CompletableFuture<ApiResponse<?>> getExperimentComparisonsByVoltage(
            @RequestParam("kind") String factorKind,
            @RequestParam("fixed") String fixedFactor) {
       return experimentService.getVoltageListByFixedFactor(factorKind, fixedFactor);
   }

   @GetMapping("/detect")
   public ApiResponse<?> getOutliers(@RequestParam String title) {
        return experimentService.fetchExperiementWithOutliers(title);
    }
    @GetMapping("/meta")
    public ApiResponse<?> getExperimentMetas(@RequestParam String userName) {
        return experimentService.getExperimentMetasByUser(userName);
    }
}