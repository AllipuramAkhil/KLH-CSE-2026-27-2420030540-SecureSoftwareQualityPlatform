package com.assqp.platform.dto.project;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectResponse {
    private Long id;
    private String name;
    private String description;
    private String repositoryUrl;
    private String branch;
    private String technologyStack;
    private String status;
    private String ownerName;
    private Double qualityScore;
    private Double securityScore;
    private Double riskScore;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
