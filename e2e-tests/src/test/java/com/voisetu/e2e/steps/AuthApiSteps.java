package com.voisetu.e2e.steps;

import com.voisetu.e2e.config.TestConfig;
import com.voisetu.e2e.utils.ApiHelper;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step Definitions for the Auth REST API feature (auth_api.feature).
 *
 * These tests use REST Assured directly — no browser/WebDriver involved.
 * They test the Spring Boot backend endpoints:
 *   POST /api/auth/register
 *   POST /api/auth/login
 *   GET  /api/me
 */
public class AuthApiSteps {

    private static final Logger log = LoggerFactory.getLogger(AuthApiSteps.class);

    private final ScenarioContext ctx;

    public AuthApiSteps(ScenarioContext ctx) {
        this.ctx = ctx;
    }

    // ------------------------------------------------------------------
    // Background
    // ------------------------------------------------------------------

    @Given("the backend API is available at the configured base URL")
    public void theBackendApiIsAvailable() {
        // REST Assured base URI is already set in ApiHelper's static init block
        log.info("API base URL: {}", TestConfig.API_BASE_URL);
        // A simple health check to fail fast if the server is down
        Response health = given()
                .baseUri(TestConfig.API_BASE_URL)
                .get("/actuator/health");
        assertThat(health.getStatusCode())
                .as("Backend health check — is Spring Boot running on %s?", TestConfig.API_BASE_URL)
                .isEqualTo(200);
    }

    // ------------------------------------------------------------------
    // Precondition
    // ------------------------------------------------------------------

    @Given("the user {string} is already registered via API")
    public void theUserIsAlreadyRegisteredViaApi(String email) {
        ApiHelper.ensureUserExists(email);
    }

    // ------------------------------------------------------------------
    // HTTP action steps
    // ------------------------------------------------------------------

    @When("a POST request is made to {string} with body:")
    public void aPostRequestIsMadeTo(String path, String jsonBody) {
        log.info("POST {} body: {}", path, jsonBody);
        Response response = given()
                .baseUri(TestConfig.API_BASE_URL)
                .contentType(JSON)
                .body(jsonBody)
                .when()
                .post(path);
        ctx.setLastApiResponse(response);
        log.info("Response: {} — {}", response.getStatusCode(), response.asString());
    }

    @When("a GET request is made to {string} without a token")
    public void aGetRequestWithoutToken(String path) {
        log.info("GET {} (no auth)", path);
        Response response = given()
                .baseUri(TestConfig.API_BASE_URL)
                .when()
                .get(path);
        ctx.setLastApiResponse(response);
    }

    @When("a GET request is made to {string} with a fake Bearer token")
    public void aGetRequestWithFakeToken(String path) {
        log.info("GET {} (fake Bearer token)", path);
        Response response = given()
                .baseUri(TestConfig.API_BASE_URL)
                .header("Authorization", "Bearer this.is.a.fake.token")
                .when()
                .get(path);
        ctx.setLastApiResponse(response);
    }

    /**
     * Uses the JWT from the last captured response to call a subsequent endpoint.
     * Supports "GET /api/me" pattern.
     */
    @When("the returned JWT is used to call {string}")
    public void theReturnedJwtIsUsedToCall(String methodAndPath) {
        // Parse "GET /api/me" → method + path
        String[] parts = methodAndPath.trim().split(" ", 2);
        String method = parts[0].toUpperCase();
        String path   = parts.length > 1 ? parts[1] : "/";

        // Extract token from last response
        Response last = ctx.getLastApiResponse();
        assertThat(last).as("A previous response should exist").isNotNull();
        String token = last.jsonPath().getString("token");
        assertThat(token).as("Last response should contain a JWT token").isNotBlank();
        ctx.setLastJwtToken(token);

        log.info("{} {} (with JWT from previous response)", method, path);
        Response response = given()
                .baseUri(TestConfig.API_BASE_URL)
                .header("Authorization", "Bearer " + token)
                .contentType(JSON)
                .when()
                .request(method, path);
        ctx.setLastApiResponse(response);
        log.info("Response: {} — {}", response.getStatusCode(), response.asString());
    }

    // ------------------------------------------------------------------
    // Assertion steps
    // ------------------------------------------------------------------

    @Then("the response status code should be {int}")
    public void theResponseStatusCodeShouldBe(int expectedStatus) {
        Response response = ctx.getLastApiResponse();
        assertThat(response).as("No API response captured").isNotNull();
        assertThat(response.getStatusCode())
                .as("Expected HTTP status %d but got %d. Body: %s",
                    expectedStatus, response.getStatusCode(), response.asString())
                .isEqualTo(expectedStatus);
    }

    @Then("the response body should contain a non-empty {string}")
    public void theResponseBodyShouldContainNonEmpty(String fieldName) {
        String value = ctx.getLastApiResponse().jsonPath().getString(fieldName);
        assertThat(value)
                .as("Response field '%s' should be non-empty", fieldName)
                .isNotNull()
                .isNotBlank();
    }

    @Then("the response body field {string} should equal {string}")
    public void theResponseBodyFieldShouldEqual(String fieldName, String expectedValue) {
        String actual = ctx.getLastApiResponse().jsonPath().getString(fieldName);
        assertThat(actual)
                .as("Response field '%s'", fieldName)
                .isEqualTo(expectedValue);
    }

    @Then("the response body field {string} should be a positive number")
    public void theResponseBodyFieldShouldBePositiveNumber(String fieldName) {
        Object raw = ctx.getLastApiResponse().jsonPath().get(fieldName);
        assertThat(raw).as("Field '%s' should not be null", fieldName).isNotNull();
        long value = Long.parseLong(raw.toString());
        assertThat(value)
                .as("Field '%s' should be a positive number", fieldName)
                .isGreaterThan(0);
    }

    /**
     * Verifies the JWT is a structurally valid 3-part base64url token.
     * Does NOT verify the signature (that's the backend's responsibility).
     */
    @Then("the JWT token should be a valid 3-part base64 structure")
    public void theJwtTokenShouldBeValidStructure() {
        String token = ctx.getLastApiResponse().jsonPath().getString("token");
        assertThat(token)
                .as("JWT should be a 3-segment base64url string (header.payload.signature)")
                .isNotNull()
                .matches("^[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+$");
    }
}
