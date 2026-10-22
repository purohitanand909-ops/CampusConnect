/**
 * CampusConnect — Client Application
 * Communicates with Spring Boot REST APIs using fetch() + HTTP Basic Auth.
 */

// ── Application State ──────────────────────────────────────────
const state = {
    user: null,          // { id, name, email, role }
    authHeader: null,    // "Basic base64(...)"
    currentTab: 'events-tab',
    events: [],
    registrations: []
};

const API_BASE = '/api/v1';

// ── Initialization ────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
    restoreSession();
    initTabNavigation();
    initEventListeners();
    loadEvents();
});

function restoreSession() {
    const savedUser = sessionStorage.getItem('cc_user');
    const savedAuth = sessionStorage.getItem('cc_auth');
    if (savedUser && savedAuth) {
        state.user = JSON.parse(savedUser);
        state.authHeader = savedAuth;
        updateUserUI();
    }
}

// ── Event Listeners ───────────────────────────────────────────
function initEventListeners() {
    // Search & Filter
    document.getElementById('search-btn').addEventListener('click', () => {
        const keyword = document.getElementById('search-keyword').value.trim();
        const category = document.getElementById('category-filter').value;
        loadEvents(keyword, category);
    });

    document.getElementById('search-keyword').addEventListener('keyup', (e) => {
        if (e.key === 'Enter') {
            const keyword = e.target.value.trim();
            const category = document.getElementById('category-filter').value;
            loadEvents(keyword, category);
        }
    });

    document.getElementById('category-filter').addEventListener('change', (e) => {
        const keyword = document.getElementById('search-keyword').value.trim();
        loadEvents(keyword, e.target.value);
    });

    document.getElementById('refresh-events-btn').addEventListener('click', () => {
        loadEvents();
        showToast('Events list refreshed', 'success');
    });

    document.getElementById('refresh-my-regs-btn').addEventListener('click', () => {
        loadMyRegistrations();
        showToast('Registrations refreshed', 'success');
    });

    // Auth Modals & Forms
    document.getElementById('open-login-btn').addEventListener('click', () => {
        openModal('login-modal');
    });

    document.getElementById('logout-btn').addEventListener('click', logout);

    document.getElementById('login-form').addEventListener('submit', (e) => {
        e.preventDefault();
        const email = document.getElementById('login-email').value.trim();
        const password = document.getElementById('login-password').value;
        login(email, password);
    });

    // Create Event Form
    document.getElementById('create-event-form').addEventListener('submit', handleCreateEvent);
}

// ── Tab Navigation ────────────────────────────────────────────
function initTabNavigation() {
    document.querySelectorAll('.nav-tab').forEach(tab => {
        tab.addEventListener('click', () => {
            const targetId = tab.getAttribute('data-tab');
            switchTab(targetId);
        });
    });
}

function switchTab(tabId) {
    document.querySelectorAll('.nav-tab').forEach(t => t.classList.remove('active'));
    document.querySelectorAll('.tab-pane').forEach(p => p.classList.remove('active'));

    const activeTab = document.querySelector(`[data-tab="${tabId}"]`);
    const activePane = document.getElementById(tabId);
    if (activeTab && activePane) {
        activeTab.classList.add('active');
        activePane.classList.add('active');
        state.currentTab = tabId;

        if (tabId === 'my-registrations-tab') {
            loadMyRegistrations();
        } else if (tabId === 'events-tab') {
            loadEvents();
        }
    }
}

// ── Authentication ────────────────────────────────────────────
async function login(email, password) {
    const credentials = btoa(`${email}:${password}`);
    const authHeader = `Basic ${credentials}`;

    try {
        const res = await fetch(`${API_BASE}/auth/me`, {
            headers: {
                'Authorization': authHeader
            }
        });

        if (!res.ok) {
            showToast('Invalid email or password.', 'error');
            return;
        }

        const data = await res.json();
        if (data.success && data.data) {
            state.user = data.data;
            state.authHeader = authHeader;

            sessionStorage.setItem('cc_user', JSON.stringify(state.user));
            sessionStorage.setItem('cc_auth', authHeader);

            closeModal('login-modal');
            updateUserUI();
            showToast(`Welcome back, ${state.user.name}!`, 'success');
            loadEvents();
        } else {
            showToast(data.message || 'Authentication failed', 'error');
        }
    } catch (err) {
        console.error('Login error:', err);
        showToast('Network error during login.', 'error');
    }
}

function quickLogin(type) {
    if (type === 'organizer') {
        login('priya.sharma@campus.edu', 'password');
    } else {
        login('anand.purohit@student.edu', 'password');
    }
}

function logout() {
    state.user = null;
    state.authHeader = null;
    sessionStorage.removeItem('cc_user');
    sessionStorage.removeItem('cc_auth');
    updateUserUI();
    switchTab('events-tab');
    showToast('Logged out successfully.', 'success');
    loadEvents();
}

function updateUserUI() {
    const loggedOutView = document.getElementById('logged-out-view');
    const loggedInView = document.getElementById('logged-in-view');
    const studentTabs = document.querySelectorAll('.student-only');
    const organizerTabs = document.querySelectorAll('.organizer-only');

    if (state.user) {
        loggedOutView.style.display = 'none';
        loggedInView.style.display = 'flex';

        document.getElementById('user-name').textContent = state.user.name;
        const roleBadge = document.getElementById('user-role-badge');
        roleBadge.textContent = state.user.role;
        roleBadge.className = `badge ${state.user.role === 'ORGANIZER' ? 'badge-organizer' : 'badge-student'}`;

        if (state.user.role === 'STUDENT') {
            studentTabs.forEach(el => el.style.display = 'inline-block');
            organizerTabs.forEach(el => el.style.display = 'none');
        } else if (state.user.role === 'ORGANIZER') {
            studentTabs.forEach(el => el.style.display = 'none');
            organizerTabs.forEach(el => el.style.display = 'inline-block');
        }
    } else {
        loggedOutView.style.display = 'flex';
        loggedInView.style.display = 'none';
        studentTabs.forEach(el => el.style.display = 'none');
        organizerTabs.forEach(el => el.style.display = 'none');
    }
}

// ── Events API ────────────────────────────────────────────────
async function loadEvents(keyword = '', category = '') {
    const grid = document.getElementById('events-grid');
    const countLabel = document.getElementById('events-count-label');
    grid.innerHTML = '<div class="empty-state"><p>Loading events...</p></div>';

    let url = `${API_BASE}/events`;
    const params = new URLSearchParams();
    if (keyword) params.append('keyword', keyword);
    if (category) params.append('category', category);
    if (params.toString()) url += `?${params.toString()}`;

    try {
        const res = await fetch(url);
        const data = await res.json();

        if (data.success) {
            state.events = data.data || [];
            countLabel.textContent = `${state.events.length} active event${state.events.length === 1 ? '' : 's'} available`;
            renderEventsGrid();
        } else {
            grid.innerHTML = `<div class="empty-state"><p>Error loading events: ${data.message}</p></div>`;
        }
    } catch (err) {
        console.error('Error loading events:', err);
        grid.innerHTML = `<div class="empty-state"><p>Failed to connect to server.</p></div>`;
    }
}

function renderEventsGrid() {
    const grid = document.getElementById('events-grid');
    if (!state.events || state.events.length === 0) {
        grid.innerHTML = `
            <div class="empty-state" style="grid-column: 1 / -1;">
                <div class="empty-state-icon">📅</div>
                <h3>No Upcoming Events Found</h3>
                <p>Try clearing your search filters or check back later for new events.</p>
            </div>
        `;
        return;
    }

    grid.innerHTML = state.events.map(event => {
        const startFormatted = formatDate(event.startTime);
        const deadlineFormatted = formatDate(event.registrationDeadline);
        const isFull = event.availableSpots <= 0;
        const fillPercentage = Math.min(100, Math.round(((event.capacity - event.availableSpots) / event.capacity) * 100));

        let actionsHtml = `<button class="btn btn-outline btn-sm" onclick="showEventDetails(${event.id})">Details</button>`;

        if (state.user && state.user.role === 'STUDENT') {
            if (isFull) {
                actionsHtml += `<button class="btn btn-secondary btn-sm" onclick="registerStudent(${event.id})">Join Waitlist</button>`;
            } else {
                actionsHtml += `<button class="btn btn-primary btn-sm" onclick="registerStudent(${event.id})">Register</button>`;
            }
        } else if (state.user && state.user.role === 'ORGANIZER') {
            actionsHtml += `<button class="btn btn-secondary btn-sm" onclick="viewParticipants(${event.id}, '${escapeHtml(event.title)}')">Participants</button>`;
            actionsHtml += `<button class="btn btn-danger btn-sm" onclick="cancelEvent(${event.id})">Cancel</button>`;
        } else {
            actionsHtml += `<button class="btn btn-secondary btn-sm" onclick="openModal('login-modal')">Log In to Register</button>`;
        }

        return `
            <div class="event-card">
                <div class="event-card-top">
                    <div class="event-card-header">
                        <span class="event-category-badge">${escapeHtml(event.category)}</span>
                        <span class="badge ${isFull ? 'badge-waitlisted' : 'badge-confirmed'}">
                            ${isFull ? 'FULL (WAITLIST)' : `${event.availableSpots} SPOTS LEFT`}
                        </span>
                    </div>

                    <h3 class="event-card-title">${escapeHtml(event.title)}</h3>
                    <p class="event-card-desc">${escapeHtml(event.description || 'No description provided.')}</p>

                    <div class="event-meta">
                        <div class="meta-row">
                            <span>📍</span> <strong>${escapeHtml(event.location)}</strong>
                        </div>
                        <div class="meta-row">
                            <span>🕒</span> <span>${startFormatted}</span>
                        </div>
                        <div class="meta-row">
                            <span>⏳</span> <span>Deadline: ${deadlineFormatted}</span>
                        </div>
                        <div class="meta-row">
                            <span>👤</span> <span>Organizer: ${escapeHtml(event.organizerName)}</span>
                        </div>
                    </div>

                    <div class="spots-bar-wrapper">
                        <div class="spots-label-row">
                            <span>Capacity: ${event.capacity - event.availableSpots} / ${event.capacity} filled</span>
                            <span>${event.availableSpots} remaining</span>
                        </div>
                        <div class="spots-progress">
                            <div class="spots-fill ${isFull ? 'full' : ''}" style="width: ${fillPercentage}%"></div>
                        </div>
                    </div>
                </div>

                <div class="event-card-actions">
                    ${actionsHtml}
                </div>
            </div>
        `;
    }).join('');
}

// ── Registration API (Student) ────────────────────────────────
async function registerStudent(eventId) {
    if (!state.user || state.user.role !== 'STUDENT') {
        showToast('Please log in as a student to register.', 'error');
        openModal('login-modal');
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/registrations`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': state.authHeader
            },
            body: JSON.stringify({
                studentId: state.user.id,
                eventId: eventId
            })
        });

        const data = await res.json();
        if (res.ok && data.success) {
            const reg = data.data;
            if (reg.status === 'CONFIRMED') {
                showToast(`🎉 Registration Confirmed for "${reg.eventTitle}"!`, 'success');
            } else if (reg.status === 'WAITLISTED') {
                showToast(`⚠️ Event is full. Added to Waitlist at Position #${reg.waitlistPosition}!`, 'warning');
            }
            loadEvents();
        } else {
            showToast(data.message || 'Registration failed.', 'error');
        }
    } catch (err) {
        console.error('Registration error:', err);
        showToast('Network error during registration.', 'error');
    }
}

async function loadMyRegistrations() {
    if (!state.user || state.user.role !== 'STUDENT') return;

    const list = document.getElementById('registrations-list');
    list.innerHTML = '<div class="empty-state"><p>Loading your registrations...</p></div>';

    try {
        const res = await fetch(`${API_BASE}/students/${state.user.id}/registrations`, {
            headers: {
                'Authorization': state.authHeader
            }
        });
        const data = await res.json();

        if (data.success) {
            state.registrations = data.data || [];
            renderRegistrationsList();
        } else {
            list.innerHTML = `<div class="empty-state"><p>Error: ${data.message}</p></div>`;
        }
    } catch (err) {
        console.error('Error loading registrations:', err);
        list.innerHTML = `<div class="empty-state"><p>Could not load registrations.</p></div>`;
    }
}

function renderRegistrationsList() {
    const list = document.getElementById('registrations-list');
    if (!state.registrations || state.registrations.length === 0) {
        list.innerHTML = `
            <div class="empty-state">
                <div class="empty-state-icon">🎟️</div>
                <h3>No Active Registrations</h3>
                <p>Browse events and register to attend workshops, hackathons, and festivals!</p>
            </div>
        `;
        return;
    }

    list.innerHTML = state.registrations.map(reg => {
        const isCancelled = reg.status === 'CANCELLED';
        let statusBadge = '';
        if (reg.status === 'CONFIRMED') {
            statusBadge = `<span class="badge badge-confirmed">CONFIRMED</span>`;
        } else if (reg.status === 'WAITLISTED') {
            statusBadge = `<span class="badge badge-waitlisted">WAITLIST #${reg.waitlistPosition}</span>`;
        } else {
            statusBadge = `<span class="badge badge-cancelled">CANCELLED</span>`;
        }

        const registeredDate = formatDate(reg.registeredAt);

        return `
            <div class="registration-item">
                <div class="reg-info">
                    <div style="display:flex; align-items:center; gap:0.6rem; margin-bottom:0.3rem;">
                        <h3>${escapeHtml(reg.eventTitle)}</h3>
                        ${statusBadge}
                    </div>
                    <div class="reg-meta">
                        <span>Registration ID: #${reg.id}</span>
                        <span>Registered on: ${registeredDate}</span>
                    </div>
                </div>
                <div>
                    ${!isCancelled ? `<button class="btn btn-danger btn-sm" onclick="cancelRegistration(${reg.id})">Cancel Registration</button>` : '<span style="color:var(--text-muted); font-size:0.85rem;">Cancelled</span>'}
                </div>
            </div>
        `;
    }).join('');
}

async function cancelRegistration(registrationId) {
    if (!confirm('Are you sure you want to cancel this registration? If confirmed, your spot will be given to the next student in the waitlist.')) {
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/registrations/${registrationId}`, {
            method: 'DELETE',
            headers: {
                'Authorization': state.authHeader
            }
        });

        const data = await res.json();
        if (res.ok && data.success) {
            showToast('Registration cancelled. Seat offered to the next waitlisted student.', 'success');
            loadMyRegistrations();
            loadEvents();
        } else {
            showToast(data.message || 'Failed to cancel registration.', 'error');
        }
    } catch (err) {
        console.error('Cancel registration error:', err);
        showToast('Network error while cancelling.', 'error');
    }
}

// ── Organizer Operations ──────────────────────────────────────
async function handleCreateEvent(e) {
    e.preventDefault();

    if (!state.user || state.user.role !== 'ORGANIZER') {
        showToast('Unauthorized: Organizer account required.', 'error');
        return;
    }

    const title = document.getElementById('event-title').value.trim();
    const category = document.getElementById('event-category').value;
    const capacity = parseInt(document.getElementById('event-capacity').value, 10);
    const location = document.getElementById('event-location').value.trim();
    const startTime = document.getElementById('event-start-time').value;
    const registrationDeadline = document.getElementById('event-deadline').value;
    const description = document.getElementById('event-description').value.trim();

    // Front-end date sanity check
    if (new Date(registrationDeadline) >= new Date(startTime)) {
        showToast('Registration deadline must be before the event start time.', 'error');
        return;
    }

    const payload = {
        title,
        description,
        category,
        location,
        startTime,
        registrationDeadline,
        capacity,
        organizerId: state.user.id
    };

    try {
        const res = await fetch(`${API_BASE}/events`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': state.authHeader
            },
            body: JSON.stringify(payload)
        });

        const data = await res.json();
        if (res.ok && data.success) {
            showToast(`Event "${title}" created successfully!`, 'success');
            document.getElementById('create-event-form').reset();
            switchTab('events-tab');
            loadEvents();
        } else {
            showToast(data.message || 'Failed to create event.', 'error');
        }
    } catch (err) {
        console.error('Create event error:', err);
        showToast('Network error while creating event.', 'error');
    }
}

async function cancelEvent(eventId) {
    if (!confirm('Are you sure you want to cancel this event? This action will mark it as cancelled for all students.')) {
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/events/${eventId}`, {
            method: 'DELETE',
            headers: {
                'Authorization': state.authHeader
            }
        });

        const data = await res.json();
        if (res.ok && data.success) {
            showToast('Event has been cancelled.', 'success');
            loadEvents();
        } else {
            showToast(data.message || 'Failed to cancel event.', 'error');
        }
    } catch (err) {
        console.error('Cancel event error:', err);
        showToast('Network error while cancelling event.', 'error');
    }
}

async function viewParticipants(eventId, eventTitle) {
    document.getElementById('modal-event-title').textContent = `Participants: ${eventTitle}`;
    const body = document.getElementById('modal-event-body');
    body.innerHTML = '<p>Loading participant records...</p>';
    openModal('event-modal');

    try {
        const res = await fetch(`${API_BASE}/events/${eventId}/registrations`, {
            headers: {
                'Authorization': state.authHeader
            }
        });

        const data = await res.json();
        if (data.success) {
            const list = data.data || [];
            if (list.length === 0) {
                body.innerHTML = '<p style="color:var(--text-muted); padding:1rem 0;">No students have registered for this event yet.</p>';
                return;
            }

            body.innerHTML = `
                <table style="width:100%; border-collapse:collapse; font-size:0.88rem;">
                    <thead>
                        <tr style="text-align:left; border-bottom:2px solid var(--surface-border); color:var(--text-secondary);">
                            <th style="padding:0.6rem 0.4rem;">Student</th>
                            <th style="padding:0.6rem 0.4rem;">Email</th>
                            <th style="padding:0.6rem 0.4rem;">Status</th>
                            <th style="padding:0.6rem 0.4rem;">Registered At</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${list.map(r => `
                            <tr style="border-bottom:1px solid var(--surface-border);">
                                <td style="padding:0.6rem 0.4rem; font-weight:600;">${escapeHtml(r.studentName)}</td>
                                <td style="padding:0.6rem 0.4rem; color:var(--text-secondary);">${escapeHtml(r.studentEmail)}</td>
                                <td style="padding:0.6rem 0.4rem;">
                                    <span class="badge ${r.status === 'CONFIRMED' ? 'badge-confirmed' : (r.status === 'WAITLISTED' ? 'badge-waitlisted' : 'badge-cancelled')}">
                                        ${r.status === 'WAITLISTED' ? `WAITLIST #${r.waitlistPosition}` : r.status}
                                    </span>
                                </td>
                                <td style="padding:0.6rem 0.4rem; font-size:0.8rem; color:var(--text-muted);">${formatDate(r.registeredAt)}</td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            `;
        } else {
            body.innerHTML = `<p style="color:var(--danger)">Error: ${data.message}</p>`;
        }
    } catch (err) {
        console.error('Error loading participants:', err);
        body.innerHTML = `<p style="color:var(--danger)">Failed to load participants.</p>`;
    }
}

async function showEventDetails(eventId) {
    document.getElementById('modal-event-title').textContent = 'Event Details';
    const body = document.getElementById('modal-event-body');
    body.innerHTML = '<p>Loading event information...</p>';
    openModal('event-modal');

    try {
        const res = await fetch(`${API_BASE}/events/${eventId}`);
        const data = await res.json();

        if (data.success && data.data) {
            const ev = data.data;
            body.innerHTML = `
                <div style="display:flex; flex-direction:column; gap:1rem;">
                    <div>
                        <span class="event-category-badge">${escapeHtml(ev.category)}</span>
                        <h2 style="font-size:1.4rem; margin-top:0.4rem; font-weight:800;">${escapeHtml(ev.title)}</h2>
                    </div>

                    <p style="color:var(--text-secondary); white-space:pre-line;">${escapeHtml(ev.description || 'No description provided.')}</p>

                    <div class="event-meta" style="margin:0;">
                        <div class="meta-row"><span>📍</span> <strong>Venue:</strong> ${escapeHtml(ev.location)}</div>
                        <div class="meta-row"><span>🕒</span> <strong>Event Time:</strong> ${formatDate(ev.startTime)}</div>
                        <div class="meta-row"><span>⏳</span> <strong>Registration Closes:</strong> ${formatDate(ev.registrationDeadline)}</div>
                        <div class="meta-row"><span>👤</span> <strong>Organizer:</strong> ${escapeHtml(ev.organizerName)}</div>
                        <div class="meta-row"><span>🎟️</span> <strong>Capacity:</strong> ${ev.capacity} (${ev.availableSpots} spots available)</div>
                    </div>
                </div>
            `;
        } else {
            body.innerHTML = `<p style="color:var(--danger)">Could not load event details.</p>`;
        }
    } catch (err) {
        console.error('Error fetching details:', err);
        body.innerHTML = `<p style="color:var(--danger)">Network error fetching details.</p>`;
    }
}

// ── Helpers & UI Utilities ────────────────────────────────────
function openModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) modal.classList.add('active');
}

function closeModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) modal.classList.remove('active');
}

window.addEventListener('click', (e) => {
    if (e.target.classList.contains('modal')) {
        e.target.classList.remove('active');
    }
});

function showToast(message, type = 'info') {
    const container = document.getElementById('toast-container');
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.textContent = message;

    container.appendChild(toast);
    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transition = 'opacity 0.3s';
        setTimeout(() => toast.remove(), 300);
    }, 4000);
}

function formatDate(dateStr) {
    if (!dateStr) return 'N/A';
    try {
        const d = new Date(dateStr);
        return d.toLocaleDateString('en-US', {
            month: 'short',
            day: 'numeric',
            year: 'numeric',
            hour: 'numeric',
            minute: '2-digit'
        });
    } catch (e) {
        return dateStr;
    }
}

function escapeHtml(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}
