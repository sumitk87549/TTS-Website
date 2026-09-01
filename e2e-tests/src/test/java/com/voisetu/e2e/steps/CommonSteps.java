package com.voisetu.e2e.steps;

import com.voisetu.e2e.config.TestConfig;
import com.voisetu.e2e.utils.WebDriverUtils;
import io.cucumber.java.en.Then;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CommonSteps — step definitions that are shared across both the Register
 * and the Login feature files.
 *
 * Moving them here avoids the Cucumber DuplicateStepDefinition error that
 * occurs when the same annotation text appears in more than one glue class.
 *
 * Shared steps:
 *   - "the user should see the error banner {string}"
 *   - "the user should be on the login page"
 *   - "the user should be on the signup page"
 */
public class CommonSteps {

    private final ScenarioContext ctx;

    public CommonSteps(ScenarioContext ctx) {
        this.ctx = ctx;
    }

    // ------------------------------------------------------------------
    // Error banners (used in both register.feature and login.feature)
    // ------------------------------------------------------------------

    /**
     * Checks the server-side error banner on whichever page is currently
     * displayed (login or signup), decided by inspecting the current URL.
     */
    @Then("the user should see the error banner {string}")
    public void theUserShouldSeeErrorBanner(String bannerText) {
        String url = ctx.getDriver().getCurrentUrl();
        boolean found;
        if (url.contains("/login")) {
            found = ctx.getLoginPage().hasErrorBanner(bannerText);
        } else {
            // /signup or any other page
            found = ctx.getSignupPage().hasErrorBanner(bannerText);
        }
        assertThat(found)
                .as("Expected error banner containing: '%s' on page %s", bannerText, url)
                .isTrue();
    }

    // ------------------------------------------------------------------
    // Navigation assertions (used in both features)
    // ------------------------------------------------------------------

    @Then("the user should be on the login page")
    public void theUserShouldBeOnLoginPage() {
        WebDriverUtils.waitForUrlContaining(ctx.getDriver(), TestConfig.PATH_LOGIN);
        assertThat(ctx.getDriver().getCurrentUrl())
                .as("Expected browser to be on /login")
                .contains(TestConfig.PATH_LOGIN);
    }

    @Then("the user should be on the signup page")
    public void theUserShouldBeOnSignupPage() {
        WebDriverUtils.waitForUrlContaining(ctx.getDriver(), TestConfig.PATH_SIGNUP);
        assertThat(ctx.getDriver().getCurrentUrl())
                .as("Expected browser to be on /signup")
                .contains(TestConfig.PATH_SIGNUP);
    }
}
