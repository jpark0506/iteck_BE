package com.iteck.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class MetaDto {
    private String userName;
    private String title;
    private String memo;
    private Map<String, String> factors;
}
