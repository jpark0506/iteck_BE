package com.iteck.repository;

import com.iteck.domain.ExperimentChunk;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExperimentChunkRepository extends MongoRepository<ExperimentChunk, String> {
    // 청크 데이터를 조회하기 위한 메서드
    List<ExperimentChunk> findByExperimentId(String experimentId);
}