package com.iteck.service;

import com.iteck.domain.ExcelData;
import com.iteck.repository.ExcelDataRepository;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FileService {
    @Autowired
    private final ExcelDataRepository excelDataRepository;
    public List<ExcelData> createExcelData(MultipartFile file) throws IOException {
        Workbook workbook = new XSSFWorkbook(file.getInputStream());
        Sheet sheet = workbook.getSheetAt(0);
        List<ExcelData> excelDataList = new ArrayList<>();
        for (Row row : sheet) {
            List<String> rowData = new ArrayList<>();
            for (Cell cell : row) {
                rowData.add(cell.toString());
            }
            ExcelData excelData = new ExcelData();
            excelData.setRowData(rowData);
            excelDataList.add(excelData);
        }
        workbook.close();
        // MongoDB에 저장
        excelDataRepository.saveAll(excelDataList);

        return excelDataList;
    }
    public List<ExcelData> readExcelData(){return excelDataRepository.findAll();}
}
