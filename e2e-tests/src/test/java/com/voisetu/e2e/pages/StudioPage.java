package com.voisetu.e2e.pages;

import com.voisetu.e2e.utils.WebDriverUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Page Object for the Angular Studio page (/studio).
 *
 * The Studio page is the post-login landing page and also the public TTS workspace.
 * For auth tests, we only need to confirm successful navigation here.
 */
public class StudioPage extends BasePage {

    public StudioPage(WebDriver driver) {
        super(driver);
    }

    /** Returns true if the browser is currently on /studio. */
    public boolean isOnStudioPage() {
        return driver.getCurrentUrl().contains("/studio");
    }

    /**
     * Attempts to find and click the "Logout" or account menu button.
     * Implementation searches for common logout triggers in the dashboard sidebar.
     *
     * @return the LoginPage after logout redirects there
     */
    public LoginPage logout() {
        // Try to find a logout button by common selectors
        WebElement logoutBtn = null;
        String[] selectors = {
            "[data-testid='btn-logout']",
            ".btn-logout",
            "button[aria-label='Logout']",
            "button[aria-label='Log out']"
        };
        for (String selector : selectors) {
            try {
                logoutBtn = driver.findElement(By.cssSelector(selector));
                break;
            } catch (NoSuchElementException ignored) {
                // try next
            }
        }

        if (logoutBtn != null) {
            WebDriverUtils.waitForClickable(driver, logoutBtn);
            logoutBtn.click();
        } else {
            // Fallback: clear localStorage token and navigate to /login manually
            // This simulates what authService.logout() does
            WebDriverUtils.removeLocalStorageItem(driver, "token");
            driver.navigate().to(
                driver.getCurrentUrl().split("/studio")[0] + "/login"
            );
        }

        WebDriverUtils.waitForUrlContaining(driver, "/login");
        return new LoginPage(driver);
    }
}
