package com.iteck.repository;

import com.iteck.domain.Experiment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExperimentRepository extends MongoRepository<Experiment, String> {
}
