package com.assqp.platform.entity;

import com.assqp.platform.enums.ExecutionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "test_executions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_case_id", nullable = false)
    private TestCase testCase;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExecutionStatus status;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long durationMs;

    @Column(length = 2000)
    private String result;

    @Column(length = 4000)
    private String logs;

    @Column(length = 2000)
    private String errorMessage;

    @Column(length = 100)
    private String environment;

    @Column(name = "build_number", length = 100)
    private String buildNumber;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
