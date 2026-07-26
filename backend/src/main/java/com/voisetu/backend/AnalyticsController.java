package com.voisetu.backend;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/public/analytics")
public class AnalyticsController {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsController.class);
    private final JdbcTemplate jdbcTemplate;
    private final AppUserRepository userRepository;

    public AnalyticsController(JdbcTemplate jdbcTemplate, AppUserRepository userRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.userRepository = userRepository;
    }

    @PostMapping("/events")
    public ResponseEntity<?> trackEvents(@RequestBody Map<String, Object> payload,
                                         HttpServletRequest request,
                                         Authentication auth) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> events = (List<Map<String, Object>>) payload.get("events");
            if (events == null || events.isEmpty()) {
                return ResponseEntity.ok().build();
            }

            String ip = request.getRemoteAddr();
            String ipHash = hashIp(ip);
            
            Long userId = null;
            if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
                Optional<AppUser> user = userRepository.findByEmail(auth.getName());
                if (user.isPresent()) {
                    userId = user.get().id();
                }
            }

            for (Map<String, Object> event : events) {
                String sessionId = (String) event.get("sessionId");
                String anonymousId = (String) event.get("anonymousId");
                String eventName = (String) event.get("name");
                String route = (String) event.get("route");
                
                @SuppressWarnings("unchecked")
                Map<String, Object> properties = (Map<String, Object>) event.get("properties");

                // Ensure session exists
                int count = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM analytics_session WHERE session_id = ?",
                        Integer.class, sessionId);

                if (count == 0) {
                    String deviceType = properties != null ? (String) properties.get("device_type") : null;
                    String browser = properties != null ? (String) properties.get("browser") : null;
                    String referrer = properties != null ? (String) properties.get("referrer") : null;

                    jdbcTemplate.update(
                            "INSERT INTO analytics_session (session_id, anonymous_id, user_id, ip_hash, device_type, browser, referrer) VALUES (?, ?, ?, ?, ?, ?, ?) ON CONFLICT DO NOTHING",
                            sessionId, anonymousId, userId, ipHash, deviceType, browser, referrer);
                } else if (userId != null) {
                    // Update user ID if they logged in during the session
                    jdbcTemplate.update("UPDATE analytics_session SET user_id = ? WHERE session_id = ? AND user_id IS NULL",
                            userId, sessionId);
                }

                // Insert event
                String propsJson = "{}";
                if (properties != null) {
                    // Simple manual JSON conversion for top-level keys
                    StringBuilder sb = new StringBuilder("{");
                    boolean first = true;
                    for (Map.Entry<String, Object> entry : properties.entrySet()) {
                        if (!first) sb.append(",");
                        sb.append("\"").append(entry.getKey()).append("\":");
                        if (entry.getValue() instanceof String) {
                            sb.append("\"").append(entry.getValue().toString().replace("\"", "\\\"")).append("\"");
                        } else {
                            sb.append(entry.getValue());
                        }
                        first = false;
                    }
                    sb.append("}");
                    propsJson = sb.toString();
                }

                jdbcTemplate.update(
                        "INSERT INTO analytics_event (session_id, event_name, route, properties) VALUES (?, ?, ?, ?::jsonb)",
                        sessionId, eventName, route, propsJson);
            }
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed to track analytics events", e);
            return ResponseEntity.ok().build(); // Always return 200 to beacon
        }
    }

    private String hashIp(String ip) {
        if (ip == null) return null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((ip + "w2v-salt-99").getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return "unknown";
        }
    }
}
