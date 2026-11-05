package com.assqp.platform.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Security fix (audit issue #3): {@code POST /api/cicd/webhook} used to be reachable by
 * anyone who knew a project id, and could trigger real quality/security/risk analysis
 * work. It has to stay unauthenticated in the Spring Security sense (a CI system calling
 * it has no user/JWT), so instead it is authenticated the way GitHub-style webhooks are:
 * the caller must sign the raw request body with a shared secret and send it as
 * {@code X-Hub-Signature-256: sha256=<hex-hmac>}. Requests without a valid signature never
 * reach the controller.
 * <p>
 * Only wraps the webhook path - every other endpoint is untouched by this filter.
 */
@Slf4j
@Component
public class WebhookSignatureFilter extends OncePerRequestFilter {

    private static final String WEBHOOK_PATH = "/api/cicd/webhook";
    private static final String SIGNATURE_HEADER = "X-Hub-Signature-256";
    private static final String SIGNATURE_PREFIX = "sha256=";
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    @Value("${webhook.secret}")
    private String webhookSecret;

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return !WEBHOOK_PATH.equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {
        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request);

        String providedSignature = request.getHeader(SIGNATURE_HEADER);
        if (providedSignature == null || !isValidSignature(cachedRequest.getCachedBody(), providedSignature)) {
            log.warn("Rejected CI/CD webhook call from {} - missing or invalid {} header",
                    request.getRemoteAddr(), SIGNATURE_HEADER);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Missing or invalid webhook signature\"}");
            return;
        }

        filterChain.doFilter(cachedRequest, response);
    }

    private boolean isValidSignature(byte[] body, String providedHeader) {
        if (!providedHeader.startsWith(SIGNATURE_PREFIX)) {
            return false;
        }
        String providedHex = providedHeader.substring(SIGNATURE_PREFIX.length());

        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] expected = mac.doFinal(body);
            byte[] provided = HexFormat.of().parseHex(providedHex.toLowerCase());
            // Constant-time comparison - avoids leaking signature bytes via a timing side channel.
            return MessageDigest.isEqual(expected, provided);
        } catch (Exception ex) {
            log.warn("Could not verify webhook signature: {}", ex.getMessage());
            return false;
        }
    }
}
