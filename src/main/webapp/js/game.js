const params = new URLSearchParams(window.location.search);
const roomId = params.get('roomId');
const roomCode = params.get('roomCode');
const playerId = params.get('playerId');
const playerName = params.get('playerName');

document.getElementById('roomCodeSmall').textContent = roomCode;

const canvas = document.getElementById('canvas');
const ctx = canvas.getContext('2d');
const colorPicker = document.getElementById('colorPicker');
const sizePicker = document.getElementById('sizePicker');

let lastStrokeId = 0;
let lastMessageId = 0;
let isDrawer = false;
let drawing = false;
let lastX = 0, lastY = 0;
let currentRoundNumber = 1;
let roundTimerInterval = null;
let secondsLeft = 60;

// ---------------- Canvas drawing (drawer only) ----------------

canvas.addEventListener('mousedown', (e) => {
    if (!isDrawer) return;
    drawing = true;
    const pos = getPos(e);
    lastX = pos.x; lastY = pos.y;
});

canvas.addEventListener('mousemove', (e) => {
    if (!isDrawer || !drawing) return;
    const pos = getPos(e);
    drawLineLocal(lastX, lastY, pos.x, pos.y, colorPicker.value, sizePicker.value);
    sendStroke(lastX, lastY, pos.x, pos.y, colorPicker.value, sizePicker.value);
    lastX = pos.x; lastY = pos.y;
});

window.addEventListener('mouseup', () => { drawing = false; });
canvas.addEventListener('mouseleave', () => { drawing = false; });

function getPos(e) {
    const rect = canvas.getBoundingClientRect();
    return {
        x: (e.clientX - rect.left) * (canvas.width / rect.width),
        y: (e.clientY - rect.top) * (canvas.height / rect.height)
    };
}

function drawLineLocal(x1, y1, x2, y2, color, width) {
    ctx.strokeStyle = color;
    ctx.lineWidth = width;
    ctx.lineCap = 'round';
    ctx.beginPath();
    ctx.moveTo(x1, y1);
    ctx.lineTo(x2, y2);
    ctx.stroke();
}

function sendStroke(x1, y1, x2, y2, color, width) {
    const p = new URLSearchParams();
    p.append('action', 'draw');
    p.append('roomId', roomId);
    p.append('x1', x1); p.append('y1', y1);
    p.append('x2', x2); p.append('y2', y2);
    p.append('color', color);
    p.append('lineWidth', width);
    fetch('GameServlet', { method: 'POST', body: p });
}

function clearCanvas() {
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    const p = new URLSearchParams();
    p.append('action', 'clear');
    p.append('roomId', roomId);
    fetch('GameServlet', { method: 'POST', body: p });
}

function nextRound() {
    const p = new URLSearchParams();
    p.append('action', 'nextRound');
    p.append('roomId', roomId);
    fetch('GameServlet', { method: 'POST', body: p }).then(() => {
        document.getElementById('roundEndBanner').style.display = 'none';
        resetTimer();
    });
}

// ---------------- Chat / Guessing ----------------

function sendGuess(e) {
    e.preventDefault();
    const input = document.getElementById('chatInput');
    const text = input.value.trim();
    if (!text) return false;
    input.value = '';

    if (isDrawer) return false; // drawer doesn't guess

    const p = new URLSearchParams();
    p.append('roomId', roomId);
    p.append('playerId', playerId);
    p.append('guess', text);
    fetch('GuessServlet', { method: 'POST', body: p });
    return false;
}

function appendChatMessage(msg) {
    const box = document.getElementById('chatBox');
    const div = document.createElement('div');
    if (msg.isCorrect) {
        div.className = 'correct';
        div.textContent = '✓ ' + msg.message;
    } else {
        div.textContent = msg.playerName + ': ' + msg.message;
    }
    box.appendChild(div);
    box.scrollTop = box.scrollHeight;
}

// ---------------- Polling ----------------

function poll() {
    const p = `GameServlet?roomId=${roomId}&playerId=${playerId}` +
               `&lastStrokeId=${lastStrokeId}&lastMessageId=${lastMessageId}`;
    fetch(p)
        .then(r => r.json())
        .then(data => {
            if (!data.success) return;

            renderPlayers(data.players);

            isDrawer = data.isDrawer;
            document.getElementById('toolbar').style.display = isDrawer ? 'flex' : 'none';
            document.getElementById('chatInput').placeholder = isDrawer ? 'You are drawing!' : 'Type your guess...';
            document.getElementById('chatInput').disabled = isDrawer;

            document.getElementById('wordDisplay').textContent = data.wordDisplay || '';
            document.getElementById('roundInfo').textContent = 'Round ' + data.roundNumber;

            if (data.roundNumber !== currentRoundNumber) {
                currentRoundNumber = data.roundNumber;
                ctx.clearRect(0, 0, canvas.width, canvas.height);
                resetTimer();
                document.getElementById('roundEndBanner').style.display = 'none';
            }

            if (data.roomStatus === 'ROUND_END') {
                stopTimer();
                document.getElementById('roundEndBanner').style.display = 'block';
                document.getElementById('roundEndBanner').textContent =
                    'Round over! The word was: ' + data.wordDisplay;
            }

            // draw new strokes
            data.strokes.forEach(s => {
                if (s.id > lastStrokeId) lastStrokeId = s.id;
                if (s.isClear) {
                    ctx.clearRect(0, 0, canvas.width, canvas.height);
                } else if (!isDrawer) {
                    // drawer already drew it locally; everyone else renders it from server
                    drawLineLocal(s.x1, s.y1, s.x2, s.y2, s.color, s.lineWidth);
                }
            });

            // new chat messages
            data.messages.forEach(m => {
                if (m.id > lastMessageId) lastMessageId = m.id;
                appendChatMessage(m);
            });
        })
        .catch(() => {});
}

function renderPlayers(players) {
    const ul = document.getElementById('playerList');
    ul.innerHTML = '';
    players
        .slice()
        .sort((a, b) => b.score - a.score)
        .forEach(p => {
            const li = document.createElement('li');
            let label = p.name + ' - ' + p.score;
            if (p.isDrawer) label += ' ✏️';
            if (p.hasGuessed) label += ' ✅';
            if (String(p.id) === String(playerId)) label += ' (you)';
            li.textContent = label;
            ul.appendChild(li);
        });
}

// ---------------- Round timer (client-side, drawer drives progression) ----------------

function resetTimer() {
    stopTimer();
    secondsLeft = 60;
    document.getElementById('timer').textContent = secondsLeft + 's';
    roundTimerInterval = setInterval(() => {
        secondsLeft--;
        document.getElementById('timer').textContent = Math.max(secondsLeft, 0) + 's';
        if (secondsLeft <= 0) {
            stopTimer();
            if (isDrawer) nextRound();
        }
    }, 1000);
}

function stopTimer() {
    if (roundTimerInterval) clearInterval(roundTimerInterval);
    roundTimerInterval = null;
}

resetTimer();
poll();
setInterval(poll, 600);
