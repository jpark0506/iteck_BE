package com.iteck.service;

import com.iteck.domain.ExcelData;
import com.iteck.domain.Experiment;
import com.iteck.dto.ApiResponse;
import com.iteck.repository.ExcelDataRepository;
import com.iteck.repository.ExperimentRepository;
import com.iteck.util.ApiStatus;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FileService {
    private final ExperimentRepository experimentRepository;
    public ApiResponse<?> createExperimentData(MultipartFile file) throws IOException {
        // CSV 파일인지 확인
        if (!"text/csv".equals(file.getContentType()) && !"application/vnd.ms-excel".equals(file.getContentType())) {
            return ApiResponse.fromResultStatus(ApiStatus.BAD_REQUEST);
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()));
        String line;
        List<Experiment> experiments = new ArrayList<>();

        while ((line = reader.readLine()) != null) {
            String[] data = line.split(",");
            Experiment experiment = new Experiment();
            experiment.setRowData(Arrays.asList(data));
            experiments.add(experiment);
        }

        // MongoDB에 저장
        experimentRepository.saveAll(experiments);
        System.out.println("success");
        return ApiResponse.fromResultStatus(ApiStatus.SUC_EXPERIMENT_CREATE, experiments);
    }

    public List<Experiment> readExcelData(){ return experimentRepository.findAll();}
}
