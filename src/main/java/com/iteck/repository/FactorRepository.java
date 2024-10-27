package com.iteck.repository;

import com.iteck.domain.Factor;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FactorRepository extends MongoRepository<Factor, String> {
}
