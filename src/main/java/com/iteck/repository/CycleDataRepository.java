package com.iteck.repository;

import com.iteck.domain.CycleData;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CycleDataRepository extends MongoRepository<CycleData, String> {

    List<CycleData> findByExperimentId(String experimentId);
}
