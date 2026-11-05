package com.assqp.platform.integration.zap;

import com.assqp.platform.enums.Severity;
import lombok.*;

/**
 * A single alert parsed out of a ZAP response/report, already normalized
 * to this platform's {@link Severity} scale. Intermediate representation
 * between {@link ZapReportParser} and ZapServiceImpl - never persisted
 * directly, mapped into SecurityVulnerability instead.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ZapAlert {
    /** ZAP's numeric plugin/alert id - used as SecurityVulnerability.externalId for dedup. */
    private String pluginId;
    private String name;
    private Severity severity;
    /** ZAP's confidence label (e.g. "High", "Medium", "Low"), kept as free text. */
    private String confidence;
    private String description;
    private String solution;
    /** The specific URL/URI this alert was raised against. */
    private String url;
    private String cweId;
}
