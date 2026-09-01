package com.voisetu.e2e.pages;

import com.voisetu.e2e.utils.WebDriverUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.List;

/**
 * Page Object for the Angular Signup page (/signup).
 *
 * HTML element IDs (from signup.component.html):
 *   #signup-name        — display name input
 *   #signup-email       — email input
 *   #signup-password    — password input
 *   #btn-signup-submit  — submit button
 *   .error-banner       — server-side error message container
 *   .field-error        — client-side validation error spans
 *   .password-hints     — password strength hints panel
 *   .hint               — individual hint span
 *   .hint.ok            — hint that has been satisfied (green)
 */
public class SignupPage extends BasePage {

    // ------------------------------------------------------------------
    // Web Elements (PageFactory)
    // ------------------------------------------------------------------

    @FindBy(id = "signup-name")
    private WebElement displayNameInput;

    @FindBy(id = "signup-email")
    private WebElement emailInput;

    @FindBy(id = "signup-password")
    private WebElement passwordInput;

    @FindBy(id = "btn-signup-submit")
    private WebElement submitButton;

    @FindBy(css = ".error-banner")
    private WebElement errorBanner;

    @FindBy(css = ".password-hints")
    private WebElement passwordHintsPanel;

    // ------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------

    public SignupPage(WebDriver driver) {
        super(driver);
    }

    // ------------------------------------------------------------------
    // Actions
    // ------------------------------------------------------------------

    /** Enters the display name value into the Name field. */
    public SignupPage enterDisplayName(String name) {
        WebDriverUtils.waitForClickable(driver, displayNameInput);
        type(displayNameInput, name);
        return this;
    }

    /** Clears the display name field without entering anything. */
    public SignupPage clearDisplayName() {
        WebDriverUtils.waitForClickable(driver, displayNameInput);
        displayNameInput.clear();
        displayNameInput.click();  // triggers touched state
        return this;
    }

    /** Enters the email address. */
    public SignupPage enterEmail(String email) {
        WebDriverUtils.waitForClickable(driver, emailInput);
        type(emailInput, email);
        return this;
    }

    /** Clears the email field. */
    public SignupPage clearEmail() {
        WebDriverUtils.waitForClickable(driver, emailInput);
        emailInput.clear();
        emailInput.click();
        return this;
    }

    /** Enters the password and clicks elsewhere to trigger validation. */
    public SignupPage enterPassword(String password) {
        WebDriverUtils.waitForClickable(driver, passwordInput);
        type(passwordInput, password);
        return this;
    }

    /** Clicks the password field and moves focus away (to trigger 'touched'). */
    public SignupPage touchPasswordField() {
        passwordInput.click();
        displayNameInput.click(); // shift focus to trigger Angular validation
        return this;
    }

    /** Clears the password field. */
    public SignupPage clearPassword() {
        WebDriverUtils.waitForClickable(driver, passwordInput);
        passwordInput.clear();
        passwordInput.click();
        return this;
    }

    /** Clicks the "Create Free Account" submit button. */
    public SignupPage clickSubmit() {
        WebDriverUtils.waitForClickable(driver, submitButton);
        submitButton.click();
        return this;
    }

    /** Clicks the "Log in" link in the footer. */
    public LoginPage clickLoginLink() {
        driver.findElement(By.linkText("Log in")).click();
        waitForNavigationTo("/login");
        return new LoginPage(driver);
    }

    // ------------------------------------------------------------------
    // Assertions / Queries
    // ------------------------------------------------------------------

    /** Returns true if the page is currently loaded. */
    public boolean isOnSignupPage() {
        return driver.getCurrentUrl().contains("/signup");
    }

    /**
     * Returns the text of the first visible .field-error element that contains
     * the given expected text.
     */
    public boolean hasFieldError(String expectedText) {
        List<WebElement> errors = driver.findElements(By.cssSelector(".field-error"));
        return errors.stream()
                .filter(WebElement::isDisplayed)
                .anyMatch(e -> e.getText().trim().equals(expectedText));
    }

    /** Returns true if the server-side error banner is visible with the given text. */
    public boolean hasErrorBanner(String expectedText) {
        try {
            WebDriverUtils.waitForVisible(driver, errorBanner);
            return errorBanner.isDisplayed()
                    && errorBanner.getText().contains(expectedText);
        } catch (Exception e) {
            return false;
        }
    }

    /** Returns true if the password hints panel is visible. */
    public boolean isPasswordHintsPanelVisible() {
        try {
            return passwordHintsPanel.isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    /**
     * Returns true if a password strength hint with the given label text is
     * currently satisfied (has the CSS class "ok").
     *
     * @param hintLabel e.g. "8+ characters", "1 uppercase letter", "1 number"
     */
    public boolean isPasswordHintSatisfied(String hintLabel) {
        List<WebElement> hints = driver.findElements(By.cssSelector(".hint"));
        return hints.stream()
                .filter(h -> h.getText().contains(hintLabel))
                .anyMatch(h -> h.getAttribute("class").contains("ok"));
    }

    /**
     * Returns true if a password strength hint with the given label text is
     * NOT yet satisfied.
     */
    public boolean isPasswordHintNotSatisfied(String hintLabel) {
        List<WebElement> hints = driver.findElements(By.cssSelector(".hint"));
        return hints.stream()
                .filter(h -> h.getText().contains(hintLabel))
                .anyMatch(h -> !h.getAttribute("class").contains("ok"));
    }
}
