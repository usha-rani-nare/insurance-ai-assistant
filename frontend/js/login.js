const loginForm = document.getElementById("login-form");
const loginButton = document.getElementById("login-button");
const registerButton = document.getElementById("register-button");
const loginMessage = document.getElementById("login-message");

loginForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    const email = document.getElementById("email").value;
    const password = document.getElementById("password").value;

    // There is no real login endpoint yet - see login() in api.js. Rather
    // than fake a successful authentication, we say so plainly and continue
    // to the dashboard using the demo user, so the rest of the UI is still
    // reachable and reviewable.
    loginButton.disabled = true;
    loginMessage.textContent = `Authentication isn't implemented yet. Continuing to the dashboard as the demo user...`;
    loginMessage.className = "info-text";

    setTimeout(() => {
        window.location.href = "dashboard.html";
    }, 900);
});

registerButton.addEventListener("click", async () => {
    const email = document.getElementById("email").value;
    const password = document.getElementById("password").value;

    if (!email || !password) {
        loginMessage.textContent = "Enter an email and password to register.";
        loginMessage.className = "error-text";
        return;
    }

    registerButton.disabled = true;
    loginMessage.textContent = "Registering...";
    loginMessage.className = "info-text";

    try {
        const name = email.split("@")[0];
        await registerUser({ name, email, password });
        loginMessage.textContent = "Account created. You can now log in.";
        loginMessage.className = "success-text";
    } catch (error) {
        loginMessage.textContent = error.message;
        loginMessage.className = "error-text";
    } finally {
        registerButton.disabled = false;
    }
});
