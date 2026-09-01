package com.voisetu.e2e.steps;

import com.voisetu.e2e.config.TestConfig;
import com.voisetu.e2e.utils.WebDriverUtils;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cucumber lifecycle hooks (@Before / @After).
 *
 * Responsibilities:
 * - @Before: initialise WebDriver for UI scenarios; skip for @api-only scenarios.
 * - @After:  quit the driver, attach screenshots on failure.
 *
 * Uses tag filtering to skip browser setup for pure API tests (tagged @api).
 */
public class Hooks {

    private static final Logger log = LoggerFactory.getLogger(Hooks.class);
    private final ScenarioContext ctx;

    public Hooks(ScenarioContext ctx) {
        this.ctx = ctx;
    }

    // ------------------------------------------------------------------
    // Before — UI scenarios (all tags EXCEPT @api)
    // ------------------------------------------------------------------

    /**
     * Starts a browser session for every scenario that is NOT tagged @api.
     * Order 1 → runs before other @Before hooks.
     */
    @Before(order = 1, value = "not @api")
    public void beforeUiScenario(Scenario scenario) {
        log.info("=== STARTING UI SCENARIO: {} ===", scenario.getName());
        ctx.setUp();
    }

    // ------------------------------------------------------------------
    // After — UI scenarios (all tags EXCEPT @api)
    // ------------------------------------------------------------------

    /**
     * Screenshots on failure and driver teardown.
     * Order 100 → runs last so other @After hooks can still use the driver.
     */
    @After(order = 100, value = "not @api")
    public void afterUiScenario(Scenario scenario) {
        WebDriver driver = ctx.getDriver();
        if (driver != null && scenario.isFailed()) {
            log.warn("Scenario FAILED: {} — capturing screenshot.", scenario.getName());
            try {
                byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
                scenario.attach(screenshot, "image/png", "failure-screenshot");
            } catch (Exception e) {
                log.error("Failed to capture screenshot", e);
            }
        }
        log.info("=== FINISHED UI SCENARIO: {} — {} ===",
                scenario.getName(), scenario.getStatus());
        ctx.tearDown();
    }

    // ------------------------------------------------------------------
    // Before — API scenarios (@api tag)
    // ------------------------------------------------------------------

    @Before(order = 1, value = "@api")
    public void beforeApiScenario(Scenario scenario) {
        log.info("=== STARTING API SCENARIO: {} ===", scenario.getName());
        // No browser needed for API tests
    }

    @After(order = 100, value = "@api")
    public void afterApiScenario(Scenario scenario) {
        log.info("=== FINISHED API SCENARIO: {} — {} ===",
                scenario.getName(), scenario.getStatus());
    }
}
