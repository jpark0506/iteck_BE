package com.iteck.util;

import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileUtil {

    public static boolean isExcel(MultipartFile file){
        if (!"text/csv".equals(file.getContentType()) && !"application/vnd.ms-excel".equals(file.getContentType())) {
            return true;
        }
        return false;
    }

    public static Map<String, Object> createRowMap(String[] headers, String[] data, int startIdx, int endIdx) {
        Map<String, Object> rowMap = new HashMap<>();
        for (int i = startIdx; i < endIdx; i++) {
            rowMap.put(replaceDots(headers[i]), parseValue(data[i]));
        }
        return rowMap;
    }
    public static List<Integer> findCycleIndexPositions(String[] headers) {
        List<Integer> cycleIndexPositions = new ArrayList<>();
        for (int i = 0; i < headers.length; i++) {
            if (headers[i].toLowerCase().contains("cycle index")) {
                cycleIndexPositions.add(i);
            }
        }
        return cycleIndexPositions;
    }

    public static String replaceDots(String key) {
        return key.replace(".", "_");
    }

    public static Object parseValue(String value) {
        // 값이 숫자인지 여부를 확인하여 적절한 타입으로 변환
        try {
            if (value.contains(".")) {
                return Double.parseDouble(value); // 소수점이 포함된 경우 double로 변환
            } else {
                return Integer.parseInt(value);  // 정수로 변환
            }
        } catch (NumberFormatException e) {
            return value; // 숫자가 아닌 경우 문자열로 저장
        }
    }
}
