package com.iteck.repository;

import com.iteck.domain.Factor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Repository
public class FactorCustomRepository {

    @Autowired
    private MongoTemplate mongoTemplate;
    public List<Factor> findByFactorKeyExists(String kind, String key) {
        String fieldPath = "factors." + kind + ".details." + key;

        // 해당 경로에 키가 존재하는 문서를 조회
        Query query = new Query(Criteria.where(fieldPath).exists(true));

        return mongoTemplate.find(query, Factor.class);
    }

    public List<Factor> findByMultipleKindsAndCriteria(List<Map<String, String>> kindKeyMap, List<Map<String, String>> kindValueMap) {
        if ((kindKeyMap == null || kindKeyMap.isEmpty()) && (kindValueMap == null || kindValueMap.isEmpty())) {
            System.out.println("Both kindKeyMap and kindValueMap are empty or null. Returning empty result.");
            return Collections.emptyList(); // 빈 리스트 반환
        }

        List<Criteria> kindCriteriaList = new ArrayList<>();

        // keyMap 반복문을 먼저 처리하여 모든 키 조건을 추가
        if (kindKeyMap != null && !kindKeyMap.isEmpty()) {
            for (Map<String, String> keyMap : kindKeyMap) {
                for (Map.Entry<String, String> entry : keyMap.entrySet()) {
                    String kind = entry.getKey();
                    String key = entry.getValue();
                    String fieldPath = "factors." + kind + ".details." + key;

                    if (valueMapExistsForKind(kindValueMap, kind)) {
                        String value = getValueFromMap(kindValueMap, kind);
                        kindCriteriaList.add(Criteria.where(fieldPath).is(value)); // key와 value 모두 일치하는 조건 추가
                        System.out.println("Added criteria for key and value: " + fieldPath + " = " + value);
                    } else {
                        kindCriteriaList.add(Criteria.where(fieldPath).exists(true)); // key만 존재하는 조건 추가
                        System.out.println("Added criteria for key only: " + fieldPath);
                    }
                }
            }
        }

        // valueMap 반복문을 처리하여 key 조건이 없는 경우 value 조건 추가
        if (kindValueMap != null && !kindValueMap.isEmpty()) {
            for (Map<String, String> valueMap : kindValueMap) {
                for (Map.Entry<String, String> entry : valueMap.entrySet()) {
                    String kind = entry.getKey();
                    String value = entry.getValue();
                    String fieldPath = "factors." + kind + ".details";

                    if (!keyMapExistsForKind(kindKeyMap, kind)) {
                        kindCriteriaList.add(new Criteria().orOperator(
                                Criteria.where(fieldPath).is(value),
                                Criteria.where(fieldPath).exists(true).andOperator(
                                        Criteria.where("$where").is("function() { " +
                                                "for (var key in this." + fieldPath + ") { " +
                                                "if (this." + fieldPath + "[key] == '" + value + "') { return true; } } return false; }")
                                )
                        ));
                        System.out.println("Added criteria for value only using $where: " + fieldPath + " contains value " + value);
                    }
                }
            }
        }




        // 유효한 조건이 없으면 빈 리스트 반환
        if (kindCriteriaList.isEmpty()) {
            System.out.println("No valid criteria found for the query. Returning empty result.");
            return Collections.emptyList();
        }

        // 전체 조건을 AND로 결합
        Criteria combinedCriteria = new Criteria().andOperator(kindCriteriaList.toArray(new Criteria[0]));
        Query query = new Query(combinedCriteria);

        System.out.println("Generated query: " + query.toString());

        return mongoTemplate.find(query, Factor.class);
    }


    private boolean valueMapExistsForKind(List<Map<String, String>> valueMaps, String kind) {
        if (valueMaps == null) return false;
        for (Map<String, String> valueMap : valueMaps) {
            if (valueMap.containsKey(kind)) return true;
        }
        return false;
    }

    private boolean keyMapExistsForKind(List<Map<String, String>> keyMaps, String kind) {
        if (keyMaps == null) return false;
        for (Map<String, String> keyMap : keyMaps) {
            if (keyMap.containsKey(kind)) return true;
        }
        return false;
    }

    private String getValueFromMap(List<Map<String, String>> valueMaps, String kind) {
        if (valueMaps == null) return null;
        for (Map<String, String> valueMap : valueMaps) {
            if (valueMap.containsKey(kind)) return valueMap.get(kind);
        }
        return null;
    }



}

