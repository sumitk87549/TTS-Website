package com.voisetu.e2e.utils;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

import static com.voisetu.e2e.config.TestConfig.EXPLICIT_WAIT_SECONDS;

/**
 * Utility helpers for Selenium WebDriver interactions.
 *
 * Design notes:
 * - All waits use explicit WebDriverWait (not Thread.sleep) for reliability.
 * - JavaScript executor is used for localStorage access and scrolling.
 */
public final class WebDriverUtils {

    private static final Logger log = LoggerFactory.getLogger(WebDriverUtils.class);

    private WebDriverUtils() {}

    // ------------------------------------------------------------------
    // Waits
    // ------------------------------------------------------------------

    /**
     * Creates a new WebDriverWait with the configured explicit wait timeout.
     */
    public static WebDriverWait wait(WebDriver driver) {
        return new WebDriverWait(driver, Duration.ofSeconds(EXPLICIT_WAIT_SECONDS));
    }

    /**
     * Waits until the browser's current URL contains the given fragment.
     */
    public static void waitForUrlContaining(WebDriver driver, String urlFragment) {
        log.debug("Waiting for URL to contain: {}", urlFragment);
        wait(driver).until(ExpectedConditions.urlContains(urlFragment));
    }

    /**
     * Waits until the browser's current URL exactly matches the given URL.
     */
    public static void waitForUrlToBe(WebDriver driver, String fullUrl) {
        wait(driver).until(ExpectedConditions.urlToBe(fullUrl));
    }

    /**
     * Waits until an element is visible and clickable, then returns it.
     */
    public static WebElement waitForClickable(WebDriver driver, WebElement element) {
        return wait(driver).until(ExpectedConditions.elementToBeClickable(element));
    }

    /**
     * Waits until an element is visible.
     */
    public static WebElement waitForVisible(WebDriver driver, WebElement element) {
        return wait(driver).until(ExpectedConditions.visibilityOf(element));
    }

    // ------------------------------------------------------------------
    // localStorage helpers
    // ------------------------------------------------------------------

    /**
     * Reads a value from the browser's localStorage.
     *
     * @param driver the active WebDriver session
     * @param key    the localStorage key to read
     * @return the stored value, or {@code null} if the key does not exist
     */
    public static String getLocalStorageItem(WebDriver driver, String key) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        Object value = js.executeScript(
                "return window.localStorage.getItem(arguments[0]);", key);
        return value != null ? value.toString() : null;
    }

    /**
     * Removes a key from the browser's localStorage.
     */
    public static void removeLocalStorageItem(WebDriver driver, String key) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("window.localStorage.removeItem(arguments[0]);", key);
    }

    /**
     * Sets a value in the browser's localStorage and dispatches a storage event
     * so Angular's signal picks up the change immediately.
     */
    public static void setLocalStorageItem(WebDriver driver, String key, String value) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript(
            "window.localStorage.setItem(arguments[0], arguments[1]);" +
            "window.dispatchEvent(new StorageEvent('storage', {key: arguments[0]}));",
            key, value);
    }

    /**
     * Clears all localStorage items.
     */
    public static void clearLocalStorage(WebDriver driver) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("window.localStorage.clear();");
    }

    // ------------------------------------------------------------------
    // Misc
    // ------------------------------------------------------------------

    /**
     * Scrolls the given element into the viewport.
     */
    public static void scrollIntoView(WebDriver driver, WebElement element) {
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block:'center'});", element);
    }

    /**
     * Pauses execution for a fixed duration (use sparingly — prefer explicit waits).
     */
    public static void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
