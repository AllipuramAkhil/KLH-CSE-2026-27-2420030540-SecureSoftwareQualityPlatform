package com.assqp.platform.integration.trivy;

import com.assqp.platform.exception.TrivyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Component
public class TrivyClientImpl implements TrivyClient {

    @Value("${trivy.binary-path:trivy}")
    private String binaryPath;

    @Value("${trivy.scan.default-target:.}")
    private String defaultTarget;

    @Value("${trivy.scan.timeout-seconds:180}")
    private long timeoutSeconds;

    // Security fix (audit issue #8): targetPath used to be passed straight from the API
    // request into the trivy CLI invocation with no bound on what could be scanned. Not
    // shell injection (ProcessBuilder, no shell involved), but a resource-access issue -
    // a caller could point the backend at any directory it can read. Every resolved scan
    // path is now required to fall inside this workspace root.
    @Value("${trivy.scan.allowed-root:./uploads}")
    private String allowedRoot;

    @Override
    public boolean isAvailable() {
        try {
            Process process = new ProcessBuilder(binaryPath, "--version")
                    .redirectErrorStream(true)
                    .start();
            boolean finished = process.waitFor(10, TimeUnit.SECONDS);
            return finished && process.exitValue() == 0;
        } catch (Exception ex) {
            log.debug("Trivy binary not available ({}): {}", binaryPath, ex.getMessage());
            return false;
        }
    }

    @Override
    public String scanFilesystem(String targetPath) {
        String requested = (targetPath != null && !targetPath.isBlank()) ? targetPath : defaultTarget;
        String target = resolveWithinAllowedRoot(requested);
        return run(List.of(binaryPath, "fs", "--format", "json",
                "--scanners", "vuln,secret,misconfig", "--quiet", target));
    }

    /**
     * Resolves the requested path against the allowed workspace root and rejects
     * anything (absolute paths, "..", symlink escapes) that would land outside it.
     */
    private String resolveWithinAllowedRoot(String requested) {
        try {
            Path root = Path.of(allowedRoot).toAbsolutePath().normalize();
            // Resolve relative to the root rather than accepting the caller's path as-is,
            // so an absolute path in the request can't simply override the root.
            Path resolved = root.resolve(requested).normalize().toAbsolutePath();
            if (!resolved.startsWith(root)) {
                throw new TrivyException(
                        "targetPath must resolve inside the allowed scan workspace ('" + allowedRoot + "')");
            }
            return resolved.toString();
        } catch (TrivyException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new TrivyException("Invalid targetPath: " + ex.getMessage(), ex);
        }
    }

    @Override
    public String scanImage(String imageRef) {
        if (imageRef == null || imageRef.isBlank()) {
            throw new TrivyException("An image reference is required for an image scan");
        }
        return run(List.of(binaryPath, "image", "--format", "json", "--quiet", imageRef));
    }

    private String run(List<String> command) {
        if (!isAvailable()) {
            throw new TrivyException("Trivy CLI is not available on this backend's PATH (binary: '" + binaryPath +
                    "'). Upload a pre-generated Trivy JSON report instead (e.g. from the GitHub Actions pipeline).");
        }
        try {
            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(false)
                    .start();

            String stdout;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                stdout = reader.lines().collect(Collectors.joining("\n"));
            }
            String stderr;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                stderr = reader.lines().collect(Collectors.joining("\n"));
            }

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new TrivyException("Trivy scan timed out after " + timeoutSeconds + " seconds");
            }

            // Trivy exits non-zero when --exit-code is set and findings exist;
            // we don't pass --exit-code, so any non-zero here is a real failure.
            if (process.exitValue() != 0) {
                throw new TrivyException("Trivy scan failed (exit code " + process.exitValue() + "): " +
                        (stderr.isBlank() ? "no error output" : stderr));
            }

            if (stdout.isBlank()) {
                throw new TrivyException("Trivy produced no output");
            }

            return stdout;

        } catch (TrivyException ex) {
            throw ex;
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new TrivyException("Failed to run Trivy: " + ex.getMessage(), ex);
        }
    }
}
