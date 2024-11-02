package com.iteck.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

public class TimeDto {
    @Getter
    @Builder
    @AllArgsConstructor
    public static class withVoltage{
        private String totalTime;
        private String voltage;

    }
    @Getter
    @Builder
    @AllArgsConstructor
    public static class withCurrent{
        private String totalTime;
        private String current;
    }


    public static Object createTimeDto(String yFactor, String totalTime, String value){
        return switch (yFactor) {
            case "current" -> withCurrent.builder()
                    .totalTime(totalTime)
                    .current(value)
                    .build();
            case "voltage" -> withVoltage.builder()
                    .totalTime(totalTime)
                    .voltage(value)
                    .build();
            default -> throw new IllegalArgumentException("Unsupported yFactor type");
        };
    }

}
