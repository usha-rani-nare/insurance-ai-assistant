const chatWindow = document.getElementById("chat-window");
const chatInput = document.getElementById("chat-input");
const chatSendButton = document.getElementById("chat-send-button");

function addMessage(sender, text) {
    const wrapper = document.createElement("div");
    wrapper.className = `chat-message ${sender === "You" ? "user" : "assistant"}`;
    wrapper.innerHTML = `
        <div class="sender">${sender}</div>
        <div class="bubble"></div>
    `;
    // Set via textContent, not innerHTML, so a question/answer can't inject markup.
    wrapper.querySelector(".bubble").textContent = text;
    chatWindow.appendChild(wrapper);
    chatWindow.scrollTop = chatWindow.scrollHeight;
    return wrapper;
}

async function sendMessage() {
    const question = chatInput.value.trim();
    if (!question) {
        return;
    }

    addMessage("You", question);
    chatInput.value = "";
    chatSendButton.disabled = true;

    const thinkingMessage = addMessage("Assistant", "Thinking...");

    try {
        const response = await askAssistant(question);
        thinkingMessage.querySelector(".bubble").textContent = response.answer;

        if (response.sources && response.sources.length > 0) {
            const sourcesLine = document.createElement("div");
            sourcesLine.className = "info-text";
            sourcesLine.style.marginTop = "4px";
            sourcesLine.style.fontSize = "0.78rem";
            sourcesLine.textContent = `Sources: ${response.sources.join(", ")}`;
            thinkingMessage.appendChild(sourcesLine);
        }
    } catch (error) {
        thinkingMessage.querySelector(".bubble").textContent = `Sorry, something went wrong: ${error.message}`;
    } finally {
        chatSendButton.disabled = false;
    }
}

chatSendButton.addEventListener("click", sendMessage);
chatInput.addEventListener("keydown", (event) => {
    if (event.key === "Enter") {
        sendMessage();
    }
});
