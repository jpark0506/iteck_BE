package com.iteck.repository;

import com.iteck.domain.Meta;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MetaRepository extends MongoRepository<Meta, String> {
    public List<Meta> findAllByUserName(String username);
}
