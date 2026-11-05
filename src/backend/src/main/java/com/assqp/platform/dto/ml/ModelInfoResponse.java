package com.assqp.platform.dto.ml;

import lombok.*;

import java.util.List;

/** Mirrors app/schemas.py ModelInfoResponse in ml-service exactly (same field names, direct JSON passthrough). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModelInfoResponse {
    private String modelVersion;
    private String algorithm;
    private String trainedAt;
    private String datasetPath;
    private String datasetHash;
    private int datasetRows;
    private List<String> featureNames;
    private List<String> classLabels;
    private String notes;
}
