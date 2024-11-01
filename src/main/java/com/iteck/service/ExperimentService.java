package com.iteck.service;

import com.iteck.domain.CycleData;
import com.iteck.domain.ExperimentMeta;
import com.iteck.domain.TimeData;
import com.iteck.dto.ApiResponse;
import com.iteck.dto.MetaDto;
import com.iteck.dto.ResponseDto;
import com.iteck.dto.TimeDto;
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


    public ApiResponse<?> createExperimentData(MultipartFile file, MetaDto metaDto) throws IOException {
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
        while ((line = reader.readLine()) != null) {
            String[] data = line.split(",");
            if (isHeader) {
                headers = data;
                isHeader = false;
                cycleIndexPositions = findCycleIndexPositions(headers);
                continue;
            }

            // 첫 번째 그룹 (TimeData의 expSpec에 추가할 데이터)
            int firstCycleIdx = cycleIndexPositions.get(0);
            Map<String, Object> timeRowData = createRowMap(headers, data, firstCycleIdx + 1, cycleIndexPositions.get(1));
            timeRowData.put("cycleIndex", parseValue(data[firstCycleIdx])); // cycleIndex 추가
            timeExpSpecBuffer.add(timeRowData);

            // 두 번째 그룹 (CycleData의 expSpec에 추가할 데이터)
            int secondCycleIdx = cycleIndexPositions.get(1);
            Map<String, Object> cycleRowData = createRowMap(headers, data, secondCycleIdx + 1, data.length);
            cycleExpSpecBuffer.add(cycleRowData);

            // Chunk 크기에 도달할 때마다 TimeData와 CycleData 저장
            if (timeExpSpecBuffer.size() == chunkSize) {
                saveTimeDataChunkAsync(experimentId, timeDataChunkId++, timeExpSpecBuffer);
                timeExpSpecBuffer.clear(); // 버퍼 초기화
            }
            if (cycleExpSpecBuffer.size() == chunkSize) {
                saveCycleDataChunkAsync(experimentId, cycleDataChunkId++, cycleExpSpecBuffer);
                cycleExpSpecBuffer.clear(); // 버퍼 초기화
            }
        }

        // 남은 데이터 저장
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

    private CompletableFuture<List<TimeDto.withCurrent>> getTimesAsync(String experimentId, String yFactor) {
        return CompletableFuture.supplyAsync(() -> {
            List<TimeData> timeDataList = timeDataRepository.findByExperimentId(experimentId);

            // 필요한 필드만 추출하여 TimeDto.withCurrent로 변환
            return timeDataList.stream()
                    .flatMap(timeData -> timeData.getExpSpec().stream())
                    .map(expSpec -> {
                        // Total Time을 String으로 변환 (존재하지 않을 경우 null)
                        String totalTime = expSpec.get("Total Time") != null ? expSpec.get("Total Time").toString() : null;

                        // Current(mA)를 Double로 받고 필요 시 String으로 변환
                        String current = yFactor.equals("current") && expSpec.get("Current(mA)") != null
                                ? expSpec.get("Current(mA)").toString()
                                : null;

                        return TimeDto.withCurrent.builder()
                                .totalTime(totalTime)
                                .current(current)
                                .build();
                    })
                    .filter(dto -> dto.getCurrent() != null) // 필요한 필드가 있는 항목만 필터링
                    .toList();
        });
    }


    @Async
    public CompletableFuture<List<CycleData>> getCyclesAsync(String experimentId) {
        List<CycleData> cycleDatas = cycleDataRepository.findByExperimentId(experimentId);
        return CompletableFuture.completedFuture(cycleDatas);
    }

    // TODO: 인자 저장 구조에서 {"인자명" : "인자함량"} 에서 {"인자종류" : {"인자명" : "인자함량"}} 으로 변경되면서  수정해야 함,
    public ApiResponse<?> getTimeListByFixedFactor(String fixedFactor, String yFactor) {
        // ExperimentMeta 조회
        List<ExperimentMeta> experimentMetas = experimentMetaCustomRepository.findByFactorKeyExists(fixedFactor);
        List<String> experimentIds = experimentMetas.stream()
                .map(ExperimentMeta::getExperimentId)
                .toList();

        // CompletableFuture로 비동기 처리하여 각 experimentId의 TimeData를 가져옴
        List<CompletableFuture<List<TimeDto.withCurrent>>> futureTimeDtos = experimentIds.stream()
                .map(id -> getTimesAsync(id, yFactor))
                .toList();

        // 모든 비동기 작업이 완료될 때까지 대기
        CompletableFuture.allOf(futureTimeDtos.toArray(new CompletableFuture[0])).join();

        // 결과를 맵으로 변환
        Map<String, List<TimeDto.withCurrent>> timeDtoMap = new HashMap<>();
        for (int i = 0; i < experimentIds.size(); i++) {
            try {
                timeDtoMap.put(experimentIds.get(i), futureTimeDtos.get(i).get());
            } catch (Exception ignored) {
                // 예외 처리
            }
        }

        // ResponseDto.TimeWithCurrent 생성
        List<ResponseDto.TimeWithCurrent> timeWithCurrentList = experimentMetas.stream()
                .map(meta -> new ResponseDto.TimeWithCurrent(
                        meta,
                        Collections.singletonMap("time: " + meta.getExperimentId(),
                                timeDtoMap.getOrDefault(meta.getExperimentId(), new ArrayList<>()))
                ))
                .toList();

        return ApiResponse.fromResultStatus(ApiStatus.SUC_EXPERIMENT_READ, timeWithCurrentList);
    }


    // TODO: 인자 저장 구조에서 {"인자명" : "인자함량"} 에서 {"인자종류" : {"인자명" : "인자함량"}} 으로 변경되면서  수정해야 함,
    public ApiResponse<?> getCycleListByFixedFactor(String fixedFactor) {
        List<ExperimentMeta> experimentMetas = experimentMetaCustomRepository.findByFactorKeyExists("ABC");
        List<String> experimentIds = experimentMetas.stream()
                .map(ExperimentMeta::getExperimentId)
                .toList();

        // CompletableFuture로 비동기 처리
        List<CompletableFuture<List<CycleData>>> futureCycleDatas = experimentIds.stream()
                .map(this::getCyclesAsync)
                .toList();

        // 모든 비동기 작업이 완료될 때까지 대기
        CompletableFuture.allOf(futureCycleDatas.toArray(new CompletableFuture[0])).join();

        // 결과를 맵으로 변환
        Map<String, List<CycleData>> cycleMap = new HashMap<>();
        for (int i = 0; i < experimentIds.size(); i++) {
            try {
                cycleMap.put(experimentIds.get(i), futureCycleDatas.get(i).get());
            } catch (Exception ignored){

            }

        }
        List<ResponseDto.Cycle> cycleDtoList = experimentMetas.stream()
                .map(meta -> ResponseDto.Cycle.builder()
                        .meta(meta)
                        .cycleDatas(Collections.singletonMap("cycle: " + meta.getExperimentId(),
                                cycleMap.getOrDefault(meta.getExperimentId(), new ArrayList<>())))
                        .build())
                .toList();

        return ApiResponse.fromResultStatus(ApiStatus.SUC_EXPERIMENT_READ, cycleDtoList);
    }








}
