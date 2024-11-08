package com.iteck.service;

import com.iteck.domain.CycleData;
import com.iteck.domain.Factor;
import com.iteck.domain.Meta;
import com.iteck.domain.TimeData;
import com.iteck.dto.*;
import com.iteck.repository.*;
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
import java.util.stream.Collectors;

import static com.iteck.util.FileUtil.*;


@Service
@AllArgsConstructor
public class ExperimentService {
    private final FactorRepository factorRepository;
    private final TimeDataRepository timeDataRepository;
    private final CycleDataRepository cycleDataRepository;
    private final FactorCustomRepository factorCustomRepository;
    private final MetaRepository metaRepository;


    public ApiResponse<?> createExperimentData(MultipartFile file, FactorsDto factorsDto) throws IOException {
        String experimentId = UUID.randomUUID().toString();

        // CSV 파일 여부 확인
        if (isExcel(file)) {
            return ApiResponse.fromResultStatus(ApiStatus.BAD_REQUEST);
        }

        // ExperimentMeta 생성 후 저장
        Factor factor = Factor.builder()
                .experimentId(experimentId)
                .userName(factorsDto.getUserName())
                .factors(factorsDto.getFactors())
                .build();
        factorRepository.save(factor);

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
        List<MetaDto> metaDtos = metaRepository.findAllByUserName(userName)
                .stream()
                .map(MetaDto::new)
                .toList();
        return ApiResponse.fromResultStatus(ApiStatus.SUC_EXPERIMENT_READ, metaDtos);
    }

    public ApiResponse<?> getExperimentMetaWithId(String metaId) {

        Optional<Meta> meta =  metaRepository.findById(metaId);

        if(meta.isEmpty()){
            return ApiResponse.fromResultStatus(ApiStatus.BAD_REQUEST);
        }

        return ApiResponse.fromResultStatus(ApiStatus.SUC_EXPERIMENT_READ,
            meta
        );
    }

    public ApiResponse<?> createMeta(MetaDto metaDto) {
        Meta meta = Meta.builder()
                .title(metaDto.getTitle())
                .memo(metaDto.getMemo())
                .userName(metaDto.getUserName())
                .expDate(metaDto.getExpDate())
                .regDate(ZonedDateTime.now(ZoneId.of("Asia/Seoul")).toInstant())
                .build();
        metaRepository.save(meta);
        return ApiResponse.fromResultStatus(ApiStatus.SUC_EXPERIMENT_READ, meta);
    }

    public ApiResponse<?> fetchUploadedFileDatas(String userName){
        return ApiResponse.fromResultStatus(ApiStatus.SUC_FACTOR_READ, factorRepository.findByUserName(userName));
    }
    @Async
    public CompletableFuture<List<CycleData>> getCyclesAsync(String experimentId) {
        List<CycleData> cycleDatas = cycleDataRepository.findAllByExperimentId(experimentId);
        return CompletableFuture.completedFuture(cycleDatas);
    }

    // 1. getTimesAsync 메서드: yFactor에 따라 명확한 타입 반환
    @Async
    private CompletableFuture<List<?>> getTimesAsync(String experimentId, String yFactor) {
        System.out.println(experimentId);
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
    private CompletableFuture<List<?>> getVoltagesAsync(String experimentId) {
        return CompletableFuture.supplyAsync(() -> {
            List<TimeData> timeDataList = timeDataRepository.findByExperimentId(experimentId);

            return timeDataList.stream()
                    .flatMap(timeData -> timeData.getExpSpec().stream())
                    .map(expSpec -> {
                        String voltage = expSpec.get("Voltage(V)") != null ? expSpec.get("Voltage(V)").toString() : null;
                        String dQmdV = expSpec.get("dQm/dV(mAh/V_g)") != null ? expSpec.get("Current(mA)").toString() : null;
                        // yFactor에 따라 명확한 타입 반환
                        return VoltageDto.createVoltageDto(voltage, dQmdV);
                    })
                    .filter(dto -> dto != null)
                    .toList();
        });
    }

    @Async
    private CompletableFuture<List<?>> getCyclesAsync(String experimentId, String yFactor){
        return CompletableFuture.supplyAsync(() -> {
                    List<CycleData> cycleDataList = cycleDataRepository.findAllByExperimentId(experimentId);
                    System.out.println("getCyclesAsync: " + experimentId);
                    return cycleDataList.stream()
                            .flatMap(cycleData -> cycleData.getExpSpec().stream())
                            .map(expSpec -> {
                                System.out.println("ExpSpec Map: " + expSpec);
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


    @Async
    private CompletableFuture<List<?>> getOutliersAsync(String experimentId) {
        return CompletableFuture.supplyAsync(() -> {
            CycleData cycleData = cycleDataRepository.findFirstByExperimentId(experimentId);

            // CycleData 혹은 expSpec이 null인 경우 빈 리스트 반환
            if (cycleData == null || cycleData.getExpSpec() == null) {
                return List.of();
            }

            // expSpec의 첫 번째 항목을 기준값으로 설정
            List<CycleDto.withOutlier> cycleDtosWithOutliers = cycleData.getExpSpec().stream()
                    .map(expSpec -> {
                        String cycleIndex = expSpec.get("Cycle Index") != null ? expSpec.get("Cycle Index").toString() : null;
                        String chgCap = expSpec.get("Chg_ Spec_ Cap_(mAh/g)") != null
                                ? expSpec.get("Chg_ Spec_ Cap_(mAh/g)").toString()
                                : null;
                        String dchgCap = expSpec.get("DChg_ Spec_ Cap_(mAh/g)") != null
                                ? expSpec.get("DChg_ Spec_ Cap_(mAh/g)").toString()
                                : null;
                        return new CycleDto.withOutlier(cycleIndex, chgCap, dchgCap, false, false);
                    })
                    .filter(dto -> dto.getChgCap() != null && dto.getDchgCap() != null) // null 값 필터링
                    .toList();

            // 기준값 설정
            if (!cycleDtosWithOutliers.isEmpty()) {
                CycleDto.withOutlier firstDto = cycleDtosWithOutliers.get(0);
                double chgThreshold = Double.parseDouble(firstDto.getChgCap()) * 0.7;
                double dchgThreshold = Double.parseDouble(firstDto.getDchgCap()) * 0.7;

                // 각 항목의 chgCap과 dchgCap을 기준값과 비교하여 이상 여부 설정
                return cycleDtosWithOutliers.stream()
                        .map(dto -> {
                            boolean chgOutlying = Double.parseDouble(dto.getChgCap()) <= chgThreshold;
                            boolean dchgOutlying = Double.parseDouble(dto.getDchgCap()) <= dchgThreshold;
                            return CycleDto.createCycleDtoWithOutlier(
                                    dto.getCycleIndex(), dto.getChgCap(), dto.getDchgCap(), chgOutlying, dchgOutlying
                            );
                        })
                        .toList();
            } else {
                return List.of();
            }
        });
    }



    // 2. getTimeListByFixedFactor 메서드
    @Async
    public CompletableFuture<ApiResponse<?>> getTimeListByFixedFactor(String userName, List<Map<String, String>> factorKind, List<Map<String, String>> fixedFactor, String yFactor, String variable) {
        ApiResponse<?> response;
        List<Factor> factors = factorCustomRepository.findByMultipleKindsAndCriteria(userName, factorKind, fixedFactor, variable);
        List<String> experimentIds = factors.stream()
                .map(Factor::getExperimentId)
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
            List<ResponseDto.TimeWithCurrent> timeWithCurrentList = factors.stream()
                    .map(meta -> new ResponseDto.TimeWithCurrent(
                            meta,
                            Collections.singletonMap("time: " + meta.getExperimentId(),
                                    (List<TimeDto.withCurrent>) (List<?>) timeDtoMap.getOrDefault(meta.getExperimentId(), new ArrayList<>()))
                    ))
                    .toList();
            response = ApiResponse.fromResultStatus(ApiStatus.SUC_EXPERIMENT_READ, timeWithCurrentList);
        } else if (yFactor.equals("voltage")) {
            List<ResponseDto.TimeWithVoltage> timeWithVoltageList = factors.stream()
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
    public CompletableFuture<ApiResponse<?>> getVoltageListByFixedFactor(String userName, List<Map<String, String>> factorKind, List<Map<String, String>> fixedFactor, String variable) {
        ApiResponse<?> response;
        List<Factor> factors = factorCustomRepository.findByMultipleKindsAndCriteria(userName, factorKind, fixedFactor, variable);
        List<String> experimentIds = factors.stream()
                .map(Factor::getExperimentId)
                .toList();

        // CompletableFuture로 비동기 처리하여 각 experimentId의 TimeData를 가져옴
        List<CompletableFuture<List<?>>> futureVoltageDtos = experimentIds.stream()
                .map(id -> getVoltagesAsync(id))
                .toList();

        CompletableFuture.allOf(futureVoltageDtos.toArray(new CompletableFuture[0])).join();

        // 결과를 맵으로 변환
        Map<String, List<?>> voltageDtoMap = new HashMap<>();
        for (int i = 0; i < experimentIds.size(); i++) {
            try {
                voltageDtoMap.put(experimentIds.get(i), futureVoltageDtos.get(i).get());
            } catch (Exception ignored) {
                // 예외 처리
            }
        }
        List<ResponseDto.VoltageWithDQmDv> voltageWithDQmDvList = factors.stream()
                .map(meta -> new ResponseDto.VoltageWithDQmDv(
                        meta,
                        Collections.singletonMap("experiment: " + meta.getExperimentId(),
                                (List<VoltageDto.withDQDV>) (List<?>) voltageDtoMap.getOrDefault(meta.getExperimentId(), new ArrayList<>()))
                ))
                .toList();
        response = ApiResponse.fromResultStatus(ApiStatus.SUC_EXPERIMENT_READ, voltageWithDQmDvList);

        return CompletableFuture.completedFuture(response);
    }

    @Async
    public CompletableFuture<ApiResponse<?>> getCycleListByFixedFactor(String userName, List<Map<String, String>> factorKind, List<Map<String, String>> fixedFactor, String yFactor, String variable) {
        ApiResponse<?> response;
        List<Factor> factors = factorCustomRepository.findByMultipleKindsAndCriteria(userName, factorKind, fixedFactor, variable);

        List<String> experimentIds = factors.stream()
                .map(Factor::getExperimentId)
                .toList();

        List<CompletableFuture<List<?>>> futureCycleDtos = experimentIds.stream()
                .map(id -> getCyclesAsync(id, yFactor))
                .toList();

        CompletableFuture.allOf(futureCycleDtos.toArray(new CompletableFuture[0])).join();
        Map<String, List<Object>> cycleDtoMap = new HashMap<>();
        for (int i = 0; i < experimentIds.size(); i++) {
            try {
                String experimentId = experimentIds.get(i);
                List<?> cycleData = futureCycleDtos.get(i).get();

                // Map에 리스트가 없다면 초기화
                cycleDtoMap.putIfAbsent(experimentId, new ArrayList<>());

                // 리스트에 데이터를 추가 (타입을 명확히 하기 위해 캐스팅)
                cycleDtoMap.get(experimentId).addAll((List<Object>) cycleData);
            } catch (Exception ignored) {
                // 예외 처리
            }
        }

        if (yFactor.equals("dchgToChg")) {
            System.out.println("Processing yFactor: dchgToChg");
            List<ResponseDto.CycleDchgToChg> cycleDchgToChgList = factors.stream()
                    .map(meta -> new ResponseDto.CycleDchgToChg(
                            meta,
                            Map.of("cycle: " + meta.getExperimentId(),
                                    (List<CycleDto.dchgToChg>) (List<?>) cycleDtoMap.getOrDefault(meta.getExperimentId(), new ArrayList<>()))
                    ))
                    .toList();
            response = ApiResponse.fromResultStatus(ApiStatus.SUC_EXPERIMENT_READ, cycleDchgToChgList);
        } else if (yFactor.equals("chgToDchg")) {
            List<ResponseDto.CycleChgToDchg> cycleChgToDchgList = factors.stream()
                    .map(meta -> new ResponseDto.CycleChgToDchg(
                            meta,
                            Map.of("cycle: " + meta.getExperimentId(),
                                    (List<CycleDto.chgToDchg>) (List<?>) cycleDtoMap.getOrDefault(meta.getExperimentId(), new ArrayList<>()))
                    ))
                    .toList();
            response = ApiResponse.fromResultStatus(ApiStatus.SUC_EXPERIMENT_READ, cycleChgToDchgList);
        } else {
            throw new IllegalArgumentException("Unsupported yFactor type");
        }

        return CompletableFuture.completedFuture(response);
    }


    public CompletableFuture<ApiResponse<?>> fetchExperiementWithOutliers(String userName, List<Map<String, String>> factorKind, List<Map<String, String>> fixedFactor, String variable) {
        ApiResponse<?> response;
        List<Factor> factors = factorCustomRepository.findByMultipleKindsAndCriteria(userName, factorKind, fixedFactor, variable);

        List<String> experimentIds = factors.stream()
                .map(Factor::getExperimentId)
                .toList();

        List<CompletableFuture<List<?>>> futureOutlierDtos = experimentIds.stream()
                .map(id -> getOutliersAsync(id))
                .toList();

        CompletableFuture.allOf(futureOutlierDtos.toArray(new CompletableFuture[0])).join();
        Map<String, List<Object>> outlierDtoMap = new HashMap<>();
        for (int i = 0; i < experimentIds.size(); i++) {
            try {
                String experimentId = experimentIds.get(i);
                List<?> outlierData = futureOutlierDtos.get(i).get();

                // Map에 리스트가 없다면 초기화
                outlierDtoMap.putIfAbsent(experimentId, new ArrayList<>());

                // 리스트에 데이터를 추가 (타입을 명확히 하기 위해 캐스팅)
                outlierDtoMap.get(experimentId).addAll((List<Object>) outlierData);
            } catch (Exception ignored) {
                // 예외 처리
            }
        }


        List<ResponseDto.CycleOutlying> cycleOutlierList = factors.stream()
                .map(meta -> new ResponseDto.CycleOutlying(
                        meta,
                        Map.of("cycle: " + meta.getExperimentId(),
                                (List<CycleDto.withOutlier>) (List<?>) outlierDtoMap.getOrDefault(meta.getExperimentId(), new ArrayList<>()))
                ))
                .toList();
        response = ApiResponse.fromResultStatus(ApiStatus.SUC_EXPERIMENT_READ, cycleOutlierList);


        return CompletableFuture.completedFuture(response);
    }
    public ApiResponse<Void> deleteExperimentData(String experimentId){
        factorRepository.findAllByExperimentId(experimentId).forEach(e -> {
                    timeDataRepository.deleteByExperimentId(e.getExperimentId());
                    cycleDataRepository.deleteByExperimentId(e.getExperimentId());
                    factorRepository.deleteByExperimentId(e.getExperimentId());}
        );
        return ApiResponse.fromResultStatus(ApiStatus.SUC_EXPERIMENT_DELETE);
    }
}
