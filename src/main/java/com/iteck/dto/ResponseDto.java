package com.iteck.dto;

import com.iteck.domain.Factor;
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
        private Factor meta;
        private Map<String, List<TimeDto.withVoltage>> timeDatas;
    }
    @Getter
    @Builder
    @AllArgsConstructor
    public static class TimeWithCurrent{
        private Factor meta;
        private Map<String, List<TimeDto.withCurrent>> timeDatas;
    }
    @Getter
    @Builder
    @AllArgsConstructor
    public static class CycleDchgToChg{
        private Factor meta;
        private Map<String, List<CycleDto.dchgToChg>> cycleDatas;
    }
    @Getter
    @Builder
    @AllArgsConstructor
    public static class CycleChgToDchg{
        private Factor meta;
        private Map<String, List<CycleDto.chgToDchg>> cycleDatas;
    }
    @Getter
    @Builder
    @AllArgsConstructor
    public static class CycleOutlying{
        private Factor meta;
        private Map<String, List<CycleDto.withOutlier>> cycleDatas;
    }
    @Getter
    @Builder
    @AllArgsConstructor
    public static class VoltageWithDQmDv{
        private Factor meta;
        private Map<String, List<VoltageDto.withDQDV>> voltageDatas;
    }
}
