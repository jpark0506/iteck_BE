package com.iteck.repository;

import com.iteck.domain.ExperimentMeta;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExperimentMetaRepository extends MongoRepository<ExperimentMeta, String> {
    public List<ExperimentMeta> findByUserName(String userName);
    @Query("{ 'factors.?0' : { $exists: true } }")
    public List<ExperimentMeta> findByDynamicFactorKey(String key);

}
