package com.iteck.repository;

import com.iteck.domain.Factor;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FactorRepository extends MongoRepository<Factor, String> {
    void deleteByExperimentId(String experimentId);
    List<Factor> findByUserName(String userName);
    List<Factor> findAllByExperimentId(String experimentId);

}
