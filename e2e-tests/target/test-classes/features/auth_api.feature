# ============================================================
# Feature: Auth REST API — Register & Login Endpoints
# ============================================================
# Direct HTTP contract tests against the Spring Boot backend.
# No browser required; uses REST Assured.
#
# Endpoints:
#   POST /api/auth/register
#   POST /api/auth/login
# ============================================================

Feature: Auth REST API Contracts

  Background:
    Given the backend API is available at the configured base URL

  # ----------------------------------------------------------
  # POST /api/auth/register — Happy Path
  # ----------------------------------------------------------

  @smoke @api @register
  Scenario: API register endpoint returns 200 with JWT on valid request
    When a POST request is made to "/api/auth/register" with body:
      """
      {
        "displayName": "API Test User",
        "email":       "api_register_ok@test.com",
        "password":    "ApiTest@99"
      }
      """
    Then the response status code should be 200
    And the response body should contain a non-empty "token"
    And the response body field "displayName" should equal "API Test User"
    And the response body field "isAdmin" should equal "false"
    And the response body field "userId" should be a positive number

  @api @register
  Scenario: API register endpoint is idempotent when email already registered
    Given the user "api_dup@test.com" is already registered via API
    When a POST request is made to "/api/auth/register" with body:
      """
      {
        "displayName": "Dup User",
        "email":       "api_dup@test.com",
        "password":    "ApiTest@99"
      }
      """
    Then the response status code should be 400
    And the response body field "code" should equal "EMAIL_ALREADY_EXISTS"

  # ----------------------------------------------------------
  # POST /api/auth/register — Validation Failures (400)
  # ----------------------------------------------------------

  @api @register @validation
  Scenario Outline: API register returns 400 for invalid input
    When a POST request is made to "/api/auth/register" with body:
      """
      {
        "displayName": "<displayName>",
        "email":       "<email>",
        "password":    "<password>"
      }
      """
    Then the response status code should be 400
    And the response body field "code" should equal "VALIDATION_ERROR"

    Examples:
      | displayName | email              | password    | reason                        |
      |             | valid@test.com     | Test@1234   | Missing display name          |
      | A           | valid@test.com     | Test@1234   | Display name too short (1ch)  |
      | Valid Name  |                    | Test@1234   | Missing email                 |
      | Valid Name  | not-an-email       | Test@1234   | Invalid email format          |
      | Valid Name  | valid@test.com     |             | Missing password              |
      | Valid Name  | valid@test.com     | Short1      | Password too short (< 8 chars)|

  # ----------------------------------------------------------
  # POST /api/auth/login — Happy Path
  # ----------------------------------------------------------

  @smoke @api @login
  Scenario: API login endpoint returns 200 with JWT on valid credentials
    Given the user "api_login_ok@test.com" is already registered via API
    When a POST request is made to "/api/auth/login" with body:
      """
      {
        "email":    "api_login_ok@test.com",
        "password": "ApiTest@99"
      }
      """
    Then the response status code should be 200
    And the response body should contain a non-empty "token"
    And the JWT token should be a valid 3-part base64 structure

  # ----------------------------------------------------------
  # POST /api/auth/login — Failure Cases (401 / 400)
  # ----------------------------------------------------------

  @api @login @validation
  Scenario: API login returns 401 for wrong password
    Given the user "api_wrongpw@test.com" is already registered via API
    When a POST request is made to "/api/auth/login" with body:
      """
      {
        "email":    "api_wrongpw@test.com",
        "password": "WrongPassword@1"
      }
      """
    Then the response status code should be 401
    And the response body field "code" should equal "INVALID_CREDENTIALS"

  @api @login @validation
  Scenario: API login returns 401 for non-existent email
    When a POST request is made to "/api/auth/login" with body:
      """
      {
        "email":    "nobody@notregistered.com",
        "password": "Test@1234"
      }
      """
    Then the response status code should be 401
    And the response body field "code" should equal "INVALID_CREDENTIALS"

  @api @login @validation
  Scenario Outline: API login returns 400 for missing fields
    When a POST request is made to "/api/auth/login" with body:
      """
      {
        "email":    "<email>",
        "password": "<password>"
      }
      """
    Then the response status code should be 400
    And the response body field "code" should equal "VALIDATION_ERROR"

    Examples:
      | email              | password  | reason            |
      |                    | Test@1234 | Missing email     |
      | valid@test.com     |           | Missing password  |
      | not-an-email       | Test@1234 | Invalid email fmt |

  # ----------------------------------------------------------
  # JWT Token Quality
  # ----------------------------------------------------------

  @api @login @token
  Scenario: Registered JWT token grants access to protected endpoint
    Given the user "api_token_check@test.com" is already registered via API
    When a POST request is made to "/api/auth/login" with body:
      """
      {
        "email":    "api_token_check@test.com",
        "password": "ApiTest@99"
      }
      """
    Then the response status code should be 200
    When the returned JWT is used to call "GET /api/me"
    Then the response status code should be 200
    And the response body field "email" should equal "api_token_check@test.com"

  @api @auth @token
  Scenario: Accessing a protected endpoint without a token returns 401
    When a GET request is made to "/api/me" without a token
    Then the response status code should be 401

  @api @auth @token
  Scenario: Accessing a protected endpoint with a fake token returns 401
    When a GET request is made to "/api/me" with a fake Bearer token
    Then the response status code should be 401
