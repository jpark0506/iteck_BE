package com.iteck.service;

import com.iteck.domain.CycleData;
import com.iteck.domain.ExperimentMeta;
import com.iteck.domain.TimeData;
import com.iteck.dto.ApiResponse;
import com.iteck.dto.MetaDto;
import com.iteck.dto.ResponseDto;
import com.iteck.repository.CycleDataRepository;
import com.iteck.repository.ExperimentMetaRepository;
import com.iteck.repository.TimeDataRepository;
import com.iteck.util.ApiStatus;
import lombok.AllArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
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
    private final RedisTemplate<String, Object> redisTemplate;


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

    @Async
    public CompletableFuture<List<TimeData>> getTimesAsync(String experimentId) {
        List<TimeData> timeDatas = timeDataRepository.findByExperimentId(experimentId);
        return CompletableFuture.completedFuture(timeDatas);
    }
    @Async
    public CompletableFuture<List<CycleData>> getCyclesAsync(String experimentId) {
        List<CycleData> cycleDatas = cycleDataRepository.findByExperimentId(experimentId);
        return CompletableFuture.completedFuture(cycleDatas);
    }

    // TODO: 인자 저장 구조에서 {"인자명" : "인자함량"} 에서 {"인자종류" : {"인자명" : "인자함량"}} 으로 변경되면서  수정해야 함,
    public ApiResponse<?> getTimeListByFixedFactor(String fixedFactor) {
        List<ExperimentMeta> experimentMetas = experimentMetaRepository.findByDynamicFactorKey(fixedFactor);
        List<String> experimentIds = experimentMetas.stream()
                .map(ExperimentMeta::getExperimentId)
                .toList();

        // CompletableFuture로 비동기 처리
        List<CompletableFuture<List<TimeData>>> futureTimeDatas = experimentIds.stream()
                .map(this::getTimesAsync)
                .toList();

        // 모든 비동기 작업이 완료될 때까지 대기
        CompletableFuture.allOf(futureTimeDatas.toArray(new CompletableFuture[0])).join();

        // 결과를 맵으로 변환
        Map<String, List<TimeData>> timeMap = new HashMap<>();
        for (int i = 0; i < experimentIds.size(); i++) {
            try {
                timeMap.put(experimentIds.get(i), futureTimeDatas.get(i).get());
            } catch (Exception ignored){

            }

        }
        List<ResponseDto.Time> timeDtoList = experimentMetas.stream()
                .map(meta -> ResponseDto.Time.builder()
                        .meta(meta)
                        .timeDatas(Collections.singletonMap("time: " + meta.getExperimentId(),
                                timeMap.getOrDefault(meta.getExperimentId(), new ArrayList<>())))
                        .build())
                .toList();

        return ApiResponse.fromResultStatus(ApiStatus.SUC_EXPERIMENT_READ, timeDtoList);
    }

    // TODO: 인자 저장 구조에서 {"인자명" : "인자함량"} 에서 {"인자종류" : {"인자명" : "인자함량"}} 으로 변경되면서  수정해야 함,
    public ApiResponse<?> getCycleListByFixedFactor(String fixedFactor) {
        List<ExperimentMeta> experimentMetas = experimentMetaRepository.findByDynamicFactorKey(fixedFactor);
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
