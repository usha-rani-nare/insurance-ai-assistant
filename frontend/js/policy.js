const statusMessage = document.getElementById("policy-status-message");
const policyCard = document.getElementById("policy-card");

function formatCurrency(amount) {
    return `₹${Number(amount).toLocaleString("en-IN")}`;
}

async function loadPolicy() {
    try {
        const policies = await getPoliciesForUser(CURRENT_USER_ID);
        const activePolicy = policies.find((p) => p.status === "ACTIVE") || policies[0];

        if (!activePolicy) {
            statusMessage.textContent = "You don't have any policies yet.";
            return;
        }

        // We already have the summary from /policies/user/{id}, but fetch
        // the single-policy endpoint too since that's the one a real policy
        // detail page would use (and it's the one Postman/tests hit directly).
        const policy = await getPolicy(activePolicy.id);

        document.getElementById("detail-policy-number").textContent = policy.policyNumber;
        document.getElementById("detail-plan-name").textContent = policy.planName;
        document.getElementById("detail-coverage").textContent = formatCurrency(policy.coverageAmount);
        document.getElementById("detail-premium").textContent = formatCurrency(policy.premiumAmount);
        document.getElementById("detail-start-date").textContent = policy.startDate;
        document.getElementById("detail-end-date").textContent = policy.endDate;

        const statusBadge = document.getElementById("detail-status-badge");
        statusBadge.textContent = policy.status;
        statusBadge.className = `badge badge-${policy.status.toLowerCase()}`;

        statusMessage.style.display = "none";
        policyCard.style.display = "block";
    } catch (error) {
        statusMessage.textContent = "Policy details could not be loaded.";
        statusMessage.className = "error-text";
    }
}

loadPolicy();
