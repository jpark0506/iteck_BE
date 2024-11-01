package com.iteck.service;

import com.iteck.domain.CycleData;
import com.iteck.domain.ExperimentMeta;
import com.iteck.domain.TimeData;
import com.iteck.dto.*;
import com.iteck.repository.CycleDataRepository;
import com.iteck.repository.ExperimentMetaCustomRepository;
import com.iteck.repository.ExperimentMetaRepository;
import com.iteck.repository.TimeDataRepository;
import com.iteck.util.ApiStatus;
import lombok.AllArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static com.iteck.util.FileUtil.*;


@Service
@AllArgsConstructor
public class ExperimentService {
    private final ExperimentMetaRepository experimentMetaRepository;
    private final TimeDataRepository timeDataRepository;
    private final CycleDataRepository cycleDataRepository;
    private final ExperimentMetaCustomRepository experimentMetaCustomRepository;


    public ApiResponse<?>  createExperimentData(MultipartFile file, MetaDto metaDto) throws IOException {
        String experimentId = UUID.randomUUID().toString();

        // CSV 파일 여부 확인
        if (isExcel(file)) {
            return ApiResponse.fromResultStatus(ApiStatus.BAD_REQUEST);
        }

        // ExperimentMeta 생성 후 저장
        ExperimentMeta experimentMeta = ExperimentMeta.builder()
                .experimentId(experimentId)
                .userName(metaDto.getUserName())
                .title(metaDto.getTitle())
                .memo(metaDto.getMemo())
                .factors(metaDto.getFactors())
                .expDate(ZonedDateTime.now(ZoneId.of("Asia/Seoul")).toInstant())
                .build();
        experimentMetaRepository.save(experimentMeta);

        // 데이터 처리를 위한 BufferedReader 생성
        BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()));
        String line;
        String[] headers = {};
        boolean isHeader = true;
        int chunkSize = 1000;
        int timeDataChunkId = 1;
        int cycleDataChunkId = 1;

        // 실험 데이터를 저장할 임시 리스트
        List<Map<String, Object>> timeExpSpecBuffer = new ArrayList<>(chunkSize);
        List<Map<String, Object>> cycleExpSpecBuffer = new ArrayList<>(chunkSize);

        // Cycle Index 열의 인덱스를 저장할 리스트
        List<Integer> cycleIndexPositions = new ArrayList<>();

        // 데이터를 읽고 저장
        // 데이터를 읽고 저장
        while ((line = reader.readLine()) != null) {
            String[] data = line.split(",");

            // 첫 번째 행이 헤더인지 확인
            if (isHeader) {
                headers = data;
                isHeader = false;
                cycleIndexPositions = findCycleIndexPositions(headers);
                continue;
            }

            // 첫 번째 그룹 (TimeData의 expSpec에 추가할 데이터)
            int firstCycleIdx = cycleIndexPositions.get(0);
            if (firstCycleIdx < data.length) {
                Map<String, Object> timeRowData = createRowMap(headers, data, firstCycleIdx + 1, cycleIndexPositions.get(1));
                timeRowData.put("cycleIndex", parseValue(data[firstCycleIdx]));
                timeExpSpecBuffer.add(timeRowData);

                if (timeExpSpecBuffer.size() == chunkSize) {
                    saveTimeDataChunkAsync(experimentId, timeDataChunkId++, timeExpSpecBuffer);
                    timeExpSpecBuffer.clear();
                }
            }

            // 두 번째 그룹 (CycleData의 expSpec에 추가할 데이터)
            if (cycleIndexPositions.size() > 1) {
                int secondCycleIdx = cycleIndexPositions.get(1);
                if (secondCycleIdx < data.length) {
                    Map<String, Object> cycleRowData = createRowMap(headers, data, secondCycleIdx, data.length);
                    cycleExpSpecBuffer.add(cycleRowData);

                    if (cycleExpSpecBuffer.size() == chunkSize) {
                        saveCycleDataChunkAsync(experimentId, cycleDataChunkId++, cycleExpSpecBuffer);
                        cycleExpSpecBuffer.clear();
                    }
                }
            }
        }

        if (!timeExpSpecBuffer.isEmpty()) {
            saveTimeDataChunkAsync(experimentId, timeDataChunkId, timeExpSpecBuffer);
        }
        if (!cycleExpSpecBuffer.isEmpty()) {
            saveCycleDataChunkAsync(experimentId, cycleDataChunkId, cycleExpSpecBuffer);
        }


        return ApiResponse.fromResultStatus(ApiStatus.SUC_EXPERIMENT_CREATE);
    }


    @Async
    private void saveTimeDataChunkAsync(String experimentId, int chunkId, List<Map<String, Object>> expSpec) {
        TimeData timeData = TimeData.builder()
                .experimentId(experimentId)
                .chunkId(chunkId)
                .expSpec(new ArrayList<>(expSpec)) // 버퍼 내용을 복사하여 설정
                .build();
        timeDataRepository.save(timeData);
    }

    @Async
    private void saveCycleDataChunkAsync(String experimentId, int chunkId, List<Map<String, Object>> expSpec) {
        // expSpec 내용 출력
        System.out.println("CycleData expSpec 내용: " + expSpec);

        CycleData cycleData = CycleData.builder()
                .experimentId(experimentId)
                .chunkId(chunkId)
                .expSpec(new ArrayList<>(expSpec)) // 버퍼 내용을 복사하여 설정
                .build();

        cycleDataRepository.save(cycleData);
    }




    public ApiResponse<?> getExperimentMetasByUser(String userName) {
        List<ExperimentMeta> experimentMetas = experimentMetaRepository.findByUserName(userName);
        return ApiResponse.fromResultStatus(ApiStatus.SUC_EXPERIMENT_READ, experimentMetas);
    }






    @Async
    public CompletableFuture<List<CycleData>> getCyclesAsync(String experimentId) {
        List<CycleData> cycleDatas = cycleDataRepository.findByExperimentId(experimentId);
        return CompletableFuture.completedFuture(cycleDatas);
    }

    // 1. getTimesAsync 메서드: yFactor에 따라 명확한 타입 반환
    @Async
    private CompletableFuture<List<?>> getTimesAsync(String experimentId, String yFactor) {
        return CompletableFuture.supplyAsync(() -> {
            List<TimeData> timeDataList = timeDataRepository.findByExperimentId(experimentId);

            return timeDataList.stream()
                    .flatMap(timeData -> timeData.getExpSpec().stream())
                    .map(expSpec -> {
                        String totalTime = expSpec.get("Total Time") != null ? expSpec.get("Total Time").toString() : null;
                        String value = yFactor.equals("current") && expSpec.get("Current(mA)") != null
                                ? expSpec.get("Current(mA)").toString()
                                : yFactor.equals("voltage") && expSpec.get("Voltage(V)") != null
                                ? expSpec.get("Voltage(V)").toString()
                                : null;

                        // yFactor에 따라 명확한 타입 반환
                        return TimeDto.createTimeDto(yFactor, totalTime, value);
                    })
                    .filter(dto -> dto != null)
                    .toList();
        });
    }

    @Async
    private CompletableFuture<List<?>> getCyclesAsync(String experimentId, String yFactor){
        return CompletableFuture.supplyAsync(() -> {
                    List<CycleData> cycleDataList = cycleDataRepository.findByExperimentId(experimentId);
                    return cycleDataList.stream()
                            .flatMap(cycleData -> cycleData.getExpSpec().stream())
                            .map(expSpec -> {
                                String cycleIndex = expSpec.get("Cycle Index") != null ? expSpec.get("Cycle Index").toString() : null;
                                String whichCap = yFactor.equals("dchgToChg") && expSpec.get("DChg_ Spec_ Cap_(mAh/g)") != null
                                        ? expSpec.get("DChg_ Spec_ Cap_(mAh/g)").toString()
                                        : yFactor.equals("chgToDchg") && expSpec.get("Chg_ Spec_ Cap_(mAh/g)") != null
                                        ? expSpec.get("Chg_ Spec_ Cap_(mAh/g)").toString()
                                        : null;
                                String whichRatio = yFactor.equals("dchgToChg") && expSpec.get("DChg/Chg Ratio (%)") != null
                                        ? expSpec.get("DChg/Chg Ratio (%)").toString()
                                        : yFactor.equals("chgToDchg") && expSpec.get("Chg/DChg Ratio (%)") != null
                                        ? expSpec.get("Chg/DChg Ratio (%)").toString()
                                        : null;
                                return CycleDto.createCycleDto(yFactor, cycleIndex, whichCap, whichRatio);
                            })
                            .filter(dto -> dto != null)
                            .toList();
                });
    }



    // 2. getTimeListByFixedFactor 메서드
    @Async
    public CompletableFuture<ApiResponse<?>> getTimeListByFixedFactor(String factorKind, String fixedFactor, String yFactor) {
        ApiResponse<?> response;
        List<ExperimentMeta> experimentMetas = experimentMetaCustomRepository.findByFactorKeyExists(factorKind, fixedFactor);
        List<String> experimentIds = experimentMetas.stream()
                .map(ExperimentMeta::getExperimentId)
                .toList();

        // CompletableFuture로 비동기 처리하여 각 experimentId의 TimeData를 가져옴
        List<CompletableFuture<List<?>>> futureTimeDtos = experimentIds.stream()
                .map(id -> getTimesAsync(id, yFactor))
                .toList();

        CompletableFuture.allOf(futureTimeDtos.toArray(new CompletableFuture[0])).join();

        // 결과를 맵으로 변환
        Map<String, List<?>> timeDtoMap = new HashMap<>();
        for (int i = 0; i < experimentIds.size(); i++) {
            try {
                timeDtoMap.put(experimentIds.get(i), futureTimeDtos.get(i).get());
            } catch (Exception ignored) {
                // 예외 처리
            }
        }


        if (yFactor.equals("current")) {
            List<ResponseDto.TimeWithCurrent> timeWithCurrentList = experimentMetas.stream()
                    .map(meta -> new ResponseDto.TimeWithCurrent(
                            meta,
                            Collections.singletonMap("time: " + meta.getExperimentId(),
                                    (List<TimeDto.withCurrent>) (List<?>) timeDtoMap.getOrDefault(meta.getExperimentId(), new ArrayList<>()))
                    ))
                    .toList();
            response = ApiResponse.fromResultStatus(ApiStatus.SUC_EXPERIMENT_READ, timeWithCurrentList);
        } else if (yFactor.equals("voltage")) {
            List<ResponseDto.TimeWithVoltage> timeWithVoltageList = experimentMetas.stream()
                    .map(meta -> new ResponseDto.TimeWithVoltage(
                            meta,
                            Collections.singletonMap("time: " + meta.getExperimentId(),
                                    (List<TimeDto.withVoltage>) (List<?>) timeDtoMap.getOrDefault(meta.getExperimentId(), new ArrayList<>()))
                    ))
                    .toList();
            response = ApiResponse.fromResultStatus(ApiStatus.SUC_EXPERIMENT_READ, timeWithVoltageList);
        } else {
            throw new IllegalArgumentException("Unsupported yFactor type");
        }

        return CompletableFuture.completedFuture(response);
    }

    @Async
    public CompletableFuture<ApiResponse<?>> getCycleListByFixedFactor(String factorKind, String fixedFactor, String yFactor){
        ApiResponse<?> response;
        List<ExperimentMeta> experimentMetas = experimentMetaCustomRepository.findByFactorKeyExists(factorKind, fixedFactor);
        List<String> experimentIds = experimentMetas.stream()
                .map(ExperimentMeta::getExperimentId)
                .toList();

        List<CompletableFuture<List<?>>> futureCycleDtos = experimentIds.stream()
                .map(id -> getCyclesAsync(id, yFactor))
                .toList();

        CompletableFuture.allOf(futureCycleDtos.toArray(new CompletableFuture[0])).join();
        Map<String, List<?>> cycleDtoMap = new HashMap<>();
        for (int i = 0; i < experimentIds.size(); i++) {
            try {
                cycleDtoMap.put(experimentIds.get(i), futureCycleDtos.get(i).get());
            } catch (Exception ignored) {
                // 예외 처리
            }
        }
        if (yFactor.equals("dchgToChg")) {
            List<ResponseDto.CycleDchgToChg> cycleDchgToChgList = experimentMetas.stream()
                    .map(meta -> new ResponseDto.CycleDchgToChg(
                            meta,
                            Collections.singletonMap("time: " + meta.getExperimentId(),
                                    (List<CycleDto.dchgToChg>) (List<?>) cycleDtoMap.getOrDefault(meta.getExperimentId(), new ArrayList<>()))
                    ))
                    .toList();
            response = ApiResponse.fromResultStatus(ApiStatus.SUC_EXPERIMENT_READ, cycleDchgToChgList);
        } else if (yFactor.equals("chgToDchg")) {
            List<ResponseDto.CycleChgToDchg> cycleChgToDchgList = experimentMetas.stream()
                    .map(meta -> new ResponseDto.CycleChgToDchg(
                            meta,
                            Collections.singletonMap("time: " + meta.getExperimentId(),
                                    (List<CycleDto.chgToDchg>) (List<?>) cycleDtoMap.getOrDefault(meta.getExperimentId(), new ArrayList<>()))
                    ))
                    .toList();
            response = ApiResponse.fromResultStatus(ApiStatus.SUC_EXPERIMENT_READ, cycleChgToDchgList);
        } else {
            throw new IllegalArgumentException("Unsupported yFactor type");
        }
        return CompletableFuture.completedFuture(response);
    }

    // TODO
    public ApiResponse<?> fetchExperiementWithOutliers(String title){
        ExperimentMeta experimentMeta = experimentMetaRepository.findByTitle(title);
        String experimentId = experimentMeta.getExperimentId();
        return ApiResponse.fromResultStatus(ApiStatus.SUC_FACTOR_CREATE);
    }
}
