package com.iteck.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.util.List;

@Document(collection = "experiment")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ExperimentMeta {
    @Id
    private String id;
    private String experimentId;
    private int chunkId;
    private String userName;
    private String title;
    private String memo;
    private LocalDate expDate;
    private List<String> rowData;

}
