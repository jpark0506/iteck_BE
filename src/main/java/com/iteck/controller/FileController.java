package com.iteck.controller;

import com.iteck.domain.ExcelData;
import com.iteck.repository.ExcelDataRepository;
import com.iteck.service.FileService;
import lombok.AllArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin("*")
@AllArgsConstructor
public class FileController {
    //private final VertexAiGeminiChatModel vertexAiGeminiChatModel;
    private final FileService fileService;

    /*@GetMapping("/chat")
    public Map<String, String> chat(@RequestBody String message) {
        Map<String, String> responses = new HashMap<>();


        String vertexAiGeminiResponse = vertexAiGeminiChatModel.call(message);
        responses.put("vertexai(gemini) 응답", vertexAiGeminiResponse);
        return responses;
    }*/
    @PostMapping("/upload")
    public ResponseEntity<?> handleFileUpload(@RequestParam("file") MultipartFile file) {
        try {
            return ResponseEntity.ok(fileService.createExcelData(file));
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to upload file");
        }
    }
}