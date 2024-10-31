package com.iteck.dto;

import com.iteck.domain.CycleData;
import com.iteck.domain.ExperimentMeta;
import com.iteck.domain.TimeData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;


public class ResponseDto {
    @Getter
    @Builder
    @AllArgsConstructor
    public static class Time{
        private ExperimentMeta meta;
        private Map<String, List<TimeData>> timeDatas;
    }
    @Getter
    @Builder
    @AllArgsConstructor
    public static class Cycle{
        private ExperimentMeta meta;
        private Map<String, List<CycleData>> cycleDatas;
    }
}
