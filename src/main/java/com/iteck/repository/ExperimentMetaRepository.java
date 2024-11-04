package com.iteck.repository;

import com.iteck.domain.ExperimentMeta;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExperimentMetaRepository extends MongoRepository<ExperimentMeta, String> {
    public List<ExperimentMeta> findByUserName(String userName);
    public List<ExperimentMeta> findAllByTitle(String title);
    ExperimentMeta findFirstByTitle(String title);
    String findExperimentIdByTitle(String title);
    void deleteByExperimentId(String experimentId);

}
