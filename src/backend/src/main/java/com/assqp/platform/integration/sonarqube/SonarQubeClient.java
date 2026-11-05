package com.assqp.platform.integration.sonarqube;

/**
 * Client for retrieving analysis results from a real SonarQube (or
 * SonarCloud) server via its Web API. Configured via SONAR_HOST_URL /
 * SONAR_TOKEN / SONAR_PROJECT_KEY. Must never throw for "not configured" -
 * callers should check {@link #isAvailable()} first - and must wrap any
 * network/parse failure in {@link com.assqp.platform.exception.SonarQubeException}
 * so the rule-based analyzer can keep functioning regardless.
 */
public interface SonarQubeClient {

    /** True only when a host URL and token are both configured. Does not guarantee reachability. */
    boolean isAvailable();

    /**
     * Fetches the current measures for the given (or configured default)
     * Sonar project key.
     *
     * @param sonarProjectKey the Sonar project key to query; if null/blank, the
     *                        configured default (sonar.project.key) is used
     */
    SonarQubeMeasures fetchMeasures(String sonarProjectKey);
}
