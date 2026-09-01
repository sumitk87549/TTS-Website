package com.voisetu.e2e.runner;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;
import org.testng.annotations.DataProvider;

/**
 * TestNG runner for the Register and Login UI feature files.
 *
 * This class ties together:
 *  - Cucumber's TestNG integration (AbstractTestNGCucumberTests)
 *  - Feature files in src/test/resources/features/
 *  - Glue code (step definitions + hooks) in com.voisetu.e2e.steps
 *  - Reporting plugins (pretty console + JSON for HTML report generation)
 *
 * Tags:
 *  - @smoke   → Critical path tests only
 *  - @ui      → Browser UI tests
 *  - @api     → REST API tests (excluded here — see AuthApiTestRunner)
 *  - Run: mvn test -Dcucumber.filter.tags="@smoke"
 */
@CucumberOptions(
        features = {
                "src/test/resources/features/register.feature",
                "src/test/resources/features/login.feature"
        },
        glue = {
                "com.voisetu.e2e.steps"
        },
        plugin = {
                "pretty",                                           // Colorised console output
                "html:target/cucumber-reports/auth-ui-report.html",
                "json:target/cucumber-json/auth-ui.json",          // For masterthought HTML report
                "timeline:target/cucumber-reports/ui-timeline"
        },
        tags = "not @api",          // UI tests only in this runner
        monochrome = false,          // Coloured output in terminals that support it
        publish = false              // Disable cucumber.io publishing
)
public class AuthUITestRunner extends AbstractTestNGCucumberTests {

    /**
     * Enables parallel scenario execution when parallel="methods" is set in testng.xml.
     * Currently disabled (single-threaded) to avoid port conflicts on a single machine.
     * To enable: set parallel = true and add parallel="methods" to testng.xml.
     */
    @Override
    @DataProvider(parallel = false)
    public Object[][] scenarios() {
        return super.scenarios();
    }
}
