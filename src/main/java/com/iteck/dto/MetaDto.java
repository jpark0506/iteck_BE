package com.iteck.dto;

import com.iteck.domain.FactorDetail;
import com.iteck.domain.Meta;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MetaDto {
    private String userName;
    private String title;
    private String memo;

    public MetaDto(Meta meta) {
        this.userName = meta.getUserName();
        this.title = meta.getTitle();
        this.memo = meta.getMemo();
    }
}
