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
    private String gender; // MALE, FEMALE

    @Column(name = "calendar_type", nullable = false)
    private String calendarType; // SOLAR, LUNAR_PLAIN, LUNAR_LEAP

    @Column(name = "birth_year", nullable = false)
    private Integer birthYear;

    @Column(name = "birth_month", nullable = false)
    private Integer birthMonth;

    @Column(name = "birth_day", nullable = false)
    private Integer birthDay;

    @Column(name = "birth_time")
    private String birthTime; // "HH:mm" 포맷, 모를 경우 null

    @Column(name = "birth_place", nullable = false)
    private String birthPlace;

    @Lob
    @Column(name = "image_data", columnDefinition = "LONGTEXT")
    private String imageData;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
