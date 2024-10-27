package com.iteck.service;

import com.iteck.domain.ExperimentChunk;
import com.iteck.domain.ExperimentMeta;
import com.iteck.dto.ApiResponse;
import com.iteck.dto.MetaDto;
import com.iteck.repository.ExperimentChunkRepository;
import com.iteck.repository.ExperimentMetaRepository;
import com.iteck.util.ApiStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExperimentService {
    private final ExperimentMetaRepository experimentMetaRepository;
    private final ExperimentChunkRepository experimentChunkRepository;
    public ApiResponse<?> createExperimentData(MultipartFile file, MetaDto metaDto) throws IOException {
        String experimentId = UUID.randomUUID().toString(); // 고유 실험 ID 생성

        // CSV 파일인지 확인
        if (!"text/csv".equals(file.getContentType()) && !"application/vnd.ms-excel".equals(file.getContentType())) {
            return ApiResponse.fromResultStatus(ApiStatus.BAD_REQUEST);
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()));
        String line;
        List<String> rowDataList = new ArrayList<>();
        System.out.println(metaDto.getUserName());
        System.out.println(metaDto.getMemo());
        System.out.println(metaDto.getTitle());
        ExperimentMeta experimentMeta = ExperimentMeta.builder()
                .userName(metaDto.getUserName())
                .title(metaDto.getTitle())
                .memo(metaDto.getMemo())
                .factors(metaDto.getFactors())
                .expDate(ZonedDateTime.now(ZoneId.of("Asia/Seoul")).toInstant()) // 시간을 조회 할 때 아래처럼 실행.
                //Instant expDateInstant = retrievedDoc.getExpDate();
                //ZonedDateTime expDateKST = expDateInstant.atZone(ZoneId.of("Asia/Seoul"));
                .build();
        experimentMetaRepository.save(experimentMeta);

        // 나머지 줄은 rowData
        while ((line = reader.readLine()) != null) {
            String[] data = line.split(",");
            rowDataList.addAll(Arrays.asList(data));
        }

        // rowData를 10,000개씩 나누어 Chunk로 저장
        int chunkSize = 10000;
        int totalChunks = (int) Math.ceil((double) rowDataList.size() / chunkSize);

        List<ExperimentChunk> chunks = new ArrayList<>();
        for (int i = 0; i < totalChunks; i++) {
            ExperimentChunk chunk = new ExperimentChunk();
            chunk.setExperimentId(experimentId);
            chunk.setChunkId(i + 1);
            chunk.setRowData(rowDataList.subList(i * chunkSize, Math.min((i + 1) * chunkSize, rowDataList.size())));
            chunks.add(chunk);
        }

        // MongoDB에 청크 저장
        experimentChunkRepository.saveAll(chunks);

        return ApiResponse.fromResultStatus(ApiStatus.SUC_EXPERIMENT_CREATE, experimentMeta);
    }
    public ApiResponse<?> getExperimentMetasByUser(String userName) {
        List<ExperimentMeta> experimentMetas = experimentMetaRepository.findByUserName(userName);
        return ApiResponse.fromResultStatus(ApiStatus.SUC_EXPERIMENT_READ, experimentMetas);
    }
    public ApiResponse<?> getExperimentChunksByExperimentId(String expId) {
        System.out.println(expId);
        List<ExperimentChunk> experimentChunks = experimentChunkRepository.findByExperimentId(expId);
        return ApiResponse.fromResultStatus(ApiStatus.SUC_EXPERIMENT_READ, experimentChunks);
    }

}
