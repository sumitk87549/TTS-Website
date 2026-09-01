package com.voisetu.e2e.steps;

import com.voisetu.e2e.config.DriverFactory;
import com.voisetu.e2e.pages.LoginPage;
import com.voisetu.e2e.pages.SignupPage;
import com.voisetu.e2e.pages.StudioPage;
import io.restassured.response.Response;
import org.openqa.selenium.WebDriver;

/**
 * Shared scenario state injected via Cucumber PicoContainer.
 *
 * One instance per Cucumber scenario — fields are reset for each scenario.
 * This replaces the need for static ThreadLocal state and makes scenarios
 * safe to run sequentially.
 *
 * All Step Definition classes declare this as a constructor parameter,
 * which Cucumber PicoContainer resolves automatically.
 */
public class ScenarioContext {

    // ------------------------------------------------------------------
    // WebDriver (browser session)
    // ------------------------------------------------------------------

    /** Live WebDriver instance — created by CommonSteps.beforeScenario() */
    private WebDriver driver;

    // ------------------------------------------------------------------
    // Page Objects (created lazily by step definitions)
    // ------------------------------------------------------------------

    private SignupPage signupPage;
    private LoginPage  loginPage;
    private StudioPage studioPage;

    // ------------------------------------------------------------------
    // API state (used by API step definitions)
    // ------------------------------------------------------------------

    /** Last REST Assured response captured by an API step */
    private Response lastApiResponse;

    /** JWT token extracted from the last successful login/register API call */
    private String lastJwtToken;

    // ------------------------------------------------------------------
    // Driver
    // ------------------------------------------------------------------

    public WebDriver getDriver() {
        return driver;
    }

    public void setDriver(WebDriver driver) {
        this.driver = driver;
    }

    // ------------------------------------------------------------------
    // Page Objects
    // ------------------------------------------------------------------

    public SignupPage getSignupPage() {
        if (signupPage == null) signupPage = new SignupPage(driver);
        return signupPage;
    }

    public LoginPage getLoginPage() {
        if (loginPage == null) loginPage = new LoginPage(driver);
        return loginPage;
    }

    public StudioPage getStudioPage() {
        if (studioPage == null) studioPage = new StudioPage(driver);
        return studioPage;
    }

    // ------------------------------------------------------------------
    // API State
    // ------------------------------------------------------------------

    public Response getLastApiResponse() {
        return lastApiResponse;
    }

    public void setLastApiResponse(Response response) {
        this.lastApiResponse = response;
    }

    public String getLastJwtToken() {
        return lastJwtToken;
    }

    public void setLastJwtToken(String token) {
        this.lastJwtToken = token;
    }

    // ------------------------------------------------------------------
    // Teardown
    // ------------------------------------------------------------------

    /** Called in @After to quit the browser session. */
    public void tearDown() {
        if (driver != null) {
            try {
                driver.quit();
            } catch (Exception ignored) {}
            driver = null;
        }
        signupPage = null;
        loginPage = null;
        studioPage = null;
        lastApiResponse = null;
        lastJwtToken = null;
    }

    /** Initialises a new WebDriver and creates initial page objects. */
    public void setUp() {
        driver = DriverFactory.createDriver();
        signupPage = new SignupPage(driver);
        loginPage = new LoginPage(driver);
        studioPage = new StudioPage(driver);
    }
}
