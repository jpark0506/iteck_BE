package com.iteck.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

public class VoltageDto {
    @Getter
    @Builder
    @AllArgsConstructor
    public static class withDQDV{
        private String voltage;
        private String dQmdV;
    }

    public static Object createVoltageDto(String voltage, String dQmdV){
        return withDQDV.builder()
                .voltage(voltage)
                .dQmdV(dQmdV)
                .build();
    }
}
