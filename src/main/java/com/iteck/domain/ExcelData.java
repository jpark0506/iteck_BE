package com.iteck.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document(collection = "excelData")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ExcelData {
    @Id
    private String id;
    private List<String> rowData;

}
