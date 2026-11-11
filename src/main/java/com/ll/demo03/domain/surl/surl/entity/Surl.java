package com.ll.demo03.domain.surl.surl.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ll.demo03.domain.member.member.entity.Member;
import com.ll.demo03.global.jpa.entity.BaseTime;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.*;

import static lombok.AccessLevel.PROTECTED;

@Entity
@NoArgsConstructor(access = PROTECTED)
@AllArgsConstructor(access = PROTECTED)
@Builder
@Getter
@Setter
public class Surl extends BaseTime {
    @ManyToOne
    @JsonIgnore
    private Member author;
    private String body;
    private String url;
    @Setter(AccessLevel.NONE)
    private long count;

    public void increaseCount() {
        count++;
    }
}
