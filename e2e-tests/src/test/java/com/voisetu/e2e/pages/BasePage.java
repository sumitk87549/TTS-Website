package com.voisetu.e2e.pages;

import com.voisetu.e2e.utils.WebDriverUtils;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base Page Object.
 * Initialises PageFactory elements and provides common navigation helpers.
 */
public abstract class BasePage {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final WebDriver driver;

    protected BasePage(WebDriver driver) {
        this.driver = driver;
        PageFactory.initElements(driver, this);
    }

    /** Checks whether an element is present and displayed without throwing. */
    protected boolean isDisplayed(WebElement element) {
        try {
            return element.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /** Returns the browser's current URL. */
    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    /** Waits for the URL to contain the given path fragment. */
    protected void waitForNavigationTo(String urlFragment) {
        WebDriverUtils.waitForUrlContaining(driver, urlFragment);
    }

    /**
     * Slowly types into an element (clears first), which is safer for
     * Angular reactive-forms that listen on each keystroke.
     */
    protected void type(WebElement element, String text) {
        element.clear();
        element.sendKeys(text);
    }

    /** Reads a value from the browser's localStorage. */
    public String getLocalStorageItem(String key) {
        return WebDriverUtils.getLocalStorageItem(driver, key);
    }
}
