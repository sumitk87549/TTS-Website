# ============================================================
# Feature: User Registration (Sign Up)
# ============================================================
# Tests cover the /signup page of the Angular frontend
# which calls POST /api/auth/register on the Spring Boot backend.
#
# Form fields:
#   - #signup-name       (display name)
#   - #signup-email      (email address)
#   - #signup-password   (password)
#   - #btn-signup-submit (submit button)
#
# Success: redirects to /studio page
# ============================================================

Feature: User Registration

  Background:
    Given the browser is open and the application is running
    And the user navigates to the signup page

  # ----------------------------------------------------------
  # Happy Path
  # ----------------------------------------------------------

  @smoke @register @ui
  Scenario: Successful user registration with valid data
    When the user enters display name "Test User"
    And the user enters email "testuser_unique@example.com"
    And the user enters password "Test@1234"
    And the user clicks the register button
    Then the user should be redirected to the studio page
    And a valid JWT token should be stored in localStorage

  @register @ui
  Scenario Outline: Successful registration with various valid names
    When the user enters display name "<displayName>"
    And the user enters email "<email>"
    And the user enters password "Secure@99"
    And the user clicks the register button
    Then the user should be redirected to the studio page

    Examples:
      | displayName    | email                          |
      | Aarav          | aarav_test@words2voice.test    |
      | Priya Sharma   | priya_test@words2voice.test    |
      | Jo             | jo_test@words2voice.test       |

  # ----------------------------------------------------------
  # Client-side Validation Failures (no API call should be made)
  # ----------------------------------------------------------

  @register @validation @ui
  Scenario: Registration fails when display name is empty
    When the user leaves display name blank
    And the user enters email "test@example.com"
    And the user enters password "Test@1234"
    And the user clicks the register button
    Then the user should see the field error "Name is required."
    And the user should remain on the signup page

  @register @validation @ui
  Scenario: Registration fails when display name is too short (1 character)
    When the user enters display name "A"
    And the user enters email "test@example.com"
    And the user enters password "Test@1234"
    And the user clicks the register button
    Then the user should see the field error "Name must be at least 2 characters."
    And the user should remain on the signup page

  @register @validation @ui
  Scenario: Registration fails when email is empty
    When the user enters display name "Test User"
    And the user leaves email blank
    And the user enters password "Test@1234"
    And the user clicks the register button
    Then the user should see the field error "Email is required."
    And the user should remain on the signup page

  @register @validation @ui
  Scenario: Registration fails when email format is invalid
    When the user enters display name "Test User"
    And the user enters email "not-an-email"
    And the user enters password "Test@1234"
    And the user clicks the register button
    Then the user should see the field error "Please enter a valid email address."
    And the user should remain on the signup page

  @register @validation @ui
  Scenario: Registration fails when password is empty
    When the user enters display name "Test User"
    And the user enters email "test@example.com"
    And the user leaves password blank
    And the user clicks the register button
    Then the user should see the field error "Password is required."
    And the user should remain on the signup page

  @register @validation @ui
  Scenario: Registration fails when password is too short
    When the user enters display name "Test User"
    And the user enters email "test@example.com"
    And the user enters password "Sh0rt"
    And the user clicks the register button
    Then the password strength hint for "8+ characters" should not be satisfied
    And the user should remain on the signup page

  @register @validation @ui
  Scenario: Registration fails when password has no uppercase letter
    When the user enters display name "Test User"
    And the user enters email "test@example.com"
    And the user enters password "nouppercase1"
    And the user clicks the register button
    Then the password strength hint for "1 uppercase letter" should not be satisfied
    And the user should remain on the signup page

  @register @validation @ui
  Scenario: Registration fails when password has no digit
    When the user enters display name "Test User"
    And the user enters email "test@example.com"
    And the user enters password "NoDigitHere"
    And the user clicks the register button
    Then the password strength hint for "1 number" should not be satisfied
    And the user should remain on the signup page

  # ----------------------------------------------------------
  # Server-side / Business Rule Failures
  # ----------------------------------------------------------

  @register @server-error @ui
  Scenario: Registration fails when email already exists
    Given a user is already registered with email "existing_user@example.com"
    When the user enters display name "Another User"
    And the user enters email "existing_user@example.com"
    And the user enters password "Test@1234"
    And the user clicks the register button
    Then the user should see the error banner "An account with this email already exists. Try logging in instead."
    And the user should remain on the signup page

  # ----------------------------------------------------------
  # Navigation
  # ----------------------------------------------------------

  @register @navigation @ui
  Scenario: User can navigate from signup page to login page
    When the user clicks the "Log in" link on the signup page
    Then the user should be on the login page

  # ----------------------------------------------------------
  # Password Strength Progressive Hints
  # ----------------------------------------------------------

  @register @ui
  Scenario: Password strength hints appear on a weak password
    When the user enters display name "Test User"
    And the user enters email "test@example.com"
    And the user enters a weak password "abc" and touches the field
    Then the password hints panel should be visible
    And the password strength hint for "8+ characters" should not be satisfied
    And the password strength hint for "1 uppercase letter" should not be satisfied
    And the password strength hint for "1 number" should not be satisfied

  @register @ui
  Scenario: Password strength hints turn green on a strong password
    When the user enters display name "Test User"
    And the user enters email "test@example.com"
    And the user enters a strong password "Strong@99" and touches the field
    Then the password strength hint for "8+ characters" should be satisfied
    And the password strength hint for "1 uppercase letter" should be satisfied
    And the password strength hint for "1 number" should be satisfied
