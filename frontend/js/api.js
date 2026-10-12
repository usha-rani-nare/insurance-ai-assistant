// Central place for talking to the Spring Boot backend. Every page includes
// this file before its own page-specific script.

const API_BASE_URL = "http://localhost:8081/api";

// --- Session handling ---
// After login the backend returns a JWT. We keep it in localStorage and send
// it as "Authorization: Bearer ..." on every request. (localStorage is fine
// for a portfolio project; a production app would usually prefer httpOnly
// cookies so scripts can't read the token.)

function getToken() {
    return localStorage.getItem("authToken");
}

function saveSession(loginResponse) {
    localStorage.setItem("authToken", loginResponse.token);
    localStorage.setItem("userId", loginResponse.userId);
    localStorage.setItem("userName", loginResponse.name);
}

function clearSession() {
    localStorage.removeItem("authToken");
    localStorage.removeItem("userId");
    localStorage.removeItem("userName");
}

function logout() {
    clearSession();
    window.location.href = "login.html";
}

const onLoginPage = window.location.pathname.endsWith("login.html");

// Every page except login.html needs a logged-in user.
if (!onLoginPage && !getToken()) {
    window.location.href = "login.html";
}

// The pages still use CURRENT_USER_ID, but it now comes from the logged-in
// session instead of being hardcoded to 1.
const CURRENT_USER_ID = Number(localStorage.getItem("userId")) || null;

// Wraps fetch() so every page handles errors the same way instead of each
// page reinventing try/catch. Throws an Error with a readable message.
async function apiRequest(path, options = {}) {
    const headers = { "Content-Type": "application/json", ...(options.headers || {}) };
    const token = getToken();
    if (token) {
        headers["Authorization"] = `Bearer ${token}`;
    }

    let response;
    try {
        response = await fetch(`${API_BASE_URL}${path}`, { ...options, headers });
    } catch (networkError) {
        throw new Error("Could not reach the server. Is the backend running?");
    }

    // Expired or invalid token: send the user back to login. The login
    // request itself is excluded so a wrong password shows its own message.
    if (response.status === 401 && path !== "/auth/login") {
        clearSession();
        window.location.href = "login.html";
        throw new Error("Your session has expired. Please log in again.");
    }

    if (!response.ok) {
        const errorBody = await response.json().catch(() => null);
        const message = errorBody?.message || `Request failed (${response.status})`;
        throw new Error(message);
    }

    if (response.status === 204) {
        return null;
    }

    return response.json();
}

function getUser(userId) {
    return apiRequest(`/users/${userId}`);
}

function getPoliciesForUser(userId) {
    return apiRequest(`/policies/user/${userId}`);
}

function getPolicy(policyId) {
    return apiRequest(`/policies/${policyId}`);
}

function getClaimsForPolicy(policyId) {
    return apiRequest(`/claims/policy/${policyId}`);
}

function createClaim(claimData) {
    return apiRequest("/claims", {
        method: "POST",
        body: JSON.stringify(claimData),
    });
}

function registerUser(userData) {
    return apiRequest("/users/register", {
        method: "POST",
        body: JSON.stringify(userData),
    });
}

function login(email, password) {
    return apiRequest("/auth/login", {
        method: "POST",
        body: JSON.stringify({ email, password }),
    });
}

function askAssistant(question) {
    return apiRequest("/ai/chat", {
        method: "POST",
        body: JSON.stringify({ question }),
    });
}