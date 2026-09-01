package com.voisetu.e2e.config;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * Factory that creates and configures a Selenium WebDriver instance.
 *
 * Supported browsers: chrome (default), firefox, edge.
 * WebDriverManager handles driver binary download automatically.
 */
public final class DriverFactory {

    private static final Logger log = LoggerFactory.getLogger(DriverFactory.class);

    private DriverFactory() {}

    /**
     * Creates a new WebDriver instance for the browser specified in {@link TestConfig#BROWSER}.
     *
     * @return configured and ready {@link WebDriver}
     */
    public static WebDriver createDriver() {
        boolean headless = TestConfig.HEADLESS;
        String browser = TestConfig.BROWSER.toLowerCase().trim();
        log.info("Creating WebDriver: browser={}, headless={}", browser, headless);

        WebDriver driver = switch (browser) {
            case "firefox" -> createFirefoxDriver(headless);
            case "edge"    -> createEdgeDriver(headless);
            default        -> createChromeDriver(headless);   // "chrome" or any unknown
        };

        driver.manage().timeouts()
              .implicitlyWait(Duration.ofSeconds(TestConfig.IMPLICIT_WAIT_SECONDS))
              .pageLoadTimeout(Duration.ofSeconds(TestConfig.PAGE_LOAD_TIMEOUT_SECONDS));
        driver.manage().window().maximize();

        return driver;
    }

    // ------------------------------------------------------------------
    // Private helpers
    // ------------------------------------------------------------------

    private static WebDriver createChromeDriver(boolean headless) {
        WebDriverManager.chromedriver().setup();
        ChromeOptions opts = new ChromeOptions();
        if (headless) {
            opts.addArguments("--headless=new");   // Chrome 112+ headless mode
        }
        opts.addArguments(
            "--no-sandbox",
            "--disable-dev-shm-usage",
            "--disable-gpu",
            "--window-size=1920,1080",
            "--disable-extensions",
            "--remote-allow-origins=*"
        );
        return new ChromeDriver(opts);
    }

    private static WebDriver createFirefoxDriver(boolean headless) {
        WebDriverManager.firefoxdriver().setup();
        FirefoxOptions opts = new FirefoxOptions();
        if (headless) {
            opts.addArguments("-headless");
        }
        return new FirefoxDriver(opts);
    }

    private static WebDriver createEdgeDriver(boolean headless) {
        WebDriverManager.edgedriver().setup();
        EdgeOptions opts = new EdgeOptions();
        if (headless) {
            opts.addArguments("--headless=new");
        }
        return new EdgeDriver(opts);
    }
}
