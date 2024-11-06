package com.iteck.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

@Document(collection = "factor")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Factor {
    @Id
    private String id;
    private String userName;
    private String experimentId;
    private Map<String, FactorDetail> factors;
}
