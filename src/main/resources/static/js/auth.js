/**
 * auth.js — shared authentication utilities
 * Include this script in teacher.html, student.html, leaderboard.html
 */

(function () {
    'use strict';

    // ── Public API ──────────────────────────────────────────────────────────────
    window.Auth = {
        getUser,
        logout,
        requireRole,
        injectUserBadge
    };

    function getUser() {
        try {
            const raw = sessionStorage.getItem('quizUser');
            return raw ? JSON.parse(raw) : null;
        } catch (_) {
            return null;
        }
    }

    function logout() {
        sessionStorage.removeItem('quizUser');
        window.location.href = '/login.html';
    }

    /**
     * Redirect to login if not logged in, or if the user's role is not in the
     * allowed list. Call at the top of each page's script.
     *
     * @param {string[]} allowedRoles  e.g. ['TEACHER'] or ['STUDENT', 'TEACHER']
     */
    function requireRole(allowedRoles) {
        const user = getUser();
        if (!user) {
            window.location.href = '/login.html';
            return;
        }
        if (allowedRoles && allowedRoles.length && !allowedRoles.includes(user.role)) {
            alert(`Access denied. This page is for ${allowedRoles.join('/')} only.`);
            window.location.href = '/';
        }
    }

    /**
     * Injects a small user-badge + logout link into an existing <nav> element.
     * Expects a <nav> on the page.
     */
    function injectUserBadge() {
        const user = getUser();
        const nav  = document.querySelector('nav');
        if (!nav || !user) return;

        // Remove existing sign-in link if present
        nav.querySelectorAll('a[href="/login.html"]').forEach(a => a.remove());

        const badge = document.createElement('span');
        badge.style.cssText = 'display:flex;align-items:center;gap:.6rem;';
        badge.innerHTML = `
            <span style="
                background:rgba(102,126,234,.2);
                border:1px solid rgba(102,126,234,.4);
                color:#c0b6f2;
                border-radius:999px;
                padding:.25rem .8rem;
                font-size:.8rem;
                font-weight:600;
            ">${user.role === 'TEACHER' ? '📚' : '✏️'} ${user.fullName || user.username}</span>
            <a href="#" onclick="Auth.logout()" style="
                color:var(--accent-pink);
                font-size:.85rem;
                font-weight:600;
                text-decoration:none;
            ">Sign Out</a>`;
        nav.appendChild(badge);
    }
})();
