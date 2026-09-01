package com.voisetu.e2e.steps;

import com.voisetu.e2e.config.TestConfig;
import com.voisetu.e2e.pages.LoginPage;
import com.voisetu.e2e.pages.SignupPage;
import com.voisetu.e2e.utils.ApiHelper;
import com.voisetu.e2e.utils.WebDriverUtils;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.openqa.selenium.WebDriver;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step Definitions for the Register (Signup) feature.
 *
 * Maps Gherkin steps in features/register.feature to browser interactions
 * using the SignupPage Page Object.
 */
public class RegisterSteps {

    private final ScenarioContext ctx;

    public RegisterSteps(ScenarioContext ctx) {
        this.ctx = ctx;
    }

    // ------------------------------------------------------------------
    // Background steps
    // ------------------------------------------------------------------

    @Given("the browser is open and the application is running")
    public void theBrowserIsOpenAndRunning() {
        // Browser was started by Hooks.beforeUiScenario().
        // This step is a no-op but documents the precondition.
        assertThat(ctx.getDriver())
                .as("WebDriver should be initialised by Hooks")
                .isNotNull();
    }

    @Given("the user navigates to the signup page")
    public void theUserNavigatesToSignupPage() {
        ctx.getDriver().get(TestConfig.BASE_URL + TestConfig.PATH_SIGNUP);
        assertThat(ctx.getSignupPage().isOnSignupPage())
                .as("Should be on /signup")
                .isTrue();
    }

    // ------------------------------------------------------------------
    // Form entry steps
    // ------------------------------------------------------------------

    @When("the user enters display name {string}")
    public void theUserEntersDisplayName(String name) {
        ctx.getSignupPage().enterDisplayName(name);
    }

    @When("the user leaves display name blank")
    public void theUserLeavesDisplayNameBlank() {
        ctx.getSignupPage().clearDisplayName();
    }

    @When("the user enters email {string}")
    public void theUserEntersEmail(String email) {
        ctx.getSignupPage().enterEmail(email);
    }

    @When("the user leaves email blank")
    public void theUserLeavesEmailBlank() {
        ctx.getSignupPage().clearEmail();
    }

    @When("the user enters password {string}")
    public void theUserEntersPassword(String password) {
        ctx.getSignupPage().enterPassword(password);
    }

    @When("the user leaves password blank")
    public void theUserLeavesPasswordBlank() {
        ctx.getSignupPage().clearPassword();
    }

    @When("the user enters a weak password {string} and touches the field")
    public void theUserEntersWeakPasswordAndTouches(String password) {
        ctx.getSignupPage()
           .enterPassword(password)
           .touchPasswordField();
    }

    @When("the user enters a strong password {string} and touches the field")
    public void theUserEntersStrongPasswordAndTouches(String password) {
        ctx.getSignupPage()
           .enterPassword(password)
           .touchPasswordField();
    }

    @When("the user clicks the register button")
    public void theUserClicksRegisterButton() {
        ctx.getSignupPage().clickSubmit();
    }

    @When("the user clicks the {string} link on the signup page")
    public void theUserClicksLinkOnSignupPage(String linkText) {
        // Currently only "Log in" is supported
        if ("Log in".equals(linkText)) {
            ctx.getSignupPage().clickLoginLink();
        }
    }

    // ------------------------------------------------------------------
    // Precondition steps
    // ------------------------------------------------------------------

    @Given("a user is already registered with email {string}")
    public void aUserIsAlreadyRegisteredWithEmail(String email) {
        ApiHelper.ensureUserExists("Existing User", email, "Test@1234");
    }

    // ------------------------------------------------------------------
    // Assertion steps
    // ------------------------------------------------------------------

    @Then("the user should be redirected to the studio page")
    public void theUserShouldBeRedirectedToStudio() {
        WebDriverUtils.waitForUrlContaining(ctx.getDriver(), TestConfig.PATH_STUDIO);
        assertThat(ctx.getStudioPage().isOnStudioPage())
                .as("After successful registration/login, should be on /studio")
                .isTrue();
    }

    @Then("a valid JWT token should be stored in localStorage")
    public void aValidJwtShouldBeInLocalStorage() {
        // Give Angular a moment to write the token
        WebDriverUtils.pause(500);
        String token = WebDriverUtils.getLocalStorageItem(ctx.getDriver(), "token");
        assertThat(token)
                .as("JWT token should be stored in localStorage key 'token' and be non-blank")
                .isNotNull()
                .isNotBlank();
        assertThat(java.util.regex.Pattern.matches(
                        "^[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+$", token))
                .as("Token '%s' should be a valid 3-segment JWT", token)
                .isTrue();
    }

    @Then("the user should see the field error {string}")
    public void theUserShouldSeeFieldError(String errorText) {
        assertThat(ctx.getSignupPage().hasFieldError(errorText))
                .as("Expected field error: '" + errorText + "'")
                .isTrue();
    }

    @Then("the user should remain on the signup page")
    public void theUserShouldRemainOnSignupPage() {
        assertThat(ctx.getSignupPage().isOnSignupPage())
                .as("User should remain on /signup after validation failure")
                .isTrue();
    }

    // NOTE: "the user should see the error banner {string}",
    //       "the user should be on the login page",  and
    //       "the user should be on the signup page"
    // are defined in CommonSteps to avoid DuplicateStepDefinition errors.

    @Then("the password hints panel should be visible")
    public void thePasswordHintsPanelShouldBeVisible() {
        assertThat(ctx.getSignupPage().isPasswordHintsPanelVisible())
                .as("Password hints panel should be visible after touching field")
                .isTrue();
    }

    @Then("the password strength hint for {string} should not be satisfied")
    public void thePasswordStrengthHintShouldNotBeSatisfied(String hintLabel) {
        assertThat(ctx.getSignupPage().isPasswordHintNotSatisfied(hintLabel))
                .as("Password hint '" + hintLabel + "' should NOT be satisfied (green)")
                .isTrue();
    }

    @Then("the password strength hint for {string} should be satisfied")
    public void thePasswordStrengthHintShouldBeSatisfied(String hintLabel) {
        assertThat(ctx.getSignupPage().isPasswordHintSatisfied(hintLabel))
                .as("Password hint '" + hintLabel + "' should be satisfied (green)")
                .isTrue();
    }
}
