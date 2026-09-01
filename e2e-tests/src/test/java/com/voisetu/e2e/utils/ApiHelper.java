package com.voisetu.e2e.utils;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static com.voisetu.e2e.config.TestConfig.API_BASE_URL;
import static com.voisetu.e2e.config.TestConfig.API_LOGIN;
import static com.voisetu.e2e.config.TestConfig.API_REGISTER;
import static com.voisetu.e2e.config.TestConfig.DEFAULT_API_PASSWORD;
import static io.restassured.RestAssured.given;

/**
 * REST API helper utility.
 *
 * Provides convenience methods for:
 * - Seeding test users via the /api/auth/register endpoint
 * - Logging in via /api/auth/login and extracting the JWT
 * - Building reusable REST Assured request specifications
 */
public final class ApiHelper {

    private static final Logger log = LoggerFactory.getLogger(ApiHelper.class);

    static {
        RestAssured.baseURI = API_BASE_URL;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    private ApiHelper() {}

    // ------------------------------------------------------------------
    // User seeding
    // ------------------------------------------------------------------

    /**
     * Ensures a test user exists. If the email is already registered (400 EMAIL_ALREADY_EXISTS),
     * the method silently succeeds — making it safe to call from @Before hooks.
     *
     * @param displayName user's display name
     * @param email       user's email
     * @param password    user's raw password
     * @return the JWT token returned from register (or null if user already existed)
     */
    public static String ensureUserExists(String displayName, String email, String password) {
        Map<String, Object> body = new HashMap<>();
        body.put("displayName", displayName);
        body.put("email", email);
        body.put("password", password);

        Response response = given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post(API_REGISTER);

        int status = response.getStatusCode();
        if (status == 200) {
            log.info("User seeded successfully: {}", email);
            return response.jsonPath().getString("token");
        } else if (status == 400) {
            String code = response.jsonPath().getString("code");
            if ("EMAIL_ALREADY_EXISTS".equals(code)) {
                log.info("User already exists, continuing: {}", email);
                return null;
            }
        }
        log.warn("Unexpected status {} when seeding user {}: {}", status, email, response.asString());
        return null;
    }

    /**
     * Convenience overload — uses the default API test password.
     */
    public static String ensureUserExists(String email) {
        // Use email prefix as display name
        String displayName = email.split("@")[0].replace("_", " ");
        return ensureUserExists(displayName, email, DEFAULT_API_PASSWORD);
    }

    // ------------------------------------------------------------------
    // Authentication
    // ------------------------------------------------------------------

    /**
     * Logs in via the REST API and returns the raw JWT string.
     *
     * @throws AssertionError if login returns non-200
     */
    public static String loginAndGetToken(String email, String password) {
        Map<String, Object> body = new HashMap<>();
        body.put("email", email);
        body.put("password", password);

        Response response = given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post(API_LOGIN)
                .then()
                .statusCode(200)
                .extract()
                .response();

        return response.jsonPath().getString("token");
    }

    // ------------------------------------------------------------------
    // Request specification builder
    // ------------------------------------------------------------------

    /**
     * Returns a REST Assured {@link RequestSpecification} with the JWT
     * attached as a Bearer Authorization header.
     */
    public static RequestSpecification authenticatedRequest(String token) {
        return given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token);
    }

    /**
     * Returns a base REST Assured request specification (no auth).
     */
    public static RequestSpecification unauthenticatedRequest() {
        return given().contentType(ContentType.JSON);
    }
}
