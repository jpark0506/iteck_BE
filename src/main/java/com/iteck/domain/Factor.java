package com.iteck.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document(collection = "experiment")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Factor {
    @Id
    private String id;
    private List<String> rowData;
    private List<String> isFixed;
}
