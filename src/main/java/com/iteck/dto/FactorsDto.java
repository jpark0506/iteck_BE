package com.iteck.dto;

import com.iteck.domain.FactorDetail;
import lombok.Getter;

import java.util.Map;

@Getter
public class FactorsDto {
    private String userName;
    private Map<String, FactorDetail> factors;
}
