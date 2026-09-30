import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useLanguage } from "../../context/LanguageContext";
import {
  apiRequest,
  setAuthData,
} from "../../api/apiClient";
import "./Auth.css";

function Auth() {
  const { language, setLanguage } = useLanguage();
  const navigate = useNavigate();

  const isHindi = language === "hi";

  /*
   * ---------------------------------------------------------
   * Authentication screens
   * ---------------------------------------------------------
   *
   * login
   * register
   * forgot
   * reset
   */
  const [mode, setMode] = useState("login");

  /*
   * ---------------------------------------------------------
   * Login state
   * ---------------------------------------------------------
   */
  const [loginEmail, setLoginEmail] = useState("");
  const [loginPassword, setLoginPassword] = useState("");

  /*
   * ---------------------------------------------------------
   * Registration state
   * ---------------------------------------------------------
   */
  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [registerEmail, setRegisterEmail] =
      useState("");
  const [registerPassword, setRegisterPassword] =
      useState("");
  const [confirmPassword, setConfirmPassword] =
      useState("");

  /*
   * ---------------------------------------------------------
   * Forgot password state
   * ---------------------------------------------------------
   */
  const [forgotEmail, setForgotEmail] = useState("");

  /*
   * ---------------------------------------------------------
   * Reset password state
   * ---------------------------------------------------------
   */
  const [resetToken, setResetToken] = useState("");
  const [newPassword, setNewPassword] =
      useState("");
  const [resetConfirmPassword, setResetConfirmPassword] =
      useState("");

  /*
   * ---------------------------------------------------------
   * UI state
   * ---------------------------------------------------------
   */
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  /*
   * Development-only reset token.
   *
   * Your backend currently supports:
   *
   * agrinexus.auth.expose-reset-token=true
   *
   * so the token can be displayed during local testing.
   */
  const [developmentResetToken, setDevelopmentResetToken] =
      useState("");

  /*
   * ---------------------------------------------------------
   * Helpers
   * ---------------------------------------------------------
   */

  const resetFeedback = () => {
    setMessage("");
    setError("");
  };


  const getErrorMessage = (
      requestError,
      fallbackMessage
  ) => {
    if (
        requestError?.data?.validationErrors
    ) {
      const validationErrors =
          Object.values(
              requestError.data.validationErrors
          );

      if (validationErrors.length > 0) {
        return validationErrors.join(" ");
      }
    }

    return (
        requestError?.message ||
        fallbackMessage
    );
  };


  /*
   * ---------------------------------------------------------
   * Login
   * ---------------------------------------------------------
   */
  const handleLogin = async (event) => {
    event.preventDefault();

    resetFeedback();

    if (!loginEmail.trim()) {
      setError(
          isHindi
              ? "ईमेल दर्ज करें।"
              : "Please enter your email."
      );
      return;
    }

    if (!loginPassword) {
      setError(
          isHindi
              ? "पासवर्ड दर्ज करें।"
              : "Please enter your password."
      );
      return;
    }

    try {
      setLoading(true);

      const response = await apiRequest(
          "/api/auth/login",
          {
            method: "POST",
            body: JSON.stringify({
              email: loginEmail.trim(),
              password: loginPassword,
            }),
          }
      );

      /*
       * Store JWT + user information.
       */
      setAuthData(response);

      setMessage(
          isHindi
              ? "लॉगिन सफल हुआ।"
              : "Login successful."
      );

      /*
       * Existing team frontend uses /home.
       * Keep that route so we don't break the
       * existing navigation.
       */
      navigate("/home");

    } catch (requestError) {
      setError(
          getErrorMessage(
              requestError,
              isHindi
                  ? "लॉगिन असफल हुआ।"
                  : "Login failed."
          )
      );
    } finally {
      setLoading(false);
    }
  };


  /*
   * ---------------------------------------------------------
   * Register
   * ---------------------------------------------------------
   */
  const handleRegister = async (event) => {
    event.preventDefault();

    resetFeedback();

    if (!firstName.trim()) {
      setError(
          isHindi
              ? "पहला नाम दर्ज करें।"
              : "Please enter your first name."
      );
      return;
    }

    if (!lastName.trim()) {
      setError(
          isHindi
              ? "अंतिम नाम दर्ज करें।"
              : "Please enter your last name."
      );
      return;
    }

    if (!registerEmail.trim()) {
      setError(
          isHindi
              ? "ईमेल दर्ज करें।"
              : "Please enter your email."
      );
      return;
    }

    if (registerPassword.length < 8) {
      setError(
          isHindi
              ? "पासवर्ड कम से कम 8 अक्षरों का होना चाहिए।"
              : "Password must contain at least 8 characters."
      );
      return;
    }

    if (
        registerPassword !== confirmPassword
    ) {
      setError(
          isHindi
              ? "पासवर्ड मेल नहीं खाते।"
              : "Passwords do not match."
      );
      return;
    }

    try {
      setLoading(true);

      const response = await apiRequest(
          "/api/auth/register",
          {
            method: "POST",
            body: JSON.stringify({
              firstName: firstName.trim(),
              lastName: lastName.trim(),
              email: registerEmail.trim(),
              password: registerPassword,
              role: "FARMER",
            }),
          }
      );

      /*
       * Backend currently returns JWT immediately
       * after successful registration.
       */
      setAuthData(response);

      setMessage(
          isHindi
              ? "खाता सफलतापूर्वक बनाया गया।"
              : "Account created successfully."
      );

      navigate("/home");

    } catch (requestError) {
      setError(
          getErrorMessage(
              requestError,
              isHindi
                  ? "पंजीकरण असफल हुआ।"
                  : "Registration failed."
          )
      );
    } finally {
      setLoading(false);
    }
  };


  /*
   * ---------------------------------------------------------
   * Forgot password
   * ---------------------------------------------------------
   */
  const handleForgotPassword = async (
      event
  ) => {
    event.preventDefault();

    resetFeedback();

    setDevelopmentResetToken("");

    if (!forgotEmail.trim()) {
      setError(
          isHindi
              ? "ईमेल दर्ज करें।"
              : "Please enter your email."
      );
      return;
    }

    try {
      setLoading(true);

      const response = await apiRequest(
          "/api/auth/forgot-password",
          {
            method: "POST",
            body: JSON.stringify({
              email: forgotEmail.trim(),
            }),
          }
      );

      setMessage(
          response?.message ||
          (
              isHindi
                  ? "पासवर्ड रीसेट अनुरोध बनाया गया है।"
                  : "Password reset request created."
          )
      );

      /*
       * Local development support.
       *
       * In production this should be removed and
       * the reset link should be delivered by email/SMS.
       */
      if (response?.resetToken) {
        setDevelopmentResetToken(
            response.resetToken
        );

        setResetToken(
            response.resetToken
        );
      }

      setMode("reset");

    } catch (requestError) {
      setError(
          getErrorMessage(
              requestError,
              isHindi
                  ? "पासवर्ड रीसेट अनुरोध असफल हुआ।"
                  : "Password reset request failed."
          )
      );
    } finally {
      setLoading(false);
    }
  };


  /*
   * ---------------------------------------------------------
   * Reset password
   * ---------------------------------------------------------
   */
  const handleResetPassword = async (
      event
  ) => {
    event.preventDefault();

    resetFeedback();

    if (!resetToken.trim()) {
      setError(
          isHindi
              ? "रीसेट टोकन दर्ज करें।"
              : "Please enter the reset token."
      );
      return;
    }

    if (newPassword.length < 8) {
      setError(
          isHindi
              ? "नया पासवर्ड कम से कम 8 अक्षरों का होना चाहिए।"
              : "New password must contain at least 8 characters."
      );
      return;
    }

    if (
        newPassword !== resetConfirmPassword
    ) {
      setError(
          isHindi
              ? "पासवर्ड मेल नहीं खाते।"
              : "Passwords do not match."
      );
      return;
    }

    try {
      setLoading(true);

      const response = await apiRequest(
          "/api/auth/reset-password",
          {
            method: "POST",
            body: JSON.stringify({
              token: resetToken.trim(),
              newPassword,
            }),
          }
      );

      setMessage(
          typeof response === "string"
              ? response
              : (
                  isHindi
                      ? "पासवर्ड सफलतापूर्वक बदल दिया गया।"
                      : "Password reset successfully."
              )
      );

      /*
       * Clear reset information after successful
       * password reset.
       */
      setResetToken("");
      setNewPassword("");
      setResetConfirmPassword("");
      setDevelopmentResetToken("");

      /*
       * Send user back to login.
       */
      setTimeout(() => {
        setMode("login");
        setMessage(
            isHindi
                ? "पासवर्ड बदल गया। अब लॉगिन करें।"
                : "Password changed. Please login."
        );
      }, 800);

    } catch (requestError) {
      setError(
          getErrorMessage(
              requestError,
              isHindi
                  ? "पासवर्ड रीसेट असफल हुआ।"
                  : "Password reset failed."
          )
      );
    } finally {
      setLoading(false);
    }
  };


  /*
   * ---------------------------------------------------------
   * Login screen
   * ---------------------------------------------------------
   */
  const renderLogin = () => (
      <form
          className="auth-card"
          onSubmit={handleLogin}
      >
        <h2 className="auth-card-title">
          {isHindi
              ? "किसान लॉगिन"
              : "Farmer Login"}
        </h2>

        <label className="auth-label">
          {isHindi
              ? "ईमेल"
              : "Email"}
        </label>

        <input
            type="email"
            className="auth-input"
            placeholder={
              isHindi
                  ? "अपना ईमेल दर्ज करें"
                  : "Enter your email"
            }
            value={loginEmail}
            onChange={(event) =>
                setLoginEmail(event.target.value)
            }
            autoComplete="email"
        />

        <label className="auth-label">
          {isHindi
              ? "पासवर्ड"
              : "Password"}
        </label>

        <input
            type="password"
            className="auth-input"
            placeholder={
              isHindi
                  ? "पासवर्ड दर्ज करें"
                  : "Enter your password"
            }
            value={loginPassword}
            onChange={(event) =>
                setLoginPassword(event.target.value)
            }
            autoComplete="current-password"
        />

        <button
            type="submit"
            className="auth-button"
            disabled={loading}
        >
          {loading
              ? (
                  isHindi
                      ? "लॉगिन हो रहा है..."
                      : "Logging in..."
              )
              : (
                  isHindi
                      ? "लॉगिन करें"
                      : "Login"
              )}
        </button>

        <button
            type="button"
            className="auth-link-button"
            onClick={() => {
              resetFeedback();
              setMode("forgot");
            }}
        >
          {isHindi
              ? "पासवर्ड भूल गए?"
              : "Forgot Password?"}
        </button>

        <div className="auth-divider">
        <span>
          {isHindi
              ? "नया किसान?"
              : "New farmer?"}
        </span>
        </div>

        <button
            type="button"
            className="auth-secondary-button"
            onClick={() => {
              resetFeedback();
              setMode("register");
            }}
        >
          {isHindi
              ? "नया खाता बनाएं"
              : "Create Account"}
        </button>
      </form>
  );


  /*
   * ---------------------------------------------------------
   * Register screen
   * ---------------------------------------------------------
   */
  const renderRegister = () => (
      <form
          className="auth-card"
          onSubmit={handleRegister}
      >
        <h2 className="auth-card-title">
          {isHindi
              ? "किसान पंजीकरण"
              : "Farmer Registration"}
        </h2>

        <label className="auth-label">
          {isHindi
              ? "पहला नाम"
              : "First Name"}
        </label>

        <input
            type="text"
            className="auth-input auth-input-text"
            placeholder={
              isHindi
                  ? "पहला नाम"
                  : "First name"
            }
            value={firstName}
            onChange={(event) =>
                setFirstName(event.target.value)
            }
            autoComplete="given-name"
        />

        <label className="auth-label">
          {isHindi
              ? "अंतिम नाम"
              : "Last Name"}
        </label>

        <input
            type="text"
            className="auth-input auth-input-text"
            placeholder={
              isHindi
                  ? "अंतिम नाम"
                  : "Last name"
            }
            value={lastName}
            onChange={(event) =>
                setLastName(event.target.value)
            }
            autoComplete="family-name"
        />

        <label className="auth-label">
          {isHindi
              ? "ईमेल"
              : "Email"}
        </label>

        <input
            type="email"
            className="auth-input"
            placeholder={
              isHindi
                  ? "ईमेल दर्ज करें"
                  : "Enter your email"
            }
            value={registerEmail}
            onChange={(event) =>
                setRegisterEmail(event.target.value)
            }
            autoComplete="email"
        />

        <label className="auth-label">
          {isHindi
              ? "पासवर्ड"
              : "Password"}
        </label>

        <input
            type="password"
            className="auth-input"
            placeholder={
              isHindi
                  ? "कम से कम 8 अक्षर"
                  : "At least 8 characters"
            }
            value={registerPassword}
            onChange={(event) =>
                setRegisterPassword(
                    event.target.value
                )
            }
            autoComplete="new-password"
        />

        <label className="auth-label">
          {isHindi
              ? "पासवर्ड की पुष्टि करें"
              : "Confirm Password"}
        </label>

        <input
            type="password"
            className="auth-input"
            placeholder={
              isHindi
                  ? "पासवर्ड दोबारा दर्ज करें"
                  : "Re-enter password"
            }
            value={confirmPassword}
            onChange={(event) =>
                setConfirmPassword(
                    event.target.value
                )
            }
            autoComplete="new-password"
        />

        <button
            type="submit"
            className="auth-button"
            disabled={loading}
        >
          {loading
              ? (
                  isHindi
                      ? "खाता बनाया जा रहा है..."
                      : "Creating account..."
              )
              : (
                  isHindi
                      ? "खाता बनाएं"
                      : "Create Account"
              )}
        </button>

        <button
            type="button"
            className="auth-link-button"
            onClick={() => {
              resetFeedback();
              setMode("login");
            }}
        >
          {isHindi
              ? "पहले से खाता है? लॉगिन करें"
              : "Already have an account? Login"}
        </button>
      </form>
  );


  /*
   * ---------------------------------------------------------
   * Forgot password screen
   * ---------------------------------------------------------
   */
  const renderForgotPassword = () => (
      <form
          className="auth-card"
          onSubmit={handleForgotPassword}
      >
        <h2 className="auth-card-title">
          {isHindi
              ? "पासवर्ड भूल गए"
              : "Forgot Password"}
        </h2>

        <p className="auth-description">
          {isHindi
              ? "अपने खाते का ईमेल दर्ज करें।"
              : "Enter your registered email address."}
        </p>

        <label className="auth-label">
          {isHindi
              ? "ईमेल"
              : "Email"}
        </label>

        <input
            type="email"
            className="auth-input"
            placeholder={
              isHindi
                  ? "अपना ईमेल दर्ज करें"
                  : "Enter your email"
            }
            value={forgotEmail}
            onChange={(event) =>
                setForgotEmail(event.target.value)
            }
            autoComplete="email"
        />

        <button
            type="submit"
            className="auth-button"
            disabled={loading}
        >
          {loading
              ? (
                  isHindi
                      ? "प्रोसेस हो रहा है..."
                      : "Processing..."
              )
              : (
                  isHindi
                      ? "रीसेट लिंक प्राप्त करें"
                      : "Request Reset"
              )}
        </button>

        <button
            type="button"
            className="auth-link-button"
            onClick={() => {
              resetFeedback();
              setMode("login");
            }}
        >
          {isHindi
              ? "वापस लॉगिन पर जाएं"
              : "Back to Login"}
        </button>
      </form>
  );


  /*
   * ---------------------------------------------------------
   * Reset password screen
   * ---------------------------------------------------------
   */
  const renderResetPassword = () => (
      <form
          className="auth-card"
          onSubmit={handleResetPassword}
      >
        <h2 className="auth-card-title">
          {isHindi
              ? "पासवर्ड रीसेट करें"
              : "Reset Password"}
        </h2>

        <label className="auth-label">
          {isHindi
              ? "रीसेट टोकन"
              : "Reset Token"}
        </label>

        <input
            type="text"
            className="auth-input auth-input-text"
            placeholder={
              isHindi
                  ? "रीसेट टोकन"
                  : "Enter reset token"
            }
            value={resetToken}
            onChange={(event) =>
                setResetToken(event.target.value)
            }
        />

        {developmentResetToken && (
            <div className="auth-reset-token-box">
              <strong>
                {isHindi
                    ? "डेवलपमेंट रीसेट टोकन:"
                    : "Development Reset Token:"}
              </strong>

              <code>
                {developmentResetToken}
              </code>

              <button
                  type="button"
                  className="auth-small-button"
                  onClick={() =>
                      setResetToken(
                          developmentResetToken
                      )
                  }
              >
                {isHindi
                    ? "टोकन इस्तेमाल करें"
                    : "Use Token"}
              </button>
            </div>
        )}

        <label className="auth-label">
          {isHindi
              ? "नया पासवर्ड"
              : "New Password"}
        </label>

        <input
            type="password"
            className="auth-input"
            placeholder={
              isHindi
                  ? "नया पासवर्ड"
                  : "Enter new password"
            }
            value={newPassword}
            onChange={(event) =>
                setNewPassword(
                    event.target.value
                )
            }
            autoComplete="new-password"
        />

        <label className="auth-label">
          {isHindi
              ? "पासवर्ड की पुष्टि करें"
              : "Confirm New Password"}
        </label>

        <input
            type="password"
            className="auth-input"
            placeholder={
              isHindi
                  ? "पासवर्ड दोबारा दर्ज करें"
                  : "Re-enter new password"
            }
            value={resetConfirmPassword}
            onChange={(event) =>
                setResetConfirmPassword(
                    event.target.value
                )
            }
            autoComplete="new-password"
        />

        <button
            type="submit"
            className="auth-button"
            disabled={loading}
        >
          {loading
              ? (
                  isHindi
                      ? "रीसेट हो रहा है..."
                      : "Resetting..."
              )
              : (
                  isHindi
                      ? "पासवर्ड बदलें"
                      : "Reset Password"
              )}
        </button>

        <button
            type="button"
            className="auth-link-button"
            onClick={() => {
              resetFeedback();
              setMode("login");
            }}
        >
          {isHindi
              ? "लॉगिन पर वापस जाएं"
              : "Back to Login"}
        </button>
      </form>
  );


  /*
   * ---------------------------------------------------------
   * Main render
   * ---------------------------------------------------------
   */
  return (
      <div className="auth-container">

        <div className="auth-lang-toggle">
          <button
              type="button"
              className={
                language === "hi"
                    ? "lang-pill active"
                    : "lang-pill"
              }
              onClick={() =>
                  setLanguage("hi")
              }
          >
            हिंदी
          </button>

          <button
              type="button"
              className={
                language === "en"
                    ? "lang-pill active"
                    : "lang-pill"
              }
              onClick={() =>
                  setLanguage("en")
              }
          >
            English
          </button>
        </div>


        <h1 className="auth-title">
          AgriNexus
        </h1>


        <p className="auth-subtitle">
          {isHindi
              ? "स्मार्ट फार्म निर्णय प्रणाली"
              : "Smart Farm Decision System"}
        </p>


        {message && (
            <div className="auth-success-message">
              {message}
            </div>
        )}


        {error && (
            <div className="auth-error-message">
              {error}
            </div>
        )}


        {mode === "login" &&
            renderLogin()}

        {mode === "register" &&
            renderRegister()}

        {mode === "forgot" &&
            renderForgotPassword()}

        {mode === "reset" &&
            renderResetPassword()}

      </div>
  );
}

export default Auth;