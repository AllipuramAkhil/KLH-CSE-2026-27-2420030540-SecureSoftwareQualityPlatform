package com.assqp.platform.dto.churn;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GitChurnStatusResponse {
    private boolean gitAvailable;
    private String repoPath;
    private int lookbackDays;
}
