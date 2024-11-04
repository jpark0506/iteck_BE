package com.iteck.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Document(collection = "experiment_meta")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExperimentMeta {
    @Id
    private String id;
    private String experimentId;
    private String userName;
    private String title;
    private String memo;
    private Instant expDate; // mongodb는 kst 미지원.
    private Instant regDate;
    private Map<String, FactorDetail> factors;
}
