package com.iteck.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document(collection = "experiment")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Experiment {
    @Id
    private String id;
    private List<String> rowData;

}
