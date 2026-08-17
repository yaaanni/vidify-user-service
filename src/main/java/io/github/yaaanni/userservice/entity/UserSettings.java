package io.github.yaaanni.userservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSettings {

    @Id
    @Column(name = "user_id", updatable = false, nullable = false)
    private UUID userId;

    @Column(name = "auto_convert_to_video", nullable = false)
    @Builder.Default
    private boolean autoConvertToVideo = false;

    @Column(name = "notification_email", nullable = false)
    private String notificationEmail;

    @Column(name = "language", nullable = false, length = 2)
    @Builder.Default
    private String language = "EN";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;
}
