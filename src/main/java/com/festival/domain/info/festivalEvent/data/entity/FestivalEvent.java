package com.festival.domain.info.festivalEvent.data.entity;


import com.festival.domain.admin.data.entity.Admin;
import com.festival.domain.info.festivalEvent.data.dto.FestivalEventReq;
import com.festival.domain.info.festivalPub.data.entity.file.PubImage;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FestivalEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "festivalEvent_id")
    private Long id;

    @Column(name = "festivalEvent_title",nullable = false)
    private String title;

    @Column(name = "festivalEvent_subTitle",nullable = false)
    private String subTitle;

    @Column(name = "festivalEvent_content", nullable = false)
    private String content;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    @JoinColumn(name = "festivalEventImage_id")
    private FestivalEventImage festivalEventImage;

    @Column(name = "festivalEvent_latitude",nullable = false) // 위도
    private int latitude;

    @Column(name = "festivalEvent_longitude",nullable = false) // 경도
    private int longitude;

    @Column(nullable = false)
    private Boolean festivalEventState;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "festivalEvent_admin_id", nullable = false)
    private Admin admin;

    @Builder
    public FestivalEvent(String title, String subTitle, String content, int latitude,FestivalEventImage festivalEventImage, int longitude, Boolean festivalEventState, Admin admin) {
        this.title = title;
        this.subTitle = subTitle;
        this.content = content;
        this.latitude = latitude;
        this.longitude = longitude;
        this.festivalEventImage = festivalEventImage;
        this.festivalEventState = festivalEventState;
        this.admin = admin;
    }

    public static FestivalEvent of(FestivalEventReq festivalEventReq, FestivalEventImage festivalEventImage, Admin admin) {
        return FestivalEvent.builder()
                .title(festivalEventReq.getTitle())
                .subTitle(festivalEventReq.getSubTitle())
                .content(festivalEventReq.getContent())
                .latitude(festivalEventReq.getLatitude())
                .longitude(festivalEventReq.getLongitude())
                .festivalEventState(festivalEventReq.getFestivalEventState())
                .festivalEventImage(festivalEventImage)
                .admin(admin)
                .build();
    }
}