package com.assqp.platform.dto.project;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectRequest {

    @NotBlank(message = "Project name is required")
    private String name;

    private String description;
    private String repositoryUrl;
    private String branch;
    private String technologyStack;
}
