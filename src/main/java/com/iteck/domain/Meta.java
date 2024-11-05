package com.iteck.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "meta")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Meta {
    @Id
    private String id;
    private String userName;
    private String title;
    private String memo;
    private Instant regDate;
}
