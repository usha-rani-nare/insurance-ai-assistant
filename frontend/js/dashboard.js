const statusMessage = document.getElementById("dashboard-status");
const content = document.getElementById("dashboard-content");

function formatCurrency(amount) {
    return `₹${Number(amount).toLocaleString("en-IN")}`;
}

function badgeClass(status) {
    return `badge badge-${status.toLowerCase()}`;
}

async function loadDashboard() {
    try {
        const user = await getUser(CURRENT_USER_ID);
        document.getElementById("welcome-heading").textContent = `Welcome, ${user.name}`;

        const policies = await getPoliciesForUser(CURRENT_USER_ID);
        const activePolicy = policies.find((p) => p.status === "ACTIVE") || policies[0];

        if (!activePolicy) {
            statusMessage.textContent = "You don't have any policies yet.";
            return;
        }

        document.getElementById("plan-name").textContent = activePolicy.planName;
        document.getElementById("policy-number").textContent = activePolicy.policyNumber;
        document.getElementById("policy-expiry").textContent = activePolicy.endDate;

        const statusBadge = document.getElementById("policy-status-badge");
        statusBadge.textContent = activePolicy.status;
        statusBadge.className = badgeClass(activePolicy.status);

        const claims = await getClaimsForPolicy(activePolicy.id);
        renderRecentClaims(claims.slice(0, 3));

        statusMessage.style.display = "none";
        content.style.display = "block";
    } catch (error) {
        statusMessage.textContent = `Dashboard details could not be loaded: ${error.message}`;
        statusMessage.className = "error-text";
    }
}

function renderRecentClaims(claims) {
    const tableBody = document.getElementById("recent-claims-body");
    const noClaimsMessage = document.getElementById("no-claims-message");

    if (claims.length === 0) {
        document.getElementById("recent-claims-table").style.display = "none";
        noClaimsMessage.style.display = "block";
        return;
    }

    tableBody.innerHTML = claims
        .map(
            (claim) => `
        <tr>
            <td>${claim.claimNumber}</td>
            <td>${claim.claimType}</td>
            <td>${formatCurrency(claim.claimAmount)}</td>
            <td><span class="${badgeClass(claim.status)}">${claim.status}</span></td>
        </tr>
    `
        )
        .join("");
}

loadDashboard();
