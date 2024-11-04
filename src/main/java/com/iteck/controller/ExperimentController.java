package com.iteck.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iteck.dto.ApiResponse;
import com.iteck.dto.MetaDto;
import com.iteck.service.ExperimentService;

import io.swagger.v3.oas.annotations.Operation;
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
    @Operation(summary = "실험 파일 업로드", description = "엑셀 파일 형식으로 된 실험 데이터 업로드。")
    public ApiResponse<?> uploadExperiment(
            @RequestPart("metaDto") MetaDto metaDto,   // JSON 데이터
            @RequestPart("file") MultipartFile file    // 파일
    ) throws IOException {
        return experimentService.createExperimentData(file, metaDto);
    }

    @DeleteMapping(value = "/delete")
    @Operation(summary = "실험 데이터 삭제", description = "삭제 파일에 포함된 데이터들을 DB에서 찾아 삭제")
    public ApiResponse<Void> deleteExperiment(@RequestParam String title) throws IOException {
        return experimentService.deleteExperimentData(title);
    }

    @GetMapping("/import/time")
    @Operation(summary = "시간-전류, 시간-전압 그래프 플롯에 필요한 데이터 반환", description = "kind(활물질, 전도체 등), fixed(고정인자 이름, CMC), yFactor(전류 or 전압)을 클라이언트에서 받아 충족하는 데이터 반환.")
    public CompletableFuture<ApiResponse<?>> getExperimentComparisonsByTime(
            @RequestParam("kind") String factorKind,
            @RequestParam("fixed") String fixedFactor,
            @RequestParam(value = "yFactor", defaultValue = "voltage") String yFactor
            ){
        return experimentService.getTimeListByFixedFactor(factorKind, fixedFactor, yFactor);
    }

    @GetMapping("/import/cycle")
    @Operation(summary = "사이클-쿨링효율, 사이클-용량 그래프 플롯에 필요한 데이터 반환", description = "kind(활물질, 전도체 등), fixed(고정인자 이름, CMC), yFactor(충전 -> 방전 or 방전 -> 충전)을 클라이언트에서 받아 충족하는 데이터 반환.")
    public CompletableFuture<ApiResponse<?>> getExperimentComparisonsByCycle(
            @RequestParam("kind") String factorKind,
            @RequestParam("fixed") String fixedFactor,
            @RequestParam(value = "yFactor", defaultValue = "dchgToChg") String yFactor
            ){
       return experimentService.getCycleListByFixedFactor(factorKind, fixedFactor, yFactor);
   }
   @GetMapping("/import/voltage")
   @Operation(summary = "전압-dQ/dV 그래프 플롯에 필요한 데이터 반환", description = "kind(활물질, 전도체 등), fixed(고정인자 이름, CMC)를 클라이언트에서 받아 충족하는 데이터 반환.")
    public CompletableFuture<ApiResponse<?>> getExperimentComparisonsByVoltage(
            @RequestParam("kind") String factorKind,
            @RequestParam("fixed") String fixedFactor) {
       return experimentService.getVoltageListByFixedFactor(factorKind, fixedFactor);
   }

   @GetMapping("/detect")
   @Operation(summary = "이상치 탐지하여 실험데이터와 같이 반환", description = "파일을 클릭하면 그 이름을 기준으로 데이터를 불러와 이상치를 탐지하고 반환.")
   public ApiResponse<?> getOutliers(@RequestParam String title) {
        return experimentService.fetchExperiementWithOutliers(title);
    }
    @GetMapping("/meta")

    @Operation(summary = "사용자가 등록한 실험의 부가정보(제목, 메모, 인자 등)를 반환.", description = "사용자명을 기준으로 실험 부가정보를 불러와 반환.")
    public ApiResponse<?> getExperimentMetas(@RequestParam String userName) {
        return experimentService.getExperimentMetasByUser(userName);
    }
}