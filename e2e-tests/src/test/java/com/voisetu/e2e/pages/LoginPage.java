package com.voisetu.e2e.pages;

import com.voisetu.e2e.utils.WebDriverUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.List;

/**
 * Page Object for the Angular Login page (/login).
 *
 * HTML element IDs (from login.component.html):
 *   #login-email        — email input
 *   #login-password     — password input
 *   #btn-login-submit   — submit button
 *   #btn-goto-register  — "Create a free account" link
 *   .error-banner       — server-side error container
 *   .field-error        — client-side validation error spans
 */
public class LoginPage extends BasePage {

    // ------------------------------------------------------------------
    // Web Elements (PageFactory)
    // ------------------------------------------------------------------

    @FindBy(id = "login-email")
    private WebElement emailInput;

    @FindBy(id = "login-password")
    private WebElement passwordInput;

    @FindBy(id = "btn-login-submit")
    private WebElement submitButton;

    @FindBy(id = "btn-goto-register")
    private WebElement gotoRegisterLink;

    @FindBy(css = ".error-banner")
    private WebElement errorBanner;

    // ------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    // ------------------------------------------------------------------
    // Actions
    // ------------------------------------------------------------------

    /** Enters the email address into the login email field. */
    public LoginPage enterEmail(String email) {
        WebDriverUtils.waitForClickable(driver, emailInput);
        type(emailInput, email);
        return this;
    }

    /** Clears the email field to leave it blank. */
    public LoginPage clearEmail() {
        WebDriverUtils.waitForClickable(driver, emailInput);
        emailInput.clear();
        emailInput.click();
        return this;
    }

    /** Enters the password into the login password field. */
    public LoginPage enterPassword(String password) {
        WebDriverUtils.waitForClickable(driver, passwordInput);
        type(passwordInput, password);
        return this;
    }

    /** Clears the password field. */
    public LoginPage clearPassword() {
        WebDriverUtils.waitForClickable(driver, passwordInput);
        passwordInput.clear();
        passwordInput.click();
        return this;
    }

    /** Clicks the "Log In" submit button. */
    public LoginPage clickSubmit() {
        WebDriverUtils.waitForClickable(driver, submitButton);
        submitButton.click();
        return this;
    }

    /** Clicks the "Create a free account" link (navigates to /signup). */
    public SignupPage clickCreateAccountLink() {
        WebDriverUtils.waitForClickable(driver, gotoRegisterLink);
        gotoRegisterLink.click();
        waitForNavigationTo("/signup");
        return new SignupPage(driver);
    }

    // ------------------------------------------------------------------
    // Assertions / Queries
    // ------------------------------------------------------------------

    /** Returns true if the browser is currently on /login. */
    public boolean isOnLoginPage() {
        return driver.getCurrentUrl().contains("/login");
    }

    /** Returns true if any visible .field-error contains the expected text. */
    public boolean hasFieldError(String expectedText) {
        List<WebElement> errors = driver.findElements(By.cssSelector(".field-error"));
        return errors.stream()
                .filter(WebElement::isDisplayed)
                .anyMatch(e -> e.getText().trim().equals(expectedText));
    }

    /** Returns true if the server-side error banner is visible and contains expected text. */
    public boolean hasErrorBanner(String expectedText) {
        try {
            WebDriverUtils.waitForVisible(driver, errorBanner);
            return errorBanner.isDisplayed()
                    && errorBanner.getText().contains(expectedText);
        } catch (Exception e) {
            return false;
        }
    }
}
