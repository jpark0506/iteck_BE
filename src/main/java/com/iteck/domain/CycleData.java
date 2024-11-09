package com.iteck.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Map;

@Document(collection = "cycle_data")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CycleData {
    @Id
    private String id;
    @Indexed
    private String experimentId;
    private int chunkId; // 청크 ID (1, 2, 3, ...)
    private List<Map<String, Object>> expSpec;
}
