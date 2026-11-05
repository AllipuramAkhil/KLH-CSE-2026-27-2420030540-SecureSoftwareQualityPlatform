package com.assqp.platform.dto.testing;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestCaseRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotNull(message = "Test type is required")
    private String testType;

    private String priority;
    private String preconditions;
    private String testSteps;
    private String expectedResult;
    private String relatedComponent;
}
