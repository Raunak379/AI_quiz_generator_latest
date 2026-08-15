// Auth guard
document.addEventListener("DOMContentLoaded", function() {
    Auth.requireRole(["STUDENT", "TEACHER"]);
    Auth.injectUserBadge();
});

const API_BASE_URL = 'http://localhost:8080/api';
let currentQuiz = null;
let questions   = [];
let answers     = {};

// Get logged-in username from session (set at login)
function getStudentName() {
    const user = Auth.getUser();
    return user ? (user.fullName || user.username) : '';
}

document.addEventListener('DOMContentLoaded', () => {
    // Show greeting with the logged-in name
    const nameEl = document.getElementById('welcomeName');
    if (nameEl) nameEl.textContent = getStudentName();

    loadAvailableQuizzes();
    loadMyPastAttempts();
});

// ── Load quizzes (only active ones) ───────────────────────────────────────────
async function loadAvailableQuizzes() {
    const selectEl = document.getElementById('quizSelect');
    try {
        // activeOnly=true so inactive quizzes don't appear for students
        const response = await fetch(`${API_BASE_URL}/quiz?activeOnly=true`);
        const quizzes  = await response.json();

        if (quizzes.length === 0) {
            selectEl.innerHTML = '<option value="">No quizzes available</option>';
        } else {
            selectEl.innerHTML = '<option value="">-- Select a quiz --</option>' +
                quizzes.map(quiz => `<option value="${quiz.id}">${quiz.title}</option>`).join('');
        }
    } catch (error) {
        console.error('Error loading quizzes:', error);
        selectEl.innerHTML = '<option value="">Error loading quizzes</option>';
    }
}

// ── Start quiz ─────────────────────────────────────────────────────────────────
async function startQuiz() {
    const studentName = getStudentName();
    const quizId      = document.getElementById('quizSelect').value;

    if (!studentName) {
        alert('Could not read your username. Please log out and log in again.');
        return;
    }
    if (!quizId) {
        alert('Please select a quiz');
        return;
    }

    try {
        const response = await fetch(`${API_BASE_URL}/quiz/${quizId}`);
        const data     = await response.json();

        currentQuiz = data.quiz;
        questions   = data.questions;
        answers     = {};

        document.getElementById('quizSelection').classList.add('hidden');
        document.getElementById('quizContainer').classList.remove('hidden');
        document.getElementById('quizTitle').textContent        = currentQuiz.title;
        document.getElementById('displayStudentName').textContent = studentName;
        document.getElementById('totalQuestions').textContent   = questions.length;

        renderQuestions();

    } catch (error) {
        console.error('Error loading quiz:', error);
        alert('Failed to load quiz. Please try again.');
    }
}

// ── Render questions ───────────────────────────────────────────────────────────
function renderQuestions() {
    const container = document.getElementById('questionsContainer');
    container.innerHTML = questions.map((question, index) => `
        <div class="question-card fade-in">
            <span class="question-number">Question ${index + 1}</span>
            <h3 style="margin: 1rem 0;">${question.questionText}</h3>
            <div class="options">
                <label class="option" onclick="selectAnswer(${question.id}, 1, this)">
                    <input type="radio" name="question-${question.id}" value="1">
                    <span>${question.option1}</span>
                </label>
                <label class="option" onclick="selectAnswer(${question.id}, 2, this)">
                    <input type="radio" name="question-${question.id}" value="2">
                    <span>${question.option2}</span>
                </label>
                <label class="option" onclick="selectAnswer(${question.id}, 3, this)">
                    <input type="radio" name="question-${question.id}" value="3">
                    <span>${question.option3}</span>
                </label>
                <label class="option" onclick="selectAnswer(${question.id}, 4, this)">
                    <input type="radio" name="question-${question.id}" value="4">
                    <span>${question.option4}</span>
                </label>
            </div>
        </div>
    `).join('');
    updateProgress();
}

function selectAnswer(questionId, answerValue, element) {
    answers[questionId] = answerValue;
    const parentCard  = element.closest('.question-card');
    parentCard.querySelectorAll('.option').forEach(opt => opt.classList.remove('selected'));
    element.classList.add('selected');
    updateProgress();
}

function updateProgress() {
    document.getElementById('currentQuestion').textContent = Object.keys(answers).length;
}

// ── Submit quiz ────────────────────────────────────────────────────────────────
async function submitQuiz() {
    const studentName = getStudentName();

    if (Object.keys(answers).length < questions.length) {
        if (!confirm('You have not answered all questions. Submit anyway?')) return;
    }

    try {
        const response = await fetch(`${API_BASE_URL}/quiz/${currentQuiz.id}/attempt`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ studentName, answers })
        });

        if (!response.ok) throw new Error('Failed to submit quiz');

        const result = await response.json();
        showResults(result);
        loadMyPastAttempts(); // refresh history after submitting

    } catch (error) {
        console.error('Error submitting quiz:', error);
        alert('Failed to submit quiz. Please try again.');
    }
}

// ── Show result screen ─────────────────────────────────────────────────────────
function showResults(attempt) {
    document.getElementById('quizContainer').classList.add('hidden');
    document.getElementById('resultsContainer').classList.remove('hidden');

    document.getElementById('scoreDisplay').textContent     = attempt.score;
    document.getElementById('totalScore').textContent       = attempt.totalQuestions;
    document.getElementById('percentageDisplay').textContent = attempt.percentage.toFixed(1);
    document.getElementById('resultStudentName').textContent = attempt.studentName;
}

// ── Past attempts ──────────────────────────────────────────────────────────────
async function loadMyPastAttempts() {
    const studentName = getStudentName();
    if (!studentName) return;

    const section  = document.getElementById('historySection');
    const loadingEl = document.getElementById('historyLoading');
    const tableEl  = document.getElementById('historyTable');
    const noHistEl = document.getElementById('noHistory');
    const tbody    = document.getElementById('historyTbody');

    try {
        const [attemptsRes, quizzesRes] = await Promise.all([
            fetch(`${API_BASE_URL}/quiz/attempts/student/${encodeURIComponent(studentName)}`),
            fetch(`${API_BASE_URL}/quiz`)
        ]);

        const attempts = await attemptsRes.json();
        const quizzes  = await quizzesRes.json();

        // Build quizId → title map
        const quizMap = {};
        quizzes.forEach(q => { quizMap[q.id] = q.title; });

        loadingEl.classList.add('hidden');
        section.classList.remove('hidden');

        if (!attempts.length) {
            tableEl.classList.add('hidden');
            noHistEl.classList.remove('hidden');
            return;
        }

        noHistEl.classList.add('hidden');
        tableEl.classList.remove('hidden');

        // Sort newest first
        attempts.sort((a, b) => new Date(b.attemptDate) - new Date(a.attemptDate));

        tbody.innerHTML = attempts.map(a => {
            const pct   = ((a.score / a.totalQuestions) * 100).toFixed(1);
            const color = parseFloat(pct) >= 80 ? '#48c78e' : parseFloat(pct) >= 50 ? '#ffd166' : '#ff636e';
            const date  = new Date(a.attemptDate).toLocaleString([], {
                year: 'numeric', month: 'short', day: '2-digit',
                hour: '2-digit', minute: '2-digit'
            });
            return `
            <tr style="border-bottom:1px solid var(--card-border);">
                <td style="padding:.6rem .8rem;">${quizMap[a.quizId] || 'Quiz #' + a.quizId}</td>
                <td style="padding:.6rem .8rem;text-align:center;font-weight:700;color:${color};">
                    ${a.score}/${a.totalQuestions}
                </td>
                <td style="padding:.6rem .8rem;text-align:center;color:${color};">${pct}%</td>
                <td style="padding:.6rem .8rem;color:var(--text-secondary);font-size:.85rem;">${date}</td>
            </tr>`;
        }).join('');

    } catch (error) {
        console.error('Error loading past attempts:', error);
        loadingEl.classList.add('hidden');
    }
}
