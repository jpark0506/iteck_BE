package com.iteck.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iteck.dto.ApiResponse;
import com.iteck.dto.FactorsDto;
import com.iteck.dto.MetaDto;
import com.iteck.service.ExperimentService;

import com.iteck.util.ApiStatus;
import io.swagger.v3.oas.annotations.Operation;
import lombok.AllArgsConstructor;

import org.springframework.http.MediaType;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.beans.PropertyEditorSupport;
import java.io.*;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.iteck.util.FileUtil.parseParameterList;

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
    @Operation(summary = "여러 실험 파일 업로드", description = "엑셀 파일 형식으로 된 여러 실험 데이터를 업로드.")
    public ApiResponse<?> uploadMultipleExperiments(
            @RequestPart("factorDto") List<FactorsDto> factorsDtoList, // JSON 데이터 리스트
            @RequestPart("file") List<MultipartFile> files             // 파일 리스트
    ) throws IOException {
        if (factorsDtoList.size() != files.size()) {
            return ApiResponse.fromResultStatus(ApiStatus.BAD_REQUEST);
        }

        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            FactorsDto factorsDto = factorsDtoList.get(i);

            // 각 파일과 매핑된 factorDto에 대해 실험 데이터 생성
            experimentService.createExperimentData(file, factorsDto);
        }

        return ApiResponse.fromResultStatus(ApiStatus.SUC_EXPERIMENT_CREATE);
    }
    @PostMapping("/meta/post")
    @Operation(summary = "사용자가 등록한 실험의 부가정보(제목, 메모, 날짜)를 저장.")
    public ApiResponse<?> postExperimentMetas(@RequestBody MetaDto metaDto) {
        return experimentService.createMeta(metaDto);
    }
    @GetMapping("/meta/get")
    @Operation(summary = "사용자가 등록한 실험의 부가정보(제목, 메모, 날짜) 리스트를 반환.", description = "사용자명을 기준으로 실험 부가정보를 불러와 반환.")
    public ApiResponse<?> getExperimentMetas(@RequestParam String userName) {
        return experimentService.getExperimentMetasByUser(userName);
    }

    @GetMapping("/meta/get/{metaId}")
    @Operation(summary = "사용자가 등록한 실험의 부가정보(제목, 메모, 날짜)를 반환.", description = "ID를 기준으로 실험 부가정보를 불러와 반환.")
    public ApiResponse<?> getExperimentMetasWithId(
        @PathVariable String metaId) {
        return experimentService.getExperimentMetaWithId(metaId);
    }

    @GetMapping("/upload/read")
    @Operation(summary = "업로드한 파일 조회", description = "사용자명을 기준으로 업로드한 파일명과 등록할 때 입력한 인자값을 불러와 반환. 단, 현재는 실험의 고유 id인 experiment_id를 불러오도록 구현.")
    public ApiResponse<?> getUploadedFileDatas(@RequestParam String userName) {
        return experimentService.fetchUploadedFileDatas(userName);
    }


    @DeleteMapping(value = "/delete")
    @Operation(summary = "실험 데이터 삭제", description = "실험 id값을 기준으로 데이터들을 DB에서 찾아 삭제")
    public ApiResponse<Void> deleteExperiment(@RequestParam String experimentId) throws IOException {
        return experimentService.deleteExperimentData(experimentId);
    }

    @GetMapping("/import/time")
    @Operation(summary = "시간-전류, 시간-전압 그래프 플롯에 필요한 데이터 반환", description =
            "kind(활물질, 전도체 등), fixed(고정인자 이름, CMC), yFactor(전류 or 전압)을 클라이언트에서 받아 충족하는 데이터 반환."
            + "예시: http://34.64.87.212:8080/exp/import/time?yFactor=current&factorKind=활물질:TEST&factorAmount=활물질:134&variable=factorKind:바인더:desc")
    public CompletableFuture<ApiResponse<?>> getExperimentComparisonsByTime(
            @RequestParam(value = "yFactor") String yFactor,
            @RequestParam(value = "factorKind", required = false) List<String> factorKind,
            @RequestParam(value = "factorAmount", required = false) List<String> factorAmount,
            @RequestParam(value = "variable", required = false) String variable
            ){
        // null 체크 후 빈 리스트로 초기화
        factorKind = (factorKind == null) ? Collections.emptyList() : factorKind;
        factorAmount = (factorAmount == null) ? Collections.emptyList() : factorAmount;

        List<Map<String, String>> parsedKinds = parseParameterList(factorKind);
        List<Map<String, String>> parsedAmounts = parseParameterList(factorAmount);
        return experimentService.getTimeListByFixedFactor(parsedKinds, parsedAmounts, yFactor, variable);
    }

    @GetMapping("/import/cycle")
    @Operation(summary = "사이클-쿨링효율, 사이클-용량 그래프 플롯에 필요한 데이터 반환", description =
            "여러 종류의 물질(kind)과 그에 대응하는 함량(amount)을 받아 데이터를 반환."
        +  "예시: http://34.64.87.212:8080/exp/import/cycle?yFactor=chgToDchg&factorKind=활물질:TEST&factorAmount=활물질:134&factorKind=바인더:BIND&variable=factorKind:바인더:desc")
    public CompletableFuture<ApiResponse<?>> getExperimentComparisonsByCycle(
            @RequestParam(value = "yFactor") String yFactor,
            @RequestParam(value = "factorKind", required = false) List<String> factorKind,
            @RequestParam(value = "factorAmount", required = false) List<String> factorAmount,
            @RequestParam(value = "variable", required = false) String variable
    ) {
        // null 체크 후 빈 리스트로 초기화
        factorKind = (factorKind == null) ? Collections.emptyList() : factorKind;
        factorAmount = (factorAmount == null) ? Collections.emptyList() : factorAmount;

        List<Map<String, String>> parsedKinds = parseParameterList(factorKind);
        List<Map<String, String>> parsedAmounts = parseParameterList(factorAmount);

        return experimentService.getCycleListByFixedFactor(parsedKinds, parsedAmounts, yFactor, variable);
    }
    @GetMapping("/import/voltage")
    @Operation(summary = "전압-dQ/dV 그래프 플롯에 필요한 데이터 반환",
            description = "고정인자를 1개 이상 입력 받고 그 값들을 갖는 데이터를 불러와  반환." +
                    "예시: http://34.64.87.212:8080/exp/import/cycle?yFactor=dchgToChg&factorKind=활물질:TEST&variable=factorKind:바인더:desc")
    public CompletableFuture<ApiResponse<?>> getExperimentComparisonsByVoltage(
            @RequestParam(value = "factorKind", required = false) List<String> factorKind,
            @RequestParam(value = "factorAmount", required = false) List<String> factorAmount,
            @RequestParam(value = "variable", required = false) String variable) {
        factorKind = (factorKind == null) ? Collections.emptyList() : factorKind;
        factorAmount = (factorAmount == null) ? Collections.emptyList() : factorAmount;

        List<Map<String, String>> parsedKinds = parseParameterList(factorKind);
        List<Map<String, String>> parsedAmounts = parseParameterList(factorAmount);
       return experimentService.getVoltageListByFixedFactor(parsedKinds, parsedAmounts, variable);
   }


   @GetMapping("/detect")
   @Operation(summary = "이상치 탐지하여 실험데이터와 같이 반환", description = "고정인자를 1개 이상 입력 받고 그 값들을 갖는 데이터를 불러와 이상치를 탐지하고 반환." +
           "예시: http://34.64.87.212:8080//exp/detect?factorKind=활물질:TEST&factorAmount=활물질:134&variable=factorKind:바인더:desc")
   public CompletableFuture<ApiResponse<?>> getOutliers(
           @RequestParam(value = "factorKind", required = false) List<String> factorKind,
           @RequestParam(value = "factorAmount", required = false) List<String> factorAmount,
           @RequestParam(value = "variable", required = false) String variable) {

       factorKind = (factorKind == null) ? Collections.emptyList() : factorKind;
       factorAmount = (factorAmount == null) ? Collections.emptyList() : factorAmount;

       List<Map<String, String>> parsedKinds = parseParameterList(factorKind);
       List<Map<String, String>> parsedAmounts = parseParameterList(factorAmount);
       return experimentService.fetchExperiementWithOutliers(parsedKinds, parsedAmounts, variable);
    }
}