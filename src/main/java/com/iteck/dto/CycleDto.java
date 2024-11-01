package com.iteck.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

public class CycleDto {
    @Getter
    @Builder
    @AllArgsConstructor
    public static class dchgToChg{
        private String cycleIndex;
        private String dchgCap;
        private String dchgRatio;

    }
    @Getter
    @Builder
    @AllArgsConstructor
    public static class chgToDchg{
        private String cycleIndex;
        private String chgCap;
        private String chgRatio;
    }

    public static Object createCycleDto(String yFactor, String cycleIndex, String whichCap, String whichRatio){
        return switch (yFactor) {
            case "dchgToChg" -> dchgToChg.builder()
                    .cycleIndex(cycleIndex)
                    .dchgCap(whichCap)
                    .dchgRatio(whichRatio)
                    .build();

            case "chgToDchg" -> chgToDchg.builder()
                    .cycleIndex(cycleIndex)
                    .chgCap(whichCap)
                    .chgRatio(whichRatio)
                    .build();
            default -> throw new IllegalArgumentException("Unsupported yFactor type");
        };
    }
}
