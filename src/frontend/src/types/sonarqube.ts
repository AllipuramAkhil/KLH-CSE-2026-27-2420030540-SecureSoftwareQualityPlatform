// Mirrors SonarQubeAnalysisResponse.java / SonarQubeStatusResponse.java
export interface SonarQubeAnalysisResponse {
  id: number;
  projectId: number;
  sonarProjectKey: string;
  bugs: number | null;
  vulnerabilities: number | null;
  codeSmells: number | null;
  technicalDebtMinutes: number | null;
  complexity: number | null;
  maintainabilityRating: number | null;
  reliabilityRating: number | null;
  securityRating: number | null;
  coveragePercent: number | null;
  duplicatedLinesDensity: number | null;
  linesOfCode: number | null;
  importedAt: string;
}

export interface SonarQubeStatusResponse {
  configured: boolean;
  hostUrl: string | null;
  defaultProjectKey: string | null;
}

/** Sonar returns ratings as 1.0-5.0; this maps them back to the familiar A-E letters for display. */
export function sonarRatingToLetter(rating: number | null | undefined): string {
  if (rating == null) return '-';
  const letters = ['A', 'B', 'C', 'D', 'E'];
  const idx = Math.round(rating) - 1;
  return letters[Math.min(Math.max(idx, 0), 4)];
}
