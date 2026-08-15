// Auth guard
document.addEventListener("DOMContentLoaded", function() {
    Auth.requireRole(["TEACHER"]);
    Auth.injectUserBadge();
});

const API_BASE_URL = 'http://localhost:8080/api';
let currentMaterialId = null;
let regeneratingQuizId = null;

function formatDateTime(dateValue) {
    const date = new Date(dateValue);
    if (Number.isNaN(date.getTime())) return 'Unknown time';
    return date.toLocaleString([], {
        year: 'numeric', month: 'short', day: '2-digit',
        hour: '2-digit', minute: '2-digit', second: '2-digit'
    });
}

function normalizeQuizTitleForDisplay(title) {
    const rawTitle = (title || '').trim();
    if (!rawTitle) return 'Untitled Quiz';
    if (rawTitle.toLowerCase().startsWith('quiz:')) {
        const legacyTitle = rawTitle.substring(5).trim();
        const extensionIndex = legacyTitle.lastIndexOf('.');
        if (extensionIndex > 0) return legacyTitle.substring(0, extensionIndex);
        return legacyTitle || 'Untitled Quiz';
    }
    return rawTitle;
}

document.addEventListener('DOMContentLoaded', () => {
    loadMaterials();
    loadQuizzes();
    loadAllStudentResults();
    setupFileUpload();
});

// ── File upload ────────────────────────────────────────────────────────────────
function setupFileUpload() {
    const uploadArea = document.getElementById('uploadArea');
    const fileInput  = document.getElementById('fileInput');

    uploadArea.addEventListener('dragover', (e) => { e.preventDefault(); uploadArea.classList.add('dragover'); });
    uploadArea.addEventListener('dragleave', () => uploadArea.classList.remove('dragover'));
    uploadArea.addEventListener('drop', (e) => {
        e.preventDefault();
        uploadArea.classList.remove('dragover');
        if (e.dataTransfer.files.length > 0) handleFileUpload(e.dataTransfer.files[0]);
    });
    fileInput.addEventListener('change', (e) => {
        if (e.target.files.length > 0) handleFileUpload(e.target.files[0]);
    });
}

async function handleFileUpload(file) {
    const uploadStatus = document.getElementById('uploadStatus');
    const validExtensions = ['txt', 'pdf', 'docx'];
    const fileExtension = file.name.split('.').pop().toLowerCase();

    if (!validExtensions.includes(fileExtension)) {
        showMessage(uploadStatus, 'error', 'Invalid file type. Please upload TXT, PDF, or DOCX files.');
        return;
    }
    if (file.size > 10 * 1024 * 1024) {
        showMessage(uploadStatus, 'error', 'File size exceeds 10MB limit.');
        return;
    }

    showMessage(uploadStatus, 'info', 'Uploading and processing file...');

    try {
        const formData = new FormData();
        formData.append('file', file);

        const response = await fetch(`${API_BASE_URL}/materials/upload`, { method: 'POST', body: formData });
        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.error || 'Upload failed');
        }

        const material = await response.json();
        const extractedLength = (material.extractedText || '').length;
        showMessage(uploadStatus, 'success', `File uploaded successfully! Extracted ${extractedLength} characters.`);
        loadMaterials();
        document.getElementById('fileInput').value = '';

    } catch (error) {
        showMessage(uploadStatus, 'error', `Upload failed: ${error.message}`);
    }
}

// ── Materials ──────────────────────────────────────────────────────────────────
async function loadMaterials() {
    const loadingEl     = document.getElementById('materialsLoading');
    const listEl        = document.getElementById('materialsList');
    const noMaterialsEl = document.getElementById('noMaterials');

    try {
        const response  = await fetch(`${API_BASE_URL}/materials`);
        const materials = await response.json();

        loadingEl.classList.add('hidden');

        if (materials.length === 0) {
            listEl.classList.add('hidden');
            noMaterialsEl.classList.remove('hidden');
        } else {
            noMaterialsEl.classList.add('hidden');
            listEl.classList.remove('hidden');
            listEl.innerHTML = materials.map(material => `
                <li class="list-item">
                    <div>
                        <strong>${material.fileName}</strong>
                        <p style="color: var(--text-secondary); font-size: 0.9rem; margin-top: 0.25rem;">
                            Uploaded: ${new Date(material.uploadDate).toLocaleDateString()} |
                            ${(material.extractedText || '').length} characters
                        </p>
                    </div>
                    <div style="display: flex; gap: 0.5rem;">
                        <button class="btn btn-primary" onclick="openQuizModal(${material.id})">Generate Quiz</button>
                        <button class="btn" style="background: var(--secondary-gradient);" onclick="deleteMaterial(${material.id})">Delete</button>
                    </div>
                </li>
            `).join('');
        }
    } catch (error) {
        console.error('Error loading materials:', error);
        loadingEl.classList.add('hidden');
    }
}

// ── Quizzes ────────────────────────────────────────────────────────────────────
async function loadQuizzes() {
    const loadingEl  = document.getElementById('quizzesLoading');
    const listEl     = document.getElementById('quizzesList');
    const noQuizzesEl = document.getElementById('noQuizzes');

    try {
        // Teacher fetches all quizzes (no activeOnly filter)
        const response = await fetch(`${API_BASE_URL}/quiz`);
        const quizzes  = await response.json();

        loadingEl.classList.add('hidden');

        if (quizzes.length === 0) {
            listEl.classList.add('hidden');
            noQuizzesEl.classList.remove('hidden');
        } else {
            noQuizzesEl.classList.add('hidden');
            listEl.classList.remove('hidden');
            listEl.innerHTML = quizzes.map(quiz => {
                const isActive = quiz.active !== false; // default true for old records
                const statusBadge = isActive
                    ? `<span style="background:rgba(72,199,142,.2);border:1px solid rgba(72,199,142,.5);color:#48c78e;border-radius:999px;padding:.2rem .7rem;font-size:.75rem;font-weight:700;">● Active</span>`
                    : `<span style="background:rgba(255,99,110,.2);border:1px solid rgba(255,99,110,.5);color:#ff636e;border-radius:999px;padding:.2rem .7rem;font-size:.75rem;font-weight:700;">● Inactive</span>`;
                return `
                <li class="list-item">
                    <div style="flex:1;">
                        <div style="display:flex;align-items:center;gap:.6rem;flex-wrap:wrap;">
                            <strong>${normalizeQuizTitleForDisplay(quiz.title)}</strong>
                            ${statusBadge}
                        </div>
                        <p style="color: var(--text-secondary); font-size: 0.9rem; margin-top: 0.25rem;">
                            Created: ${formatDateTime(quiz.createdDate)} |
                            Method: <span class="method-chip">${quiz.generationMethod}</span>
                        </p>
                    </div>
                    <div style="display:flex;gap:.5rem;flex-wrap:wrap;justify-content:flex-end;">
                        <button class="btn btn-success" onclick="previewQuiz(${quiz.id})">Preview</button>
                        <button class="btn" style="background:rgba(255,171,0,.2);border:1px solid rgba(255,171,0,.4);color:#ffd166;"
                            onclick="openRegenerateModal(${quiz.id}, '${normalizeQuizTitleForDisplay(quiz.title).replace(/'/g,"\\'")}')">Re-generate</button>
                        <button class="btn" style="background:rgba(102,126,234,.15);border:1px solid rgba(102,126,234,.4);color:#a78bfa;"
                            onclick="toggleQuizStatus(${quiz.id}, this)">
                            ${isActive ? 'Deactivate' : 'Activate'}
                        </button>
                        <button class="btn" style="background:rgba(255,99,110,.15);border:1px solid rgba(255,99,110,.4);color:#ff636e;"
                            onclick="deleteQuiz(${quiz.id})">Delete</button>
                    </div>
                </li>`;
            }).join('');
        }
    } catch (error) {
        console.error('Error loading quizzes:', error);
        loadingEl.classList.add('hidden');
    }
}

// ── Toggle active/inactive ─────────────────────────────────────────────────────
async function toggleQuizStatus(quizId, btn) {
    try {
        const response = await fetch(`${API_BASE_URL}/quiz/${quizId}/status`, { method: 'PATCH' });
        if (!response.ok) throw new Error('Failed to update status');
        loadQuizzes(); // reload to reflect badge + button text changes
    } catch (error) {
        alert('Failed to update quiz status: ' + error.message);
    }
}

// ── Delete quiz ────────────────────────────────────────────────────────────────
async function deleteQuiz(quizId) {
    if (!confirm('Delete this quiz? All student attempts for this quiz will also be removed from the results.')) return;
    try {
        const response = await fetch(`${API_BASE_URL}/quiz/${quizId}`, { method: 'DELETE' });
        if (!response.ok) throw new Error('Delete failed');
        loadQuizzes();
        loadAllStudentResults();
    } catch (error) {
        alert('Failed to delete quiz: ' + error.message);
    }
}

// ── Re-generate modal ──────────────────────────────────────────────────────────
function openRegenerateModal(quizId, quizTitle) {
    regeneratingQuizId = quizId;
    document.getElementById('regenerateQuizName').textContent = quizTitle;
    document.getElementById('regenerateCount').value = '10';
    const modal = document.getElementById('regenerateModal');
    modal.classList.remove('hidden');
    modal.style.display = 'flex';
}

function closeRegenerateModal() {
    regeneratingQuizId = null;
    const modal = document.getElementById('regenerateModal');
    modal.classList.add('hidden');
    modal.style.display = 'none';
}

async function confirmRegenerate() {
    if (!regeneratingQuizId) return;
    const questionCount = document.getElementById('regenerateCount').value;
    closeRegenerateModal();

    const uploadStatus = document.getElementById('uploadStatus');
    showMessage(uploadStatus, 'info', 'Re-generating quiz... This may take a moment.');

    try {
        const response = await fetch(
            `${API_BASE_URL}/quiz/${regeneratingQuizId}/regenerate?questionCount=${questionCount}`,
            { method: 'POST' }
        );

        if (!response.ok) {
            const error = await response.json().catch(() => ({ error: 'Re-generation failed' }));
            throw new Error(error.error || 'Re-generation failed');
        }

        const result = await response.json();
        showMessage(uploadStatus, 'success',
            `Quiz re-generated successfully with ${result.questions.length} new questions using ${result.quiz.generationMethod} method.`);
        loadQuizzes();

    } catch (error) {
        showMessage(uploadStatus, 'error', `Re-generation failed: ${error.message}`);
    }
}

// ── Preview quiz ───────────────────────────────────────────────────────────────
async function previewQuiz(quizId) {
    try {
        const response = await fetch(`${API_BASE_URL}/quiz/${quizId}`);
        const data = await response.json();
        const questions = data.questions || [];

        const modal   = document.getElementById('previewModal');
        const title   = document.getElementById('previewQuizTitle');
        const body    = document.getElementById('previewQuestionsBody');

        title.textContent = normalizeQuizTitleForDisplay(data.quiz.title);
        body.innerHTML = questions.map((q, i) => `
            <div style="margin-bottom:1.5rem;padding:1rem;background:rgba(255,255,255,.04);border-radius:8px;border:1px solid var(--card-border);">
                <p style="font-weight:700;margin-bottom:.75rem;">Q${i+1}. ${q.questionText}</p>
                ${[1,2,3,4].map(n => {
                    const isCorrect = q.correctAnswer === n;
                    return `<div style="padding:.4rem .8rem;margin:.3rem 0;border-radius:6px;
                        background:${isCorrect ? 'rgba(72,199,142,.15)' : 'rgba(255,255,255,.04)'};
                        border:1px solid ${isCorrect ? 'rgba(72,199,142,.5)' : 'var(--card-border)'};
                        color:${isCorrect ? '#48c78e' : 'var(--text-primary)'};">
                        ${isCorrect ? '✓ ' : ''}${q['option'+n]}
                    </div>`;
                }).join('')}
            </div>
        `).join('');

        modal.classList.remove('hidden');
        modal.style.display = 'flex';
    } catch (error) {
        alert('Failed to load quiz preview: ' + error.message);
    }
}

function closePreviewModal() {
    const modal = document.getElementById('previewModal');
    modal.classList.add('hidden');
    modal.style.display = 'none';
}

// ── Student results ────────────────────────────────────────────────────────────
async function loadAllStudentResults() {
    const loadingEl  = document.getElementById('resultsLoading');
    const tableEl    = document.getElementById('resultsTable');
    const noResultsEl = document.getElementById('noResults');
    const tbodyEl    = document.getElementById('resultsTbody');

    try {
        // Fetch all attempts and all quizzes in parallel
        const [attemptsRes, quizzesRes] = await Promise.all([
            fetch(`${API_BASE_URL}/quiz/attempts/all`),
            fetch(`${API_BASE_URL}/quiz`)
        ]);

        const attempts = await attemptsRes.json();
        const quizzes  = await quizzesRes.json();

        // Build a quizId → title map
        const quizMap = {};
        quizzes.forEach(q => { quizMap[q.id] = normalizeQuizTitleForDisplay(q.title); });

        loadingEl.classList.add('hidden');

        if (attempts.length === 0) {
            tableEl.classList.add('hidden');
            noResultsEl.classList.remove('hidden');
            return;
        }

        noResultsEl.classList.add('hidden');
        tableEl.classList.remove('hidden');

        tbodyEl.innerHTML = attempts.map(a => {
            const pct     = ((a.score / a.totalQuestions) * 100).toFixed(1);
            const pctNum  = parseFloat(pct);
            const color   = pctNum >= 80 ? '#48c78e' : pctNum >= 50 ? '#ffd166' : '#ff636e';
            return `
            <tr style="border-bottom:1px solid var(--card-border);">
                <td style="padding:.6rem .8rem;">${a.studentName}</td>
                <td style="padding:.6rem .8rem;">${quizMap[a.quizId] || 'Quiz #' + a.quizId}</td>
                <td style="padding:.6rem .8rem;text-align:center;font-weight:700;color:${color};">
                    ${a.score}/${a.totalQuestions}
                </td>
                <td style="padding:.6rem .8rem;text-align:center;color:${color};">${pct}%</td>
                <td style="padding:.6rem .8rem;color:var(--text-secondary);font-size:.85rem;">${formatDateTime(a.attemptDate)}</td>
            </tr>`;
        }).join('');

    } catch (error) {
        console.error('Error loading student results:', error);
        loadingEl.classList.add('hidden');
    }
}

// ── Generate quiz modal ────────────────────────────────────────────────────────
function openQuizModal(materialId) {
    currentMaterialId = materialId;
    const modal      = document.getElementById('quizModal');
    const titleInput = document.getElementById('quizTitle');
    const countInput = document.getElementById('questionCount');

    if (titleInput) titleInput.value = '';
    if (countInput) countInput.value = '10';

    modal.classList.remove('hidden');
    modal.style.display = 'flex';
    if (titleInput) titleInput.focus();
}

function closeQuizModal(resetSelection = true) {
    const modal = document.getElementById('quizModal');
    modal.classList.add('hidden');
    modal.style.display = 'none';
    if (resetSelection) currentMaterialId = null;
}

async function confirmGenerateQuiz() {
    const questionCount      = document.getElementById('questionCount').value;
    const quizTitle          = (document.getElementById('quizTitle')?.value || '').trim();
    const selectedMaterialId = currentMaterialId;

    if (!selectedMaterialId) {
        showMessage(document.getElementById('uploadStatus'), 'error', 'Please select a study material first.');
        return;
    }
    if (quizTitle.length > 100) {
        showMessage(document.getElementById('uploadStatus'), 'error', 'Quiz title must be 100 characters or fewer.');
        return;
    }

    closeQuizModal(false);

    const uploadStatus = document.getElementById('uploadStatus');
    showMessage(uploadStatus, 'info', 'Generating quiz... This may take a moment.');

    try {
        const query = new URLSearchParams({ questionCount });
        if (quizTitle) query.append('quizTitle', quizTitle);

        const response = await fetch(`${API_BASE_URL}/quiz/generate/${selectedMaterialId}?${query.toString()}`, {
            method: 'POST'
        });

        if (!response.ok) {
            const error = await response.json().catch(() => ({ error: 'Quiz generation failed' }));
            throw new Error(error.error || 'Quiz generation failed');
        }

        const result = await response.json();
        showMessage(uploadStatus, 'success',
            `Quiz "${result.quiz.title}" generated with ${result.questions.length} questions using ${result.quiz.generationMethod} method.`);
        loadQuizzes();

    } catch (error) {
        showMessage(uploadStatus, 'error', `Quiz generation failed: ${error.message}`);
    } finally {
        currentMaterialId = null;
    }
}

async function deleteMaterial(materialId) {
    if (!confirm('Are you sure you want to delete this material?')) return;
    try {
        const response = await fetch(`${API_BASE_URL}/materials/${materialId}`, { method: 'DELETE' });
        if (response.ok) loadMaterials();
    } catch (error) {
        console.error('Error deleting material:', error);
    }
}

function showMessage(element, type, message) {
    const alertClass = type === 'error' ? 'alert-error' : type === 'info' ? 'alert-info' : 'alert-success';
    element.innerHTML = `<div class="alert ${alertClass}">${message}</div>`;
    element.classList.remove('hidden');
}
