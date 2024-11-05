package com.iteck.repository;

import com.iteck.domain.ExperimentMeta;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ExperimentMetaCustomRepository {

    @Autowired
    private MongoTemplate mongoTemplate;

    public List<ExperimentMeta> findByFactorKeyExists(String kind, String key) {
        String fieldPath = "factors." + kind + ".details." + key;

        // 해당 경로에 키가 존재하는 문서를 조회
        Query query = new Query(Criteria.where(fieldPath).exists(true));

        return mongoTemplate.find(query, ExperimentMeta.class);
    }
}

