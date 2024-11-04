package com.iteck.repository;

import com.iteck.domain.CycleData;
import com.iteck.domain.TimeData;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TimeDataRepository extends MongoRepository<TimeData, String> {
    List<TimeData> findByExperimentId(String experimentId);
    void deleteByExperimentId(String experimentId);
}

