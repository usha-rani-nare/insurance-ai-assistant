// Central place for talking to the Spring Boot backend. Every page includes
// this file before its own page-specific script.

const API_BASE_URL = "http://localhost:8081/api";

// There's no login/session yet (that's a later phase), so every page acts as
// this one demo user for now. Once real authentication exists, this constant
// goes away and the user id comes from the logged-in session instead.
const CURRENT_USER_ID = 1;

// Wraps fetch() so every page handles errors the same way instead of each
// page reinventing try/catch. Throws an Error with a readable message.
async function apiRequest(path, options = {}) {
    let response;
    try {
        response = await fetch(`${API_BASE_URL}${path}`, {
            headers: { "Content-Type": "application/json" },
            ...options,
        });
    } catch (networkError) {
        throw new Error("Could not reach the server. Is the backend running?");
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

// Not wired up yet - there is no POST /api/auth/login endpoint yet (that's a
// later phase). This exists so login.js has something real to call once it's
// built, instead of the frontend inventing a fake response.
function login(email, password) {
    return apiRequest("/auth/login", {
        method: "POST",
        body: JSON.stringify({ email, password }),
    });
}

// Same idea for the AI assistant - the endpoint doesn't exist yet.
function askAssistant(question) {
    return apiRequest("/ai/chat", {
        method: "POST",
        body: JSON.stringify({ question }),
    });
}
