package com.voisetu.e2e.config;

/**
 * Central configuration holder for the E2E test suite.
 * All values are read from System properties (set via Maven -D flags or testng.xml).
 *
 * Default values target the local development setup:
 *   - Angular dev server on http://localhost:4200
 *   - Spring Boot backend on http://localhost:8080
 */
public final class TestConfig {

    private TestConfig() { /* utility class */ }

    /** Base URL of the Angular frontend (e.g. http://localhost:4200) */
    public static final String BASE_URL =
            System.getProperty("base.url", "http://localhost:4200");

    /** Base URL of the Spring Boot REST API (e.g. http://localhost:8080) */
    public static final String API_BASE_URL =
            System.getProperty("api.base.url", "http://localhost:8080");

    /** Browser to use: "chrome" | "firefox" | "edge" */
    public static final String BROWSER =
            System.getProperty("browser", "chrome");

    /** Whether to run the browser in headless mode */
    public static final boolean HEADLESS =
            Boolean.parseBoolean(System.getProperty("headless", "true"));

    // -------------------------------------------------------
    // Route paths (relative to BASE_URL)
    // -------------------------------------------------------
    public static final String PATH_LOGIN  = "/login";
    public static final String PATH_SIGNUP = "/signup";
    public static final String PATH_STUDIO = "/studio";

    // -------------------------------------------------------
    // API paths (relative to API_BASE_URL)
    // -------------------------------------------------------
    public static final String API_REGISTER = "/api/auth/register";
    public static final String API_LOGIN    = "/api/auth/login";
    public static final String API_ME       = "/api/me";

    // -------------------------------------------------------
    // Timeouts
    // -------------------------------------------------------
    /** Max seconds to wait for an element to appear */
    public static final int IMPLICIT_WAIT_SECONDS = 10;

    /** Max seconds to wait for a page load / redirect */
    public static final int PAGE_LOAD_TIMEOUT_SECONDS = 30;

    /** Explicit wait timeout in seconds */
    public static final int EXPLICIT_WAIT_SECONDS = 15;

    // -------------------------------------------------------
    // Default API test credentials (seeded via REST API in Background)
    // -------------------------------------------------------
    public static final String DEFAULT_API_PASSWORD = "ApiTest@99";
}
