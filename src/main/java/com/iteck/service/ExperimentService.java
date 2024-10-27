package com.iteck.service;

import com.iteck.domain.ExperimentChunk;
import com.iteck.domain.ExperimentMeta;
import com.iteck.dto.ApiResponse;
import com.iteck.repository.ExperimentChunkRepository;
import com.iteck.repository.ExperimentMetaRepository;
import com.iteck.util.ApiStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileService {
    private final ExperimentMetaRepository experimentMetaRepository;
    private final ExperimentChunkRepository experimentChunkRepository;
    public ApiResponse<?> createExperimentData(MultipartFile file) throws IOException {
        String user = "user1"; // 임의의 사용자 설정
        String experimentId = UUID.randomUUID().toString(); // 고유 실험 ID 생성

        // CSV 파일인지 확인
        if (!"text/csv".equals(file.getContentType()) && !"application/vnd.ms-excel".equals(file.getContentType())) {
            return ApiResponse.fromResultStatus(ApiStatus.BAD_REQUEST);
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()));
        String line;
        List<String> rowDataList = new ArrayList<>();

        // 파일의 첫 줄은 실험 메타데이터라고 가정
        String[] metadata = reader.readLine().split(",");

        // 실험 메타데이터 저장
        ExperimentMeta metadataObj = new ExperimentMeta();
        metadataObj.setExperimentId(experimentId);
        metadataObj.setUserName(user);
        metadataObj.setTitle("제목");
        metadataObj.setMemo("메모");
        metadataObj.setExpDate(LocalDate.parse("2020-08-03"));
        metadataObj.setParameters();
        experimentMetaRepository.save(metadataObj);

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

        return ApiResponse.fromResultStatus(ApiStatus.SUC_EXPERIMENT_CREATE, metadataObj);
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
