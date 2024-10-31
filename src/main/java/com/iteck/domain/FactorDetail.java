package com.iteck.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FactorDetail {
    private Map<String, String> details = new HashMap<>();  // 유연한 키-값 쌍 관리
}
