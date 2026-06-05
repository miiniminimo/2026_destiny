package com.destinycode.saju;

import com.destinycode.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "saju_info")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SajuInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String gender;

    @Column(name = "calendar_type", nullable = false)
    private String calendarType;

    @Column(name = "birth_year", nullable = false)
    private Integer birthYear;

    @Column(name = "birth_month", nullable = false)
    private Integer birthMonth;

    @Column(name = "birth_day", nullable = false)
    private Integer birthDay;

    @Column(name = "birth_time")
    private String birthTime;

    @Column(name = "birth_place", nullable = false)
    private String birthPlace;

    @Lob
    @Column(name = "image_data", columnDefinition = "LONGTEXT")
    private String imageData;

    /** DALL-E 이미지 생성 완료 여부 (비동기 처리) */
    @Column(name = "image_ready", nullable = false)
    @Builder.Default
    private Boolean imageReady = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
