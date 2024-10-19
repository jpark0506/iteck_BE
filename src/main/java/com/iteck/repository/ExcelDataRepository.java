package com.iteck.repository;

import com.iteck.domain.ExcelData;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ExcelDataRepository extends MongoRepository<ExcelData, String> {
}
