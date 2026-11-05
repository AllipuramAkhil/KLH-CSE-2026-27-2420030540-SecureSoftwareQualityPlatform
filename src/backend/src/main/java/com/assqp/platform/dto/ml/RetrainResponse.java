package com.assqp.platform.dto.ml;

import lombok.*;

/** Mirrors app/schemas.py RetrainResponse in ml-service. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RetrainResponse {
    private String status;
    private String message;
    private ModelInfoResponse metadata;
}
