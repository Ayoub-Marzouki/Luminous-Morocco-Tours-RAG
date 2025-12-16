function switchTab(tabId) {
    document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
    document.querySelectorAll('.content').forEach(c => c.classList.remove('active'));
    document.querySelector(`.tab[onclick="switchTab('${tabId}')"]`).classList.add('active');
    document.getElementById(tabId).classList.add('active');
}

function handleEnter(e) {
    if (e.key === 'Enter') sendMessage();
}

async function sendMessage() {
    const input = document.getElementById('chat-input');
    const history = document.getElementById('chat-history');
    const text = input.value.trim();
    if (!text) return;

    // Add User Message
    history.innerHTML += `<div class="message user-msg">${text}</div>`;
    input.value = '';
    history.scrollTop = history.scrollHeight;

    try {
        const response = await fetch('/api/chat', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ query: text })
        });
        const data = await response.json();
        
        // Add AI Message
        // Simple markdown parsing for bolding
        let answer = data.answer.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
        history.innerHTML += `<div class="message ai-msg">${answer}</div>`;
        history.scrollTop = history.scrollHeight;
    } catch (error) {
        history.innerHTML += `<div class="message ai-msg" style="color:red">Error: Could not reach the server.</div>`;
    }
}

async function generateBlog() {
    const topic = document.getElementById('blog-topic').value;
    const resultDiv = document.getElementById('blog-result');
    const loading = document.getElementById('blog-loading');
    const btn = document.getElementById('generate-btn');

    if (!topic) return;

    resultDiv.style.display = 'none';
    loading.style.display = 'block';
    btn.disabled = true;

    try {
        const response = await fetch('/api/blog/generate', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ topic: topic })
        });
        const data = await response.json();
        
        resultDiv.innerHTML = data.content; // Render HTML directly or text
        resultDiv.style.display = 'block';
    } catch (error) {
        alert('Failed to generate blog.');
    } finally {
        loading.style.display = 'none';
        btn.disabled = false;
    }
}
