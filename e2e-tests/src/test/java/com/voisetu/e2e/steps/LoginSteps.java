package com.voisetu.e2e.steps;

import com.voisetu.e2e.config.TestConfig;
import com.voisetu.e2e.pages.LoginPage;
import com.voisetu.e2e.pages.StudioPage;
import com.voisetu.e2e.utils.ApiHelper;
import com.voisetu.e2e.utils.WebDriverUtils;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step Definitions for the Login feature.
 *
 * Maps Gherkin steps in features/login.feature to browser interactions
 * using the LoginPage Page Object.
 */
public class LoginSteps {

    private final ScenarioContext ctx;

    public LoginSteps(ScenarioContext ctx) {
        this.ctx = ctx;
    }

    // ------------------------------------------------------------------
    // Background / Precondition steps
    // ------------------------------------------------------------------

    @Given("a registered user exists with email {string} and password {string}")
    public void aRegisteredUserExists(String email, String password) {
        // Seed via REST API — idempotent, safe to call every scenario
        ApiHelper.ensureUserExists("Login Test User", email, password);
    }

    @Given("the user navigates to the login page")
    public void theUserNavigatesToLoginPage() {
        ctx.getDriver().get(TestConfig.BASE_URL + TestConfig.PATH_LOGIN);
        assertThat(ctx.getLoginPage().isOnLoginPage())
                .as("Should be on /login")
                .isTrue();
    }

    @Given("the user is already logged in as {string} with password {string}")
    public void theUserIsAlreadyLoggedIn(String email, String password) {
        // Ensure user exists
        ApiHelper.ensureUserExists("Login Test User", email, password);
        // Get a JWT via the API
        String token = ApiHelper.loginAndGetToken(email, password);

        // Navigate to app first so localStorage is same-origin
        ctx.getDriver().get(TestConfig.BASE_URL + TestConfig.PATH_STUDIO);
        // Inject token into localStorage (same mechanism Angular uses)
        WebDriverUtils.setLocalStorageItem(ctx.getDriver(), "token", token);
        // Reload so Angular picks up the token signal
        ctx.getDriver().navigate().refresh();
        WebDriverUtils.waitForUrlContaining(ctx.getDriver(), TestConfig.PATH_STUDIO);
    }

    // ------------------------------------------------------------------
    // Form entry steps
    // ------------------------------------------------------------------

    @When("the user enters login email {string}")
    public void theUserEntersLoginEmail(String email) {
        ctx.getLoginPage().enterEmail(email);
    }

    @When("the user leaves the login email blank")
    public void theUserLeavesLoginEmailBlank() {
        ctx.getLoginPage().clearEmail();
    }

    @When("the user enters login password {string}")
    public void theUserEntersLoginPassword(String password) {
        ctx.getLoginPage().enterPassword(password);
    }

    @When("the user leaves the login password blank")
    public void theUserLeavesLoginPasswordBlank() {
        ctx.getLoginPage().clearPassword();
    }

    @When("the user clicks the login button")
    public void theUserClicksLoginButton() {
        ctx.getLoginPage().clickSubmit();
    }

    @When("the user clicks the {string} link on the login page")
    public void theUserClicksLinkOnLoginPage(String linkText) {
        if ("Create a free account".equals(linkText)) {
            ctx.getLoginPage().clickCreateAccountLink();
        }
    }

    @When("the user tries to navigate to the login page directly")
    public void theUserTriesToNavigateToLoginDirectly() {
        ctx.getDriver().get(TestConfig.BASE_URL + TestConfig.PATH_LOGIN);
    }

    @When("the user refreshes the page")
    public void theUserRefreshesThePage() {
        ctx.getDriver().navigate().refresh();
    }

    @When("the user logs out")
    public void theUserLogsOut() {
        ctx.getStudioPage().logout();
    }

    // ------------------------------------------------------------------
    // Assertion steps
    // ------------------------------------------------------------------

    @Then("the user should remain on the login page")
    public void theUserShouldRemainOnLoginPage() {
        assertThat(ctx.getLoginPage().isOnLoginPage())
                .as("User should remain on /login after validation failure")
                .isTrue();
    }

    @Then("the user should see the login field error {string}")
    public void theUserShouldSeeLoginFieldError(String errorText) {
        assertThat(ctx.getLoginPage().hasFieldError(errorText))
                .as("Expected field error: '" + errorText + "'")
                .isTrue();
    }

    // NOTE: "the user should see the error banner {string}" is in CommonSteps.

    @Then("the user should still be on the studio page")
    public void theUserShouldStillBeOnStudioPage() {
        assertThat(ctx.getStudioPage().isOnStudioPage())
                .as("User should still be on /studio after page refresh")
                .isTrue();
    }

    @Then("the JWT token should be removed from localStorage")
    public void theJwtTokenShouldBeRemovedFromLocalStorage() {
        WebDriverUtils.pause(500);
        String token = WebDriverUtils.getLocalStorageItem(ctx.getDriver(), "token");
        assertThat(token)
                .as("JWT token should be null/absent from localStorage after logout")
                .isNullOrEmpty();
    }

    // NOTE: "the user should be on the login page" and
    //       "the user should be on the signup page" are in CommonSteps.
}
