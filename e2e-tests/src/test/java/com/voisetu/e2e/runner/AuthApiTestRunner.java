package com.voisetu.e2e.runner;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;
import org.testng.annotations.DataProvider;

/**
 * TestNG runner for the Auth REST API feature file.
 *
 * Runs scenarios tagged @api which use REST Assured directly
 * against the Spring Boot backend — no browser required.
 *
 * Usage:
 *   mvn test -Dtest=AuthApiTestRunner
 *   mvn test -Dcucumber.filter.tags="@smoke and @api"
 */
@CucumberOptions(
        features = {
                "src/test/resources/features/auth_api.feature"
        },
        glue = {
                "com.voisetu.e2e.steps"
        },
        plugin = {
                "pretty",
                "html:target/cucumber-reports/auth-api-report.html",
                "json:target/cucumber-json/auth-api.json",
                "timeline:target/cucumber-reports/api-timeline"
        },
        tags = "@api",
        monochrome = false,
        publish = false
)
public class AuthApiTestRunner extends AbstractTestNGCucumberTests {

    @Override
    @DataProvider(parallel = false)
    public Object[][] scenarios() {
        return super.scenarios();
    }
}
