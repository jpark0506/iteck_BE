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
    public static class TimeWithVoltage{
        private ExperimentMeta meta;
        private Map<String, List<TimeDto.withVoltage>> timeDatas;
    }
    @Getter
    @Builder
    @AllArgsConstructor
    public static class TimeWithCurrent{
        private ExperimentMeta meta;
        private Map<String, List<TimeDto.withCurrent>> timeDatas;
    }
    @Getter
    @Builder
    @AllArgsConstructor
    public static class CycleDchgToChg{
        private ExperimentMeta meta;
        private Map<String, List<CycleDto.dchgToChg>> cycleDatas;
    }
    @Getter
    @Builder
    @AllArgsConstructor
    public static class CycleChgToDchg{
        private ExperimentMeta meta;
        private Map<String, List<CycleDto.chgToDchg>> cycleDatas;
    }
    @Getter
    @Builder
    @AllArgsConstructor
    public static class VoltageWithDQmDv{
        private ExperimentMeta meta;
        private Map<String, List<VoltageDto.withDQDV>> voltageDatas;
    }
}
