package com.iteck.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document(collection = "experiment_chunks")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ExperimentChunk {

    @Id
    private String id;
    private String experimentId; // 실험 ID
    private int chunkId; // 청크 ID (1, 2, 3, ...)
    private List<String> rowData; // 각 청크의 데이터

}