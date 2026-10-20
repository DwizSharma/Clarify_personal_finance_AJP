(function () {
    var form = document.getElementById('assistant-form');
    if (!form) return;
    var box = document.getElementById('assistant-messages');
    var input = document.getElementById('assistant-input');
    var send = form.querySelector('button');
    var history = [];

    function appendMessage(text, className) {
        var message = document.createElement('p');
        message.className = className;
        message.textContent = text;
        box.appendChild(message);
        box.scrollTop = box.scrollHeight;
        return message;
    }

    async function ask(question) {
        appendMessage(question, 'assistant-question');
        var loading = appendMessage('Thinking…', 'assistant-reply assistant-loading');
        send.disabled = true;
        try {
            var headers = { 'Content-Type': 'application/json' };
            if (form.dataset.csrfHeader && form.dataset.csrfToken) {
                headers[form.dataset.csrfHeader] = form.dataset.csrfToken;
            }
            var response = await fetch('/api/assistant', {
                method: 'POST',
                headers: headers,
                body: JSON.stringify({ message: question, history: history.slice(-10) })
            });
            if (!response.ok) throw new Error('Assistant request failed');
            var result = await response.json();
            loading.remove();
            appendMessage(result.answer || 'I could not find an answer. Try asking another way.', 'assistant-reply');
            history.push({ role: 'user', content: question }, { role: 'assistant', content: result.answer || '' });
            history = history.slice(-10);
            if (result.ai === 'true') appendMessage('AI answer', 'assistant-ai-tag');
            if (result.url && result.label) {
                var link = document.createElement('a');
                link.className = 'assistant-answer-link';
                link.href = result.url;
                link.textContent = result.label + ' ↗';
                box.appendChild(link);
            }
        } catch (error) {
            loading.textContent = 'I couldn’t connect just now. Check your connection and try again.';
        } finally {
            send.disabled = false;
            box.scrollTop = box.scrollHeight;
        }
    }

    form.addEventListener('submit', function (event) {
        event.preventDefault();
        var question = input.value.trim();
        if (!question) return;
        input.value = '';
        ask(question);
    });
    box.querySelectorAll('.assistant-suggestions button').forEach(function (button) {
        button.addEventListener('click', function () { ask(button.textContent.trim()); });
    });
})();
