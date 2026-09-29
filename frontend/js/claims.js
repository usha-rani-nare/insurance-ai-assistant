const claimsStatusMessage = document.getElementById("claims-status-message");
const claimsTable = document.getElementById("claims-table");
const claimsTableBody = document.getElementById("claims-table-body");
const noClaimsMessage = document.getElementById("no-claims-message");
const claimPolicySelect = document.getElementById("claim-policy");
const claimForm = document.getElementById("claim-form");
const claimFormMessage = document.getElementById("claim-form-message");
const claimSubmitButton = document.getElementById("claim-submit-button");

let userPolicies = [];

function formatCurrency(amount) {
    return `₹${Number(amount).toLocaleString("en-IN")}`;
}

function badgeClass(status) {
    return `badge badge-${status.toLowerCase()}`;
}

async function loadPoliciesIntoSelect() {
    userPolicies = await getPoliciesForUser(CURRENT_USER_ID);
    claimPolicySelect.innerHTML = userPolicies
        .map((policy) => `<option value="${policy.id}">${policy.policyNumber} - ${policy.planName}</option>`)
        .join("");
}

async function loadClaims() {
    try {
        if (userPolicies.length === 0) {
            claimsStatusMessage.textContent = "You don't have any policies yet, so there are no claims to show.";
            return;
        }

        // A user could have several policies; for this simple demo we show
        // claims across all of them rather than adding a filter control.
        const claimLists = await Promise.all(userPolicies.map((p) => getClaimsForPolicy(p.id)));
        const claims = claimLists.flat();

        if (claims.length === 0) {
            claimsStatusMessage.style.display = "none";
            noClaimsMessage.style.display = "block";
            return;
        }

        claimsTableBody.innerHTML = claims
            .map(
                (claim) => `
            <tr>
                <td>${claim.claimNumber}</td>
                <td>${claim.claimType}</td>
                <td>${formatCurrency(claim.claimAmount)}</td>
                <td>${claim.submittedAt.split("T")[0]}</td>
                <td><span class="${badgeClass(claim.status)}">${claim.status}</span></td>
            </tr>
        `
            )
            .join("");

        claimsStatusMessage.style.display = "none";
        claimsTable.style.display = "table";
    } catch (error) {
        claimsStatusMessage.textContent = `Claims could not be loaded: ${error.message}`;
        claimsStatusMessage.className = "error-text";
    }
}

claimForm.addEventListener("submit", async (event) => {
    event.preventDefault();

    const policyId = Number(claimPolicySelect.value);
    const claimType = document.getElementById("claim-type").value;
    const description = document.getElementById("claim-description").value.trim();
    const claimAmount = Number(document.getElementById("claim-amount").value);

    if (!policyId || !claimType || !description || !(claimAmount > 0)) {
        claimFormMessage.textContent = "Please fill in every field with a valid claim amount.";
        claimFormMessage.className = "error-text";
        return;
    }

    claimSubmitButton.disabled = true;
    claimFormMessage.textContent = "Submitting claim...";
    claimFormMessage.className = "info-text";

    try {
        await createClaim({ policyId, claimType, description, claimAmount });
        claimFormMessage.textContent = "Claim submitted successfully.";
        claimFormMessage.className = "success-text";
        claimForm.reset();
        await loadClaims();
    } catch (error) {
        claimFormMessage.textContent = error.message;
        claimFormMessage.className = "error-text";
    } finally {
        claimSubmitButton.disabled = false;
    }
});

async function init() {
    try {
        await loadPoliciesIntoSelect();
        await loadClaims();
    } catch (error) {
        claimsStatusMessage.textContent = `Claims could not be loaded: ${error.message}`;
        claimsStatusMessage.className = "error-text";
    }
}

init();
