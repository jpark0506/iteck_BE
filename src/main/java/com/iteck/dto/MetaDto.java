package com.iteck.dto;

import com.iteck.domain.FactorDetail;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class MetaDto {
    private String userName;
    private String title;
    private String memo;
    private Map<String, FactorDetail> factors;
}
