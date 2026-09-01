# ============================================================
# Feature: User Login
# ============================================================
# Tests cover the /login page of the Angular frontend
# which calls POST /api/auth/login on the Spring Boot backend.
#
# Form fields:
#   - #login-email       (email address)
#   - #login-password    (password)
#   - #btn-login-submit  (submit button)
#
# Success: redirects to /studio page with JWT in localStorage
# ============================================================

Feature: User Login

  Background:
    Given the browser is open and the application is running
    And a registered user exists with email "login_test@example.com" and password "Test@1234"
    And the user navigates to the login page

  # ----------------------------------------------------------
  # Happy Path
  # ----------------------------------------------------------

  @smoke @login @ui
  Scenario: Successful login with valid credentials
    When the user enters login email "login_test@example.com"
    And the user enters login password "Test@1234"
    And the user clicks the login button
    Then the user should be redirected to the studio page
    And a valid JWT token should be stored in localStorage

  @login @ui
  Scenario: Successful login and session persists on refresh
    When the user enters login email "login_test@example.com"
    And the user enters login password "Test@1234"
    And the user clicks the login button
    Then the user should be redirected to the studio page
    When the user refreshes the page
    Then the user should still be on the studio page

  # ----------------------------------------------------------
  # Client-side Validation Failures
  # ----------------------------------------------------------

  @login @validation @ui
  Scenario: Login fails when email field is empty
    When the user leaves the login email blank
    And the user enters login password "Test@1234"
    And the user clicks the login button
    Then the user should see the login field error "Email is required."
    And the user should remain on the login page

  @login @validation @ui
  Scenario: Login fails when password field is empty
    When the user enters login email "login_test@example.com"
    And the user leaves the login password blank
    And the user clicks the login button
    Then the user should see the login field error "Password is required."
    And the user should remain on the login page

  @login @validation @ui
  Scenario: Login fails when email format is invalid
    When the user enters login email "not-an-email"
    And the user enters login password "Test@1234"
    And the user clicks the login button
    Then the user should see the login field error "Please enter a valid email address."
    And the user should remain on the login page

  @login @validation @ui
  Scenario: Login fails when password is shorter than 6 characters
    When the user enters login email "login_test@example.com"
    And the user enters login password "abc"
    And the user clicks the login button
    Then the user should see the login field error "Password must be at least 6 characters."
    And the user should remain on the login page

  # ----------------------------------------------------------
  # Server-side / Business Rule Failures
  # ----------------------------------------------------------

  @login @server-error @ui
  Scenario: Login fails with wrong password
    When the user enters login email "login_test@example.com"
    And the user enters login password "WrongPass@99"
    And the user clicks the login button
    Then the user should see the error banner "Invalid email or password. Please try again."
    And the user should remain on the login page

  @login @server-error @ui
  Scenario: Login fails with a non-existent email
    When the user enters login email "nobody@notregistered.com"
    And the user enters login password "Test@1234"
    And the user clicks the login button
    Then the user should see the error banner "Invalid email or password. Please try again."
    And the user should remain on the login page

  # ----------------------------------------------------------
  # Navigation
  # ----------------------------------------------------------

  @login @navigation @ui
  Scenario: User can navigate from login page to signup page via the register link
    When the user clicks the "Create a free account" link on the login page
    Then the user should be on the signup page

  @login @navigation @ui
  Scenario: Already authenticated user is redirected away from login page
    Given the user is already logged in as "login_test@example.com" with password "Test@1234"
    When the user tries to navigate to the login page directly
    Then the user should be redirected to the studio page

  # ----------------------------------------------------------
  # Token Expiry / Logout
  # ----------------------------------------------------------

  @login @auth @ui
  Scenario: User can logout and JWT is cleared from localStorage
    Given the user is already logged in as "login_test@example.com" with password "Test@1234"
    When the user logs out
    Then the JWT token should be removed from localStorage
    And the user should be on the login page
