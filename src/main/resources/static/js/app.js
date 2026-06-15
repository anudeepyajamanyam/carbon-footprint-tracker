const API_BASE_URL = '/api';

let currentAuthMode = 'LOGIN'; // LOGIN or REGISTER
let currentView = 'dashboard';
let _validationDebounceTimer = null;

// ----------------------------------------------------
// OFFLINE DETECTION
// ----------------------------------------------------
function checkOnlineStatus() {
    const offlineBanner = document.getElementById('offlineBanner');
    if (!navigator.onLine) {
        if (!offlineBanner) {
            const banner = document.createElement('div');
            banner.id = 'offlineBanner';
            banner.style.cssText = 'position:fixed;top:0;left:0;right:0;z-index:99999;background:#ef4444;color:#fff;text-align:center;padding:8px;font-size:0.88rem;font-weight:600;';
            banner.textContent = '⚠️ No internet connection. Some features may not work.';
            document.body.prepend(banner);
        }
    } else {
        if (offlineBanner) offlineBanner.remove();
    }
}
window.addEventListener('online', checkOnlineStatus);
window.addEventListener('offline', checkOnlineStatus);

// Interactive Tour configuration
let currentTourStep = 0;
const tourSteps = [
    {
        title: "Welcome to BiomeTrck! 🌿",
        text: "BiomeTrck is an interactive platform built to help you track carbon emissions, adopt green habits, and challenge friends. Let's take a quick walk through each tab!",
        view: "dashboard"
    },
    {
        title: "1. Insights Dashboard 📊",
        text: "This dashboard displays your net emissions this month, previous month comparisons, active goals, and your net category breakdown. As you log activities or complete habits, these charts update in real-time!",
        view: "dashboard"
    },
    {
        title: "2. Log History 📋",
        text: "Here you can see all your logged activities. Every action, including your daily completed habits (as carbon offsets), is logged here. Click '+ Log Activity' at the top right to record transport, food, energy, or waste.",
        view: "logs"
    },
    {
        title: "3. Eco-Challenges ⚡",
        text: "Commit to green habits like Meatless Monday or Switching to LEDs. Opt in, track your daily streak, and log completions. These generate offsets that decrease your carbon footprint!",
        view: "challenges"
    },
    {
        title: "4. Community & Leaderboards 👥",
        text: "Compare standings with the local community, earn ranking badges (from Eco Rookie to Carbon Free), and challenge your friends by sending them custom themed challenge invites!",
        view: "community"
    }
];

// Initialize on page load
document.addEventListener('DOMContentLoaded', () => {
    checkThemeState();
    checkSidebarState();
    checkAuthState();
    
    // Set default date in log modal to today
    const today = new Date().toISOString().split('T')[0];
    document.getElementById('logDate').value = today;
    
    // Set default month/year in goals view
    const currentMonth = new Date().getMonth() + 1;
    const currentYear = new Date().getFullYear();
    document.getElementById('goalMonth').value = currentMonth;
    document.getElementById('goalYear').value = currentYear;

    // Load initial categories for log modal
    onCategoryChange();
});

function checkAuthState() {
    const token = localStorage.getItem('biometrck_token');
    const username = localStorage.getItem('biometrck_username');
    const email = localStorage.getItem('biometrck_email');

    if (token) {
        const authContainer = document.getElementById('authContainer');
        if (authContainer) authContainer.style.display = 'none';
        
        const appContainer = document.getElementById('appContainer');
        if (appContainer) appContainer.style.display = 'grid';
        
        document.getElementById('profileUsername').textContent = username || 'User';
        document.getElementById('profileEmail').textContent = email || '';
        
        initCharts();
        loadWalkState();
        switchView(currentView);
        
        // Handle challenge invite redirects — post-login
        const pendingInvite = sessionStorage.getItem('pendingInvite');
        if (pendingInvite) {
            setTimeout(() => processPendingInvite(JSON.parse(pendingInvite)), 1000);
        } else {
            setTimeout(handleIncomingChallenge, 800);
        }
        
        // Auto-trigger tour if first time visit
        const tourCompleted = localStorage.getItem('tour_completed_' + username);
        if (!tourCompleted) {
            setTimeout(startTour, 900);
        }
    } else {
        window.location.href = '/login';
    }
}

function toggleAuthMode() {
    const emailGroup = document.getElementById('emailGroup');
    const authSubtitle = document.getElementById('authSubtitle');
    const authSubmitBtn = document.getElementById('authSubmitBtn');
    const authSwitchText = document.getElementById('authSwitchText');
    const authSwitchBtn = document.getElementById('authSwitchBtn');
    const tcGroup = document.getElementById('tcGroup');

    if (currentAuthMode === 'LOGIN') {
        currentAuthMode = 'REGISTER';
        emailGroup.style.display = 'block';
        document.getElementById('authEmail').required = true;
        authSubtitle.textContent = 'Create your account to start tracking emissions';
        authSubmitBtn.textContent = 'Create Account';
        authSwitchText.textContent = 'Already have an account? ';
        authSwitchBtn.textContent = 'Log In';
        if (tcGroup) tcGroup.style.display = 'block';
        // Password hint for register mode
        setFieldHint('authPasswordHint', 'At least 8 characters recommended', 'loading');
    } else {
        currentAuthMode = 'LOGIN';
        emailGroup.style.display = 'none';
        document.getElementById('authEmail').required = false;
        authSubtitle.textContent = 'Your journey to a sustainable footprint starts here';
        authSubmitBtn.textContent = 'Log In';
        authSwitchText.textContent = "Don't have an account? ";
        authSwitchBtn.textContent = 'Sign Up';
        if (tcGroup) tcGroup.style.display = 'none';
        // Clear all hints on switch back
        clearFieldHint('authUsernameHint');
        clearFieldHint('authEmailHint');
        clearFieldHint('authPasswordHint');
        setInputState('authUsername', '');
        setInputState('authEmail', '');
        setInputState('authPassword', '');
    }
}

async function handleAuthSubmit(e) {
    e.preventDefault();
    const username = document.getElementById('authUsername').value.trim();
    const password = document.getElementById('authPassword').value;
    const email = document.getElementById('authEmail').value.trim();

    // Client-side validation
    if (!username || username.length < 3) {
        setFieldHint('authUsernameHint', 'Username must be at least 3 characters', 'err');
        setInputState('authUsername', 'err');
        return;
    }
    if (!password || password.length < 8) {
        setFieldHint('authPasswordHint', 'Password must be at least 8 characters', 'err');
        setInputState('authPassword', 'err');
        return;
    }
    if (currentAuthMode === 'REGISTER') {
        if (!email || !email.includes('@')) {
            setFieldHint('authEmailHint', 'Please enter a valid email address', 'err');
            setInputState('authEmail', 'err');
            return;
        }
        const tcChecked = document.getElementById('tcCheckbox')?.checked;
        if (!tcChecked) {
            showToast('Please accept the Terms & Conditions to continue.', true);
            return;
        }
    }

    const payload = { username, password };
    if (currentAuthMode === 'REGISTER') {
        payload.email = email;
    }

    const endpoint = currentAuthMode === 'REGISTER' ? '/auth/register' : '/auth/login';
    const submitBtn = document.getElementById('authSubmitBtn');
    submitBtn.disabled = true;
    submitBtn.textContent = currentAuthMode === 'REGISTER' ? 'Creating...' : 'Logging in...';

    try {
        const response = await fetch(API_BASE_URL + endpoint, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload),
            signal: AbortSignal.timeout(10000)
        });

        const data = await safeJson(response);

        if (response.status === 409) {
            // User already exists — show redirect dialog
            const msg = data.error || 'An account with this username or email already exists.';
            showUserExistsDialog(msg);
            return;
        }

        if (!response.ok) {
            throw new Error(data.error || 'Authentication failed');
        }

        localStorage.setItem('biometrck_token', data.token);
        localStorage.setItem('biometrck_username', data.username);
        localStorage.setItem('biometrck_email', data.email);

        // Mark T&C accepted (first time only)
        if (currentAuthMode === 'REGISTER') {
            localStorage.setItem('tc_accepted_' + data.username, 'true');
        }
        
        showToast(currentAuthMode === 'REGISTER' ? 'Account created successfully! Welcome to BiomeTrck.' : 'Logged in successfully!');
        
        document.getElementById('authUsername').value = '';
        document.getElementById('authPassword').value = '';
        document.getElementById('authEmail').value = '';
        if (document.getElementById('tcCheckbox')) document.getElementById('tcCheckbox').checked = false;

        checkAuthState();
    } catch (err) {
        if (err.name === 'TimeoutError' || err.name === 'AbortError') {
            showToast('Request timed out. Please check your connection and try again.', true);
        } else if (err.message === 'User not registered') {
            showToast('Username not found. Redirecting to Sign Up...', false);
            if (currentAuthMode === 'LOGIN') {
                toggleAuthMode();
                document.getElementById('authUsername').value = username;
            }
        } else {
            showToast(err.message, true);
        }
    } finally {
        submitBtn.disabled = false;
        submitBtn.textContent = currentAuthMode === 'REGISTER' ? 'Create Account' : 'Log In';
    }
}

// ---- Real-time field validation helpers ----
function setFieldHint(id, msg, type) {
    const el = document.getElementById(id);
    if (!el) return;
    el.textContent = msg;
    el.className = 'field-hint' + (type ? ' hint-' + type : '');
}

function clearFieldHint(id) {
    const el = document.getElementById(id);
    if (!el) return;
    el.textContent = '';
    el.className = 'field-hint';
}

function setInputState(id, state) {
    const el = document.getElementById(id);
    if (!el) return;
    el.classList.remove('input-ok', 'input-err');
    if (state === 'ok') el.classList.add('input-ok');
    if (state === 'err') el.classList.add('input-err');
}

function onAuthUsernameInput(input) {
    const val = input.value.trim();
    clearTimeout(_validationDebounceTimer);
    if (val.length < 3) {
        setFieldHint('authUsernameHint', val.length > 0 ? 'Min 3 characters' : '', 'err');
        setInputState('authUsername', val.length > 0 ? 'err' : '');
        return;
    }
    if (!/^[a-zA-Z0-9_]+$/.test(val)) {
        setFieldHint('authUsernameHint', 'Only letters, numbers and underscores allowed', 'err');
        setInputState('authUsername', 'err');
        return;
    }
    if (currentAuthMode === 'REGISTER') {
        setFieldHint('authUsernameHint', 'Checking...', 'loading');
        _validationDebounceTimer = setTimeout(async () => {
            try {
                const res = await fetch(`${API_BASE_URL}/auth/check?username=${encodeURIComponent(val)}`);
                const data = await res.json();
                if (data.usernameTaken) {
                    setFieldHint('authUsernameHint', '✗ Username already taken', 'err');
                    setInputState('authUsername', 'err');
                } else {
                    setFieldHint('authUsernameHint', '✓ Available', 'ok');
                    setInputState('authUsername', 'ok');
                }
            } catch { clearFieldHint('authUsernameHint'); }
        }, 600);
    } else {
        clearFieldHint('authUsernameHint');
        setInputState('authUsername', 'ok');
    }
}

function onAuthEmailInput(input) {
    const val = input.value.trim();
    clearTimeout(_validationDebounceTimer);
    if (!val.includes('@') || val.length < 5) {
        setFieldHint('authEmailHint', val.length > 0 ? 'Enter a valid email' : '', 'err');
        setInputState('authEmail', val.length > 0 ? 'err' : '');
        return;
    }
    setFieldHint('authEmailHint', 'Checking...', 'loading');
    _validationDebounceTimer = setTimeout(async () => {
        try {
            const res = await fetch(`${API_BASE_URL}/auth/check?email=${encodeURIComponent(val)}`);
            const data = await res.json();
            if (data.emailTaken) {
                setFieldHint('authEmailHint', '✗ Email already registered — go to Login', 'err');
                setInputState('authEmail', 'err');
            } else {
                setFieldHint('authEmailHint', '✓ Email available', 'ok');
                setInputState('authEmail', 'ok');
            }
        } catch { clearFieldHint('authEmailHint'); }
    }, 600);
}

function onAuthPasswordInput(input) {
    const val = input.value;
    if (val.length === 0) { clearFieldHint('authPasswordHint'); setInputState('authPassword', ''); return; }
    if (val.length < 8) {
        setFieldHint('authPasswordHint', `${8 - val.length} more characters needed`, 'err');
        setInputState('authPassword', 'err');
    } else {
        const strong = val.length >= 12 && /[A-Z]/.test(val) && /[0-9]/.test(val);
        setFieldHint('authPasswordHint', strong ? '✓ Strong password' : '✓ Acceptable (add numbers/uppercase for stronger)', 'ok');
        setInputState('authPassword', 'ok');
    }
}

// ---- User Already Exists Dialog ----
function showUserExistsDialog(msg) {
    document.getElementById('userExistsMsg').textContent = msg;
    document.getElementById('userExistsModal').style.display = 'flex';
}

function closeUserExistsModal() {
    document.getElementById('userExistsModal').style.display = 'none';
}

function redirectToLogin() {
    closeUserExistsModal();
    if (currentAuthMode !== 'LOGIN') toggleAuthMode();
    // Pre-fill username field for convenience
    const uname = document.getElementById('authUsername').value;
    if (uname) document.getElementById('authUsername').value = uname;
}

// ---- Terms & Conditions Modal ----
function openTCModal() {
    document.getElementById('tcModal').style.display = 'flex';
}

function closeTCModal() {
    document.getElementById('tcModal').style.display = 'none';
}

function acceptTCFromModal() {
    const cb = document.getElementById('tcCheckbox');
    if (cb) cb.checked = true;
    closeTCModal();
    showToast('Terms accepted. Complete registration below.');
}

function handleLogout() {
    // Close all modal overlays
    document.querySelectorAll('.modal-overlay').forEach(modal => {
        modal.style.display = 'none';
    });
    
    localStorage.removeItem('biometrck_token');
    localStorage.removeItem('biometrck_username');
    localStorage.removeItem('biometrck_email');
    
    showToast('Logged out successfully.');
    checkAuthState();
}

// Safe response JSON parser to avoid "Unexpected end of JSON input" errors
async function safeJson(response) {
    try {
        const text = await response.text();
        if (!text || text.trim() === '') {
            return {};
        }
        return JSON.parse(text);
    } catch (e) {
        console.error("JSON parse failed. Response text was:", e);
        return {};
    }
}

// Global fetch helper with auth header injection + retry + timeout
async function authFetch(url, options = {}, _retryCount = 0) {
    const token = localStorage.getItem('biometrck_token');
    if (!token) {
        handleLogout();
        return;
    }

    if (!options.headers) options.headers = {};
    options.headers['Authorization'] = `Bearer ${token}`;
    options.headers['Content-Type'] = 'application/json';

    // 10-second timeout to prevent hung requests
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 10000);
    const optWithSignal = { ...options, signal: controller.signal };

    try {
        const response = await fetch(url, optWithSignal);
        clearTimeout(timeoutId);

        if (response.status === 401 || response.status === 403) {
            handleLogout();
            throw new Error('Session expired or unauthorized. Please log in again.');
        }

        // Retry on 5xx server errors (up to 2 times with exponential backoff)
        if (response.status >= 500 && _retryCount < 2) {
            const delay = _retryCount === 0 ? 500 : 1000;
            await new Promise(r => setTimeout(r, delay));
            return authFetch(url, options, _retryCount + 1);
        }

        return response;
    } catch (err) {
        clearTimeout(timeoutId);
        if (err.name === 'AbortError') {
            throw new Error('Request timed out. Please check your connection.');
        }
        // Retry on network errors too
        if (_retryCount < 2 && err.message !== 'Session expired or unauthorized. Please log in again.') {
            const delay = _retryCount === 0 ? 500 : 1000;
            await new Promise(r => setTimeout(r, delay));
            return authFetch(url, options, _retryCount + 1);
        }
        console.error('API Fetch Error:', err);
        throw err;
    }
}

function switchView(viewId) {
    currentView = viewId;
    
    // Reset workspace scroll to top to prevent cutting off headers/tiles
    const mainWorkspace = document.querySelector('main');
    if (mainWorkspace) {
        mainWorkspace.scrollTop = 0;
    }
    
    // Hide all views
    document.querySelectorAll('.app-view').forEach(view => {
        view.style.display = 'none';
    });

    // Remove active navigation class
    document.querySelectorAll('.nav-item').forEach(item => {
        item.classList.remove('active');
    });

    // Show target view
    const targetView = document.getElementById(`view-${viewId}`);
    if (targetView) {
        targetView.style.display = 'block';
    }

    // Set sidebar item active
    const navItem = document.getElementById(`nav-${viewId}`);
    if (navItem) {
        navItem.classList.add('active');
    }

    // Trigger data fetch for current view
    if (viewId === 'dashboard') {
        fetchDashboardData();
    } else if (viewId === 'logs') {
        fetchLogsHistory();
    } else if (viewId === 'challenges') {
        fetchChallenges();
    } else if (viewId === 'goals') {
        fetchGoalHistory();
    } else if (viewId === 'community') {
        fetchLeaderboard();
        updateInviteLink();
    }
}

// ----------------------------------------------------
// DASHBOARD LOGIC
// ----------------------------------------------------
async function fetchDashboardData() {
    try {
        const response = await authFetch(API_BASE_URL + '/insights/dashboard');
        if (!response.ok) throw new Error('Could not fetch dashboard metrics');
        const data = await safeJson(response);

        // Update basic metrics
        document.getElementById('metric-current-val').innerHTML = `${(data.currentMonthEmissions || 0).toFixed(1)}<span>kg</span>`;
        updateEmissionHealthBar(data.currentMonthEmissions || 0);
        document.getElementById('metric-prev-val').innerHTML = `${(data.previousMonthEmissions || 0).toFixed(1)}<span>kg</span>`;
        document.getElementById('metric-offset-val').innerHTML = `${(data.totalSavedCo2 || 0).toFixed(1)}<span>kg</span>`;
        
        if (data.currentMonthGoal) {
            document.getElementById('metric-goal-val').innerHTML = `${data.currentMonthGoal.toFixed(0)}<span>kg</span>`;
            document.getElementById('metric-goal-footer').textContent = 'Monthly footprint budget';
            
            // Goal progress calculations
            document.getElementById('progressEmissionsText').textContent = (data.currentMonthEmissions || 0).toFixed(1);
            document.getElementById('progressGoalText').textContent = data.currentMonthGoal.toFixed(0);
            
            const pct = Math.min(((data.currentMonthEmissions || 0) / data.currentMonthGoal) * 100, 100);
            const pb = document.getElementById('dashboardProgressBar');
            pb.style.width = `${pct}%`;
            
            const emissions = data.currentMonthEmissions || 0;
            const goal = data.currentMonthGoal;
            const remainder = (goal - emissions).toFixed(1);
            const over = (emissions - goal).toFixed(1);
            const usedPct = ((emissions / goal) * 100).toFixed(0);

            let statusLabel = '';
            let guidanceHtml = '';

            if (emissions > goal) {
                pb.classList.add('exceeded');
                statusLabel = '⚠️ Budget exceeded! Take action to reduce your footprint.';
                document.getElementById('goalProgressStatusLabel').style.color = 'var(--color-danger)';
                guidanceHtml = `<div class="smart-guidance-banner guidance-exceeded">
                    <span class="guidance-icon">🚨</span>
                    <div class="guidance-body">
                        <strong>You've exceeded your budget by ${over} kg CO₂</strong>
                        You are ${usedPct}% of your limit. Here's how to recover:
                        <div class="guidance-actions">
                            <span class="guidance-action-pill">🥗 Switch to vegetarian meals this week</span>
                            <span class="guidance-action-pill">🚶 Walk short trips under 2 km</span>
                            <span class="guidance-action-pill">🌿 Join an eco-challenge to offset</span>
                            <span class="guidance-action-pill">💡 Reduce standby energy use</span>
                        </div>
                    </div>
                </div>`;
            } else if (pct >= 80) {
                pb.classList.remove('exceeded');
                statusLabel = `⚡ Approaching your limit — ${remainder} kg remaining.`;
                document.getElementById('goalProgressStatusLabel').style.color = 'var(--color-warning)';
                guidanceHtml = `<div class="smart-guidance-banner guidance-warn">
                    <span class="guidance-icon">⚠️</span>
                    <div class="guidance-body">
                        <strong>You've used ${usedPct}% of your budget — slow down!</strong>
                        Only ${remainder} kg left. Avoid high-emission activities for the rest of the month.
                        <div class="guidance-actions">
                            <span class="guidance-action-pill">🚌 Use public transport</span>
                            <span class="guidance-action-pill">🌱 Try a vegan meal</span>
                            <span class="guidance-action-pill">🔌 Unplug unused devices</span>
                        </div>
                    </div>
                </div>`;
            } else if (pct < 50 && emissions > 0) {
                pb.classList.remove('exceeded');
                statusLabel = `✅ You're doing great! ${remainder} kg still available.`;
                document.getElementById('goalProgressStatusLabel').style.color = 'var(--color-eco)';
                guidanceHtml = `<div class="smart-guidance-banner guidance-ok">
                    <span class="guidance-icon">🌟</span>
                    <div class="guidance-body">
                        <strong>Excellent pace — only ${usedPct}% of budget used so far.</strong>
                        Keep your momentum going. Consider joining an eco-challenge to earn bonus offsets!
                        <div class="guidance-actions">
                            <span class="guidance-action-pill">🏆 Join an eco-challenge</span>
                            <span class="guidance-action-pill">🚶 Log your walking steps</span>
                        </div>
                    </div>
                </div>`;
            } else {
                pb.classList.remove('exceeded');
                statusLabel = `On track: ${remainder} kg CO₂ remaining this month.`;
                document.getElementById('goalProgressStatusLabel').style.color = 'var(--text-secondary)';
                guidanceHtml = `<div class="smart-guidance-banner guidance-ok">
                    <span class="guidance-icon">👍</span>
                    <div class="guidance-body">
                        <strong>You're on track for this month.</strong>
                        Maintain your habits. ${remainder} kg still available in your budget.
                    </div>
                </div>`;
            }

            document.getElementById('goalProgressStatusLabel').textContent = statusLabel;
            document.getElementById('smartGoalGuidance').innerHTML = guidanceHtml;
        } else {
            document.getElementById('metric-goal-val').innerHTML = `--<span>kg</span>`;
            document.getElementById('metric-goal-footer').textContent = 'No active monthly budget';
            
            document.getElementById('progressEmissionsText').textContent = (data.currentMonthEmissions || 0).toFixed(1);
            document.getElementById('progressGoalText').textContent = '--';
            document.getElementById('dashboardProgressBar').style.width = '0%';
            document.getElementById('goalProgressStatusLabel').textContent = 'Set a carbon budget under the Budgets tab to track progress.';
            document.getElementById('goalProgressStatusLabel').style.color = 'var(--text-secondary)';
            document.getElementById('smartGoalGuidance').innerHTML = `<div class="smart-guidance-banner guidance-no-goal">
                <span class="guidance-icon">🎯</span>
                <div class="guidance-body">
                    <strong>No active budget set.</strong>
                    Go to <strong>Budgets</strong> to set a monthly CO₂ target. This enables progress tracking and personalised guidance.
                </div>
            </div>`;
        }

        // Previous Month comparison text
        const current = data.currentMonthEmissions || 0;
        const prev = data.previousMonthEmissions || 0;
        const curFooter = document.getElementById('metric-current-footer');
        if (prev > 0) {
            const diffPct = ((current - prev) / prev) * 100;
            if (diffPct > 0) {
                curFooter.innerHTML = `<span class="metric-trend-up">▲ +${diffPct.toFixed(1)}%</span> vs last month`;
            } else {
                curFooter.innerHTML = `<span class="metric-trend-down">▼ ${Math.abs(diffPct).toFixed(1)}%</span> vs last month`;
            }
        } else {
            curFooter.textContent = 'First month of logging';
        }

        // Render Recent Logs Table (color negative emissions green)
        const recentBody = document.getElementById('recentLogsTableBody');
        if (data.recentLogs && data.recentLogs.length > 0) {
            recentBody.innerHTML = data.recentLogs.map(log => {
                const isNeg = log.emission < 0;
                const emLabel = isNeg
                    ? `<span class="emission-negative" title="Offset: saves CO₂">${log.emission.toFixed(1)} kg ♻️</span>`
                    : `<span class="emission-positive">${log.emission.toFixed(1)} kg</span>`;
                return `<tr>
                    <td><span class="badge badge-${log.category.toLowerCase()}">${log.category}</span></td>
                    <td>${log.subType} (${log.amount})</td>
                    <td>${emLabel}</td>
                    <td>${formatDate(log.logDate)}</td>
                </tr>`;
            }).join('');
        } else {
            recentBody.innerHTML = `<tr><td colspan="4" style="text-align: center; color: var(--text-secondary);">No activities logged this month.</td></tr>`;
        }

        // Render Tips
        const tipsContainer = document.getElementById('tipsContainer');
        if (data.recommendationTips && data.recommendationTips.length > 0) {
            tipsContainer.innerHTML = data.recommendationTips.map(tip => {
                const isWarning = tip.startsWith('Warning:');
                return `<div class="tip-item ${isWarning ? 'warning' : ''}">${tip}</div>`;
            }).join('');
        } else {
            tipsContainer.innerHTML = `<div class="tip-item">Log your daily activities to get tailored insights.</div>`;
        }

        // Render charts
        updateDashboardCharts(data.categoryBreakdown, current, prev, data.currentMonthGoal);
        
    } catch (err) {
        showToast(err.message, true);
    }
}

function toggleCalculatorDetails() {
    const panel = document.getElementById('calculatorDetailsPanel');
    const icon = document.getElementById('calcToggleIcon');
    const btn = document.getElementById('calcToggleBtn');
    if (panel.style.display === 'none') {
        panel.style.display = 'block';
        icon.textContent = '▲ Hide factors';
        if (btn) btn.setAttribute('aria-expanded', 'true');
    } else {
        panel.style.display = 'none';
        icon.textContent = '▼ Show factors';
        if (btn) btn.setAttribute('aria-expanded', 'false');
    }
}

// ----------------------------------------------------
// LOG HISTORY LOGIC
// ----------------------------------------------------
async function fetchLogsHistory() {
    try {
        const response = await authFetch(API_BASE_URL + '/logs');
        if (!response.ok) throw new Error('Could not fetch logs history');
        const logs = await safeJson(response);

        const body = document.getElementById('fullLogsTableBody');
        if (logs.length > 0) {
            body.innerHTML = logs.map(log => {
                const isNeg = log.emission < 0;
                const emCell = isNeg
                    ? `<span class="emission-negative" title="This is an offset — it reduces your net footprint \u2665">${log.emission.toFixed(2)} kg ♻️</span>`
                    : `${log.emission.toFixed(2)} kg`;
                return `<tr id="log-row-${log.id}">
                    <td><span class="badge badge-${log.category.toLowerCase()}">${log.category}</span></td>
                    <td>${log.subType}</td>
                    <td>${log.amount}</td>
                    <td>${emCell}</td>
                    <td>${formatDate(log.logDate)}</td>
                    <td>${log.notes || '-'}</td>
                    <td>
                        <button class="btn btn-danger" style="padding: 6px 12px; font-size: 0.8rem;" onclick="deleteLog(${log.id})">
                            Delete
                        </button>
                    </td>
                </tr>`;
            }).join('');
        } else {
            body.innerHTML = `<tr><td colspan="7" style="text-align: center; color: var(--text-secondary);">No activities logged yet.</td></tr>`;
        }
    } catch (err) {
        showToast(err.message, true);
    }
}

async function deleteLog(id) {
    if (!confirm('Are you sure you want to delete this log? This will recalculate your monthly emissions.')) return;

    try {
        const response = await authFetch(`${API_BASE_URL}/logs/${id}`, { method: 'DELETE' });
        if (!response.ok) throw new Error('Could not delete activity log');
        
        showToast('Activity log deleted successfully');
        
        // Remove row or reload
        const row = document.getElementById(`log-row-${id}`);
        if (row) row.remove();
        
        // Refresh values if we are on dashboard
        if (currentView === 'logs') {
            fetchLogsHistory();
        }
    } catch (err) {
        showToast(err.message, true);
    }
}

// Category Subtype Option Maps
const subTypesMap = {
    TRANSPORT: [
        { value: 'Petrol Car', label: 'Petrol Car (km)', unit: 'Distance (km)' },
        { value: 'Diesel Car', label: 'Diesel Car (km)', unit: 'Distance (km)' },
        { value: 'Electric Car', label: 'Electric Car (km)', unit: 'Distance (km)' },
        { value: 'Bus', label: 'Bus (km)', unit: 'Distance (km)' },
        { value: 'Train', label: 'Train (km)', unit: 'Distance (km)' },
        { value: 'Flight', label: 'Flight (km)', unit: 'Distance (km)' },
        { value: 'Bicycle/Walking', label: 'Bicycle/Walking (km)', unit: 'Distance (km)' }
    ],
    ENERGY: [
        { value: 'Electricity', label: 'Electricity (kWh)', unit: 'Electricity (kWh)' },
        { value: 'Natural Gas', label: 'Natural Gas (kWh)', unit: 'Natural Gas (kWh)' },
        { value: 'LPG / Propane', label: 'LPG / Propane (kg)', unit: 'Weight (kg)' },
        { value: 'Coal', label: 'Coal (kg)', unit: 'Weight (kg)' }
    ],
    FOOD: [
        { value: 'Meat-Heavy Meal', label: 'Meat-Heavy Meal (count)', unit: 'Number of Meals' },
        { value: 'Average Meal', label: 'Average Meal (count)', unit: 'Number of Meals' },
        { value: 'Vegetarian Meal', label: 'Vegetarian Meal (count)', unit: 'Number of Meals' },
        { value: 'Vegan Meal', label: 'Vegan Meal (count)', unit: 'Number of Meals' }
    ],
    WASTE: [
        { value: 'General Waste / Landfill', label: 'General Waste / Landfill (kg)', unit: 'Weight (kg)' },
        { value: 'Recyclable Waste', label: 'Recyclable Waste (kg)', unit: 'Weight (kg)' },
        { value: 'Organic Waste', label: 'Organic Waste (kg)', unit: 'Weight (kg)' }
    ]
};

function onCategoryChange() {
    const category = document.getElementById('logCategory').value;
    const subTypeSelect = document.getElementById('logSubType');
    const amountLabel = document.getElementById('amountLabel');

    const options = subTypesMap[category];
    subTypeSelect.innerHTML = options.map(opt => `<option value="${opt.value}">${opt.value}</option>`).join('');
    
    amountLabel.textContent = options[0].unit;
}

// ----------------------------------------------------
// ECO CHALLENGES / HABITS LOGIC
// ----------------------------------------------------
async function fetchChallenges() {
    try {
        // Fetch all challenges and active ones
        const [resAll, resUser] = await Promise.all([
            authFetch(API_BASE_URL + '/actions'),
            authFetch(API_BASE_URL + '/actions/user')
        ]);

        if (!resAll.ok || !resUser.ok) throw new Error('Could not load challenges');

        const allActions = await safeJson(resAll);
        const userActions = await safeJson(resUser);

        // Render Active Challenges
        const activeContainer = document.getElementById('activeChallengesContainer');
        if (userActions.length > 0) {
            activeContainer.innerHTML = userActions.map(ua => {
                const completedToday = isSameAsToday(ua.completionDate);
                const buttonHtml = completedToday
                    ? `<button class="btn btn-success-disabled" style="padding: 8px 14px; font-size: 0.85rem;" disabled>✓ Completed Today</button>`
                    : `<button class="btn btn-primary" style="padding: 8px 14px; font-size: 0.85rem;" onclick="logChallengeCompletion(${ua.id})">✓ Done</button>`;
                
                const challengerHtml = ua.challengedBy 
                    ? `<div style="font-size: 0.82rem; color: var(--color-eco); margin-top: 6px; font-weight: 500;">🤝 Challenged by: <strong>${ua.challengedBy}</strong></div>`
                    : '';

                return `
                <div class="challenge-card">
                    <div>
                        <div class="challenge-header">
                            <span class="badge badge-${ua.action.category.toLowerCase()}">${ua.action.category}</span>
                            <span class="badge badge-${ua.action.difficulty.toLowerCase()}">${ua.action.difficulty}</span>
                        </div>
                        <div class="challenge-title">${ua.action.title}</div>
                        <div class="challenge-desc">${ua.action.description}</div>
                        <div class="challenge-impact">Offset: -${ua.action.co2Savings.toFixed(1)} kg CO2/day</div>
                        ${challengerHtml}
                    </div>
                    
                    <div style="display:flex; justify-content:space-between; align-items:center; margin-top:20px;">
                        <div class="user-challenge-streak">
                            <span>🔥 Streak:</span>
                            <span class="streak-count" id="streak-${ua.id}">${ua.streakDays} days</span>
                        </div>
                        <div style="display:flex; gap:8px;">
                            ${buttonHtml}
                            <button class="btn btn-secondary" style="padding: 8px 10px; font-size: 0.85rem;" onclick="abandonChallenge(${ua.id})">
                                Abandon
                            </button>
                        </div>
                    </div>
                </div>
                `;
            }).join('');
        } else {
            activeContainer.innerHTML = `<div style="grid-column: 1/-1; text-align: center; color: var(--text-secondary); padding: 20px;">You do not have any active habits. Select an eco challenge below to commit!</div>`;
        }

        // Render Available Challenges
        // Filter out catalog items that are already active
        const activeActionIds = userActions.map(ua => ua.action.id);
        const availableActions = allActions.filter(action => !activeActionIds.includes(action.id));

        const availableContainer = document.getElementById('availableChallengesContainer');
        if (availableActions.length > 0) {
            availableContainer.innerHTML = availableActions.map(action => `
                <div class="challenge-card">
                    <div>
                        <div class="challenge-header">
                            <span class="badge badge-${action.category.toLowerCase()}">${action.category}</span>
                            <span class="badge badge-${action.difficulty.toLowerCase()}">${action.difficulty}</span>
                        </div>
                        <div class="challenge-title">${action.title}</div>
                        <div class="challenge-desc">${action.description}</div>
                        <div class="challenge-impact">Est. Offset: -${action.co2Savings.toFixed(1)} kg CO2/day</div>
                    </div>
                    
                    <button class="btn btn-secondary" style="width:100%; margin-top:20px;" onclick="optInChallenge(${action.id})">
                        Commit to Challenge
                    </button>
                </div>
            `).join('');
        } else {
            availableContainer.innerHTML = `<div style="grid-column: 1/-1; text-align: center; color: var(--text-secondary); padding: 20px;">Wow! You are committed to all available challenges!</div>`;
        }

    } catch (err) {
        showToast(err.message, true);
    }
}

async function optInChallenge(actionId) {
    try {
        const response = await authFetch(`${API_BASE_URL}/actions/opt-in/${actionId}`, { method: 'POST' });
        if (!response.ok) {
            const data = await safeJson(response);
            throw new Error(data.error || 'Failed to commit to challenge');
        }
        
        showToast('Successfully committed to habit! Try checking it off daily.');
        fetchChallenges();
    } catch (err) {
        showToast(err.message, true);
    }
}

async function logChallengeCompletion(userActionId) {
    try {
        const response = await authFetch(`${API_BASE_URL}/actions/complete/${userActionId}`, { method: 'POST' });
        if (!response.ok) {
            const data = await safeJson(response);
            throw new Error(data.error || 'Failed to record completion');
        }
        const data = await safeJson(response);
        
        showToast(`Challenge logged! Streak increased to ${data.streakDays} days!`);
        fetchChallenges();
    } catch (err) {
        showToast(err.message, true);
    }
}

async function abandonChallenge(userActionId) {
    if (!confirm('Are you sure you want to abandon this habit? Your active streak will be lost.')) return;

    try {
        const response = await authFetch(`${API_BASE_URL}/actions/abandon/${userActionId}`, { method: 'DELETE' });
        if (!response.ok) throw new Error('Failed to abandon challenge');
        
        showToast('Challenge abandoned');
        fetchChallenges();
    } catch (err) {
        showToast(err.message, true);
    }
}

// ----------------------------------------------------
// CARBON BUDGETS / GOALS LOGIC
// ----------------------------------------------------
async function fetchGoalHistory() {
    try {
        const response = await authFetch(API_BASE_URL + '/goals');
        if (!response.ok) throw new Error('Failed to fetch goals');
        const goals = await safeJson(response);

        const body = document.getElementById('goalsTableBody');
        if (goals.length > 0) {
            body.innerHTML = goals.map(goal => {
                let badgeClass = 'easy';
                if (goal.status === 'EXCEEDED') badgeClass = 'hard';
                else if (goal.status === 'IN_PROGRESS') badgeClass = 'medium';

                return `
                    <tr>
                        <td><strong>${getMonthName(goal.month)} ${goal.year}</strong></td>
                        <td>${goal.targetEmission.toFixed(0)} kg CO2</td>
                        <td><span class="badge badge-${badgeClass}">${goal.status}</span></td>
                    </tr>
                `;
            }).join('');
        } else {
            body.innerHTML = `<tr><td colspan="3" style="text-align: center; color: var(--text-secondary);">No goals set yet.</td></tr>`;
        }
    } catch (err) {
        showToast(err.message, true);
    }
}

async function handleSetGoal(e) {
    e.preventDefault();
    const month = parseInt(document.getElementById('goalMonth').value);
    const year = parseInt(document.getElementById('goalYear').value);
    const targetEmission = parseFloat(document.getElementById('goalEmission').value);

    try {
        const response = await authFetch(API_BASE_URL + '/goals', {
            method: 'POST',
            body: JSON.stringify({ month, year, targetEmission })
        });

        if (!response.ok) {
            const data = await safeJson(response);
            throw new Error(data.error || 'Failed to save monthly budget');
        }
        
        showToast('Monthly carbon budget saved successfully!');
        
        // Reset input field only
        document.getElementById('goalEmission').value = '';
        
        // Refresh goal listing
        fetchGoalHistory();
    } catch (err) {
        showToast(err.message, true);
    }
}

// ----------------------------------------------------
// COMMUNITY / SOCIAL LOGIC
// ----------------------------------------------------
async function fetchLeaderboard() {
    try {
        const response = await authFetch(API_BASE_URL + '/social/leaderboard');
        if (!response.ok) throw new Error('Could not fetch community standings');
        const data = await safeJson(response);
        
        const body = document.getElementById('leaderboardTableBody');
        if (data.length > 0) {
            body.innerHTML = data.map(entry => {
                // Determine badge based on total saved co2
                let badge = '<span class="badge badge-easy">Eco Rookie</span>';
                if (entry.totalSavedCo2 >= 150) {
                    badge = '<span class="badge badge-hard">Carbon Free</span>';
                } else if (entry.totalSavedCo2 >= 50) {
                    badge = '<span class="badge badge-transport">Eco Champion</span>';
                } else if (entry.totalSavedCo2 >= 10) {
                    badge = '<span class="badge badge-medium">Green Ranger</span>';
                }
                
                // Highlight current user
                const currentUsername = localStorage.getItem('biometrck_username');
                const isSelf = entry.username === currentUsername;
                
                return `
                    <tr style="${isSelf ? 'background: rgba(16, 185, 129, 0.08); font-weight: 500;' : ''}">
                        <td><strong>#${entry.rank}</strong></td>
                        <td>${entry.username} ${isSelf ? ' (You)' : ''}</td>
                        <td>${entry.totalSavedCo2.toFixed(1)} kg</td>
                        <td>${badge}</td>
                    </tr>
                `;
            }).join('');
        } else {
            body.innerHTML = `<tr><td colspan="4" style="text-align: center; color: var(--text-secondary);">No community standings available.</td></tr>`;
        }
    } catch (err) {
        showToast(err.message, true);
    }
}

function copyInviteLink() {
    const copyText = document.getElementById("inviteLinkInput");
    copyText.select();
    copyText.setSelectionRange(0, 99999); // For mobile devices
    navigator.clipboard.writeText(copyText.value);
    showToast("Invite link copied to clipboard!");
}

function updateInviteLink() {
    const type = document.getElementById('challengeTypeSelect').value;
    const username = localStorage.getItem('biometrck_username') || 'user';
    const input = document.getElementById('inviteLinkInput');
    if (input) {
        input.value = `${window.location.origin}${window.location.pathname}?ref=challenge&type=${type}&from=${encodeURIComponent(username)}`;
    }
}

function checkInviteParams() {
    const urlParams = new URLSearchParams(window.location.search);
    const ref = urlParams.get('ref');
    const fromUser = urlParams.get('from');
    const type = urlParams.get('type');
    if (ref === 'challenge' && fromUser && type) {
        window.history.replaceState({}, document.title, window.location.pathname);
        sessionStorage.setItem('pendingInvite', JSON.stringify({ fromUser, type }));
        // Show invite banner on auth screen
        const banner = document.getElementById('inviteBanner');
        const bannerText = document.getElementById('inviteBannerText');
        if (banner && bannerText) {
            bannerText.textContent = `🌿 @${fromUser} has invited you to a challenge! Create an account or log in to accept.`;
            banner.style.display = 'block';
        }
        // Pre-switch to register mode
        if (currentAuthMode === 'LOGIN') toggleAuthMode();
    }
}

async function processPendingInvite(invite) {
    sessionStorage.removeItem('pendingInvite');
    const { fromUser, type } = invite;
    let actionId = 2, challengeName = 'Meatless Monday';
    if (type === 'commute') { actionId = 1; challengeName = 'Use Public Transit'; }
    else if (type === 'energy') { actionId = 3; challengeName = 'Switch to LED Bulbs'; }
    else if (type === 'general') { actionId = 5; challengeName = 'Compost Organic Waste'; }
    
    const accepted = confirm(`🌿 @${fromUser} challenged you to: "${challengeName}"\n\nAccept this challenge?`);
    if (accepted) {
        await optInChallengeFromFriend(actionId, fromUser, challengeName);
    }
}

function handleIncomingChallenge() {
    const urlParams = new URLSearchParams(window.location.search);
    const ref = urlParams.get('ref');
    const fromUser = urlParams.get('from');
    const type = urlParams.get('type');
    
    if (ref === 'challenge' && fromUser && type) {
        // Clear query parameters to avoid accepting repeatedly on refresh
        window.history.replaceState({}, document.title, window.location.pathname);
        
        let actionId = 2; // default Meatless Monday
        let challengeName = 'Meatless Monday';
        if (type === 'commute') {
            actionId = 1;
            challengeName = 'Use Public Transit';
        } else if (type === 'energy') {
            actionId = 3;
            challengeName = 'Switch to LED Bulbs';
        } else if (type === 'general') {
            actionId = 5;
            challengeName = 'Compost Organic Waste';
        }
        
        const accepted = confirm(`🌿 @${fromUser} challenged you to: "${challengeName}"\n\nAccept this challenge?`);
        if (accepted) {
            optInChallengeFromFriend(actionId, fromUser, challengeName);
        }
    }
}

async function optInChallengeFromFriend(actionId, fromUser, challengeName) {
    try {
        const response = await authFetch(`${API_BASE_URL}/actions/opt-in/${actionId}?challengedBy=${encodeURIComponent(fromUser)}`, {
            method: 'POST'
        });
        
        if (response.ok) {
            showToast(`Accepted "${challengeName}" challenge from ${fromUser}!`);
        } else {
            const data = await safeJson(response);
            showToast(data.error || `You are already tracking "${challengeName}"!`);
        }
        switchView('challenges');
    } catch (err) {
        console.error("Failed to accept challenge", err);
    }
}

// ----------------------------------------------------
// INTERACTIVE TOUR LOGIC
// ----------------------------------------------------
function startTour() {
    const modal = document.getElementById('tourModal');
    if (modal) modal.style.display = 'none';

    currentTourStep = 0;
    renderTourStep();
}

function renderTourStep() {
    const step = tourSteps[currentTourStep];
    
    // Automatically navigate to correct tab
    switchView(step.view);
    
    const activeView = document.getElementById(`view-${step.view}`);
    const banner = document.getElementById('tourBanner');
    
    if (activeView && banner) {
        // Move the banner to the top of active view
        activeView.prepend(banner);
        banner.style.display = 'flex';
        
        const isLast = currentTourStep === tourSteps.length - 1;
        const backBtnHtml = currentTourStep === 0 
            ? '' 
            : `<button class="btn btn-secondary" style="padding: 6px 12px; font-size: 0.8rem;" onclick="prevTourStep()">Back</button>`;
        
        banner.innerHTML = `
            <div class="tour-banner-header">
                <span class="tour-banner-title">${step.title}</span>
                <span style="font-size: 0.8rem; color: var(--text-secondary); font-weight: 500;">Step ${currentTourStep + 1} of ${tourSteps.length}</span>
            </div>
            <div class="tour-banner-text">${step.text}</div>
            <div class="tour-banner-footer">
                ${backBtnHtml}
                <div style="display: flex; gap: 8px;">
                    <button class="btn btn-secondary" style="padding: 6px 12px; font-size: 0.8rem;" onclick="endTour()">End Tour</button>
                    <button class="btn btn-primary" style="padding: 6px 14px; font-size: 0.8rem;" onclick="nextTourStep()">
                        ${isLast ? 'Finish' : 'Next'}
                    </button>
                </div>
            </div>
        `;
    }
}

function nextTourStep() {
    if (currentTourStep < tourSteps.length - 1) {
        currentTourStep++;
        renderTourStep();
    } else {
        endTour();
    }
}

function prevTourStep() {
    if (currentTourStep > 0) {
        currentTourStep--;
        renderTourStep();
    }
}

function endTour() {
    const banner = document.getElementById('tourBanner');
    if (banner) banner.style.display = 'none';
    
    const username = localStorage.getItem('biometrck_username');
    if (username) {
        localStorage.setItem('tour_completed_' + username, 'true');
    }
    showToast("Tour finished! You're ready to trace your footprint!");
}

// ----------------------------------------------------
// WALKING & EMISSIONS INDICATORS
// ----------------------------------------------------
let steps = 0;
let isTrackingSteps = false;
let stepThreshold = 11.5;
let lastStepTime = 0;
const WALK_TARGET_KM = 5;

function updateEmissionHealthBar(kg) {
    const maxKg = 500;
    const pct = Math.min(Math.max(kg, 0) / maxKg * 100, 98);
    const marker = document.getElementById('emissionHealthMarker');
    if (marker) marker.style.left = pct + '%';
    
    const valEl = document.getElementById('metric-current-val');
    const labelEl = document.getElementById('emissionHealthLabel');
    let color, label;
    if (kg <= 0) { color = 'var(--color-eco)'; label = '🌟 Carbon neutral or negative!'; }
    else if (kg <= 50) { color = 'var(--color-eco)'; label = '🌿 Excellent — well below sustainable target'; }
    else if (kg <= 100) { color = '#34d399'; label = '✅ Good — within sustainable range (<100 kg/mo)'; }
    else if (kg <= 200) { color = '#fbbf24'; label = '⚡ Average — room to improve'; }
    else if (kg <= 400) { color = '#f97316'; label = '⚠️ Above average — take action'; }
    else { color = '#ef4444'; label = '🚨 High — significantly above global average'; }
    if (valEl) valEl.style.color = color;
    if (labelEl) { labelEl.textContent = label; labelEl.style.color = color; }
}

function loadWalkState() {
    const username = localStorage.getItem('biometrck_username');
    if (!username) return;
    const today = new Date().toISOString().split('T')[0];
    const savedDate = localStorage.getItem('walk_date_' + username);
    const savedKm = parseFloat(localStorage.getItem('walk_km_' + username) || '0');
    
    if (savedDate === today) {
        const isLogged = localStorage.getItem('walk_logged_' + username) === today;
        if (isLogged) {
            document.getElementById('logWalkBtn').disabled = true;
            document.getElementById('walkKmInput').value = savedKm.toFixed(1);
            document.getElementById('walkKmInput').disabled = true;
            document.getElementById('startStepBtn').disabled = true;
            document.getElementById('sensorStatusText').textContent = '🚶 You have already logged your walk for today. Come back tomorrow!';
        } else {
            document.getElementById('logWalkBtn').disabled = savedKm <= 0;
            document.getElementById('walkKmInput').value = savedKm > 0 ? savedKm.toFixed(1) : '';
            document.getElementById('walkKmInput').disabled = false;
            document.getElementById('startStepBtn').disabled = false;
        }
        updateWalkRing(savedKm);
    } else {
        steps = 0;
        localStorage.setItem('walk_date_' + username, today);
        localStorage.setItem('walk_km_' + username, '0');
        localStorage.removeItem('walk_logged_' + username);
        
        document.getElementById('walkKmInput').value = '';
        document.getElementById('walkKmInput').disabled = false;
        document.getElementById('startStepBtn').disabled = false;
        document.getElementById('logWalkBtn').disabled = true;
        updateWalkRing(0);
    }
}

function onWalkInputChange() {
    const km = parseFloat(document.getElementById('walkKmInput').value) || 0;
    const savings = km > 0 ? (km * 0.18).toFixed(3) : '0.000';
    document.getElementById('walkSavingsDisplay').textContent = savings + ' kg';
    
    const username = localStorage.getItem('biometrck_username');
    const today = new Date().toISOString().split('T')[0];
    const isLogged = localStorage.getItem('walk_logged_' + username) === today;
    
    document.getElementById('logWalkBtn').disabled = isLogged || km <= 0;
    updateWalkRing(km);
    
    steps = Math.round((km * 1000.0) / 0.75);
}

function updateWalkRing(km) {
    const pct = Math.min(km / WALK_TARGET_KM, 1);
    const circumference = 239;
    const offset = circumference * (1 - pct);
    const arc = document.getElementById('walkProgressArc');
    if (arc) arc.style.strokeDashoffset = offset;
    const disp = document.getElementById('walkKmDisplay');
    if (disp) disp.textContent = km.toFixed(1);
}

async function logWalkingOffset() {
    const km = parseFloat(document.getElementById('walkKmInput').value);
    if (!km || km <= 0) { showToast('Please enter a valid distance.', true); return; }
    
    const username = localStorage.getItem('biometrck_username');
    const today = new Date().toISOString().split('T')[0];
    const isLogged = localStorage.getItem('walk_logged_' + username) === today;
    if (isLogged) {
        showToast('You have already logged a walking offset today. Come back tomorrow!', true);
        return;
    }
    
    const savings = (km * 0.18).toFixed(3);
    
    try {
        const response = await authFetch(API_BASE_URL + '/logs', {
            method: 'POST',
            body: JSON.stringify({
                category: 'TRANSPORT',
                subType: 'Walk Distance Offset',
                amount: km,
                logDate: today,
                notes: `Walked ${km} km today. Saved ${savings} kg CO₂.`
            })
        });
        
        if (!response.ok) {
            const data = await safeJson(response);
            throw new Error(data.error || 'Failed to save walk offset');
        }
        
        localStorage.setItem('walk_logged_' + username, today);
        localStorage.setItem('walk_km_' + username, km.toString());
        
        showToast(`🚶 ${km} km walk logged! Saved ${savings} kg CO₂.`);
        
        document.getElementById('walkKmInput').disabled = true;
        document.getElementById('startStepBtn').disabled = true;
        document.getElementById('logWalkBtn').disabled = true;
        document.getElementById('sensorStatusText').textContent = '🚶 You have already logged your walk for today. Come back tomorrow!';
        
        if (isTrackingSteps) {
            stopStepCounter();
        }
        
        // Refresh dashboard numbers
        if (currentView === 'dashboard') {
            fetchDashboardData();
        }
    } catch (err) {
        showToast(err.message, true);
    }
}

function startStepCounter() {
    const startBtn = document.getElementById('startStepBtn');
    const statusText = document.getElementById('sensorStatusText');
    if (!startBtn || !statusText) return;

    if (isTrackingSteps) {
        stopStepCounter();
        return;
    }

    if (typeof DeviceMotionEvent !== 'undefined' && typeof DeviceMotionEvent.requestPermission === 'function') {
        DeviceMotionEvent.requestPermission()
            .then(permissionState => {
                if (permissionState === 'granted') {
                    activateStepSensor();
                } else {
                    showToast("Sensor access denied. You can still enter distance manually.", true);
                }
            })
            .catch(err => {
                console.error(err);
                showToast("Sensor permission request failed.", true);
            });
    } else {
        activateStepSensor();
    }
}

function activateStepSensor() {
    isTrackingSteps = true;
    const startBtn = document.getElementById('startStepBtn');
    const statusText = document.getElementById('sensorStatusText');
    if (startBtn) {
        startBtn.textContent = 'Stop Sensor';
        startBtn.style.background = 'var(--color-danger)';
    }
    if (statusText) {
        statusText.textContent = 'Sensor active! Walk with your device or shake it to count steps.';
    }
    window.addEventListener('devicemotion', handleDeviceMotion);
}

function stopStepCounter() {
    isTrackingSteps = false;
    const startBtn = document.getElementById('startStepBtn');
    const statusText = document.getElementById('sensorStatusText');
    if (startBtn) {
        startBtn.textContent = '📱 Sensor';
        startBtn.style.background = '';
    }
    if (statusText) {
        statusText.textContent = 'Sensor stopped.';
    }
    window.removeEventListener('devicemotion', handleDeviceMotion);
}

function handleDeviceMotion(event) {
    const acc = event.accelerationIncludingGravity;
    if (!acc) return;
    const magnitude = Math.sqrt(acc.x * acc.x + acc.y * acc.y + acc.z * acc.z);
    const now = Date.now();
    if (magnitude > stepThreshold && (now - lastStepTime) > 350) {
        steps++;
        lastStepTime = now;
        updateStepDisplay();
    }
}

function simulateStep() {
    const username = localStorage.getItem('biometrck_username');
    const today = new Date().toISOString().split('T')[0];
    const isLogged = localStorage.getItem('walk_logged_' + username) === today;
    if (isLogged) return;

    steps += 150; // increment steps by 150 for demonstration
    updateStepDisplay();
}

function updateStepDisplay() {
    const km = steps * 0.75 / 1000.0;
    
    // Save to localStorage
    const username = localStorage.getItem('biometrck_username');
    if (username) {
        localStorage.setItem('walk_km_' + username, km.toString());
    }
    
    const input = document.getElementById('walkKmInput');
    if (input) {
        input.value = km.toFixed(2);
    }
    onWalkInputChange();
}

// ----------------------------------------------------
// LOG DIALOG MODALS
// ----------------------------------------------------
function openLogModal() {
    document.getElementById('logModal').style.display = 'flex';
}

function closeLogModal() {
    document.getElementById('logModal').style.display = 'none';
}

async function handleLogActivitySubmit(e) {
    e.preventDefault();
    
    const category = document.getElementById('logCategory').value;
    const subType = document.getElementById('logSubType').value;
    const amount = parseFloat(document.getElementById('logAmount').value);
    const logDate = document.getElementById('logDate').value;
    const notes = document.getElementById('logNotes').value;

    try {
        const response = await authFetch(API_BASE_URL + '/logs', {
            method: 'POST',
            body: JSON.stringify({ category, subType, amount, logDate, notes })
        });

        if (!response.ok) {
            const data = await safeJson(response);
            throw new Error(data.error || 'Failed to save log entry');
        }

        showToast('Activity logged successfully!');
        closeLogModal();
        
        // Reset inputs
        document.getElementById('logAmount').value = '';
        document.getElementById('logNotes').value = '';
        
        // Refresh dashboard or history
        if (currentView === 'dashboard') {
            fetchDashboardData();
        } else if (currentView === 'logs') {
            fetchLogsHistory();
        }
    } catch (err) {
        showToast(err.message, true);
    }
}

// ----------------------------------------------------
// UI UTILS
// ----------------------------------------------------
function showToast(message, isError = false) {
    const toast = document.getElementById('toast');
    const toastMessage = document.getElementById('toastMessage');
    
    toastMessage.textContent = message;
    if (isError) {
        toast.classList.add('error');
    } else {
        toast.classList.remove('error');
    }
    
    toast.style.display = 'block';
    
    setTimeout(() => {
        toast.style.display = 'none';
    }, 4000);
}

function formatDate(dateStr) {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
}

function getMonthName(monthNumber) {
    const months = [
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    ];
    return months[monthNumber - 1] || '';
}

function isSameAsToday(dateVal) {
    if (!dateVal) return false;
    let year, month, day;
    if (Array.isArray(dateVal)) {
        [year, month, day] = dateVal;
    } else if (typeof dateVal === 'string') {
        const parts = dateVal.split('-');
        if (parts.length === 3) {
            year = parseInt(parts[0], 10);
            month = parseInt(parts[1], 10);
            day = parseInt(parts[2], 10);
        } else {
            const date = new Date(dateVal);
            year = date.getFullYear();
            month = date.getMonth() + 1;
            day = date.getDate();
        }
    } else {
        return false;
    }
    const today = new Date();
    return today.getFullYear() === year && 
           (today.getMonth() + 1) === month && 
           today.getDate() === day;
}

// ----------------------------------------------------
// THEME SWITCHER LOGIC
// ----------------------------------------------------
function checkThemeState() {
    const savedTheme = localStorage.getItem('theme');
    const body = document.body;
    const sidebarToggleLabel = document.getElementById('themeToggleLabel');
    
    if (savedTheme === 'light') {
        body.classList.add('light-theme');
        if (sidebarToggleLabel) sidebarToggleLabel.textContent = 'Light';
    } else {
        body.classList.remove('light-theme');
        if (sidebarToggleLabel) sidebarToggleLabel.textContent = 'Dark';
    }
}

function toggleTheme() {
    const body = document.body;
    const isLight = body.classList.toggle('light-theme');
    localStorage.setItem('theme', isLight ? 'light' : 'dark');
    
    const sidebarToggleLabel = document.getElementById('themeToggleLabel');
    if (sidebarToggleLabel) sidebarToggleLabel.textContent = isLight ? 'Light' : 'Dark';
    
    showToast(`Switched to ${isLight ? 'Light' : 'Dark'} Mode`);
}

// ----------------------------------------------------
// ACCOUNT SETTINGS LOGIC
// ----------------------------------------------------
function openAccountModal() {
    const username = localStorage.getItem('biometrck_username') || '';
    const email = localStorage.getItem('biometrck_email') || '';
    
    document.getElementById('accountUsername').value = username;
    document.getElementById('accountEmail').value = email;
    document.getElementById('accountPassword').value = '';
    
    document.getElementById('accountModal').style.display = 'flex';
}

function closeAccountModal() {
    document.getElementById('accountModal').style.display = 'none';
}

async function handleUpdateAccount(e) {
    e.preventDefault();
    const username = document.getElementById('accountUsername').value;
    const email = document.getElementById('accountEmail').value;
    const password = document.getElementById('accountPassword').value;
    
    const payload = { username, email };
    if (password && password.trim() !== '') {
        payload.password = password;
    }
    
    try {
        const response = await authFetch(API_BASE_URL + '/users/profile', {
            method: 'PUT',
            body: JSON.stringify(payload)
        });
        
        if (!response.ok) {
            const data = await safeJson(response);
            throw new Error(data.error || 'Failed to update account details');
        }
        
        const data = await safeJson(response);
        
        // Save new token and details in localStorage
        localStorage.setItem('biometrck_token', data.token);
        localStorage.setItem('biometrck_username', data.username);
        localStorage.setItem('biometrck_email', data.email);
        
        // Update values in sidebar
        document.getElementById('profileUsername').textContent = data.username;
        document.getElementById('profileEmail').textContent = data.email;
        
        showToast('Account details updated successfully!');
        closeAccountModal();
        
        // Refresh active views
        if (currentView === 'dashboard') {
            fetchDashboardData();
        } else if (currentView === 'community') {
            fetchLeaderboard();
        }
    } catch (err) {
        showToast(err.message, true);
    }
}

async function handleDeleteAccount() {
    const confirmDelete = confirm('⚠️ WARNING: Are you sure you want to delete your account? This will permanently wipe your profile, carbon logs, budgets, and habit streaks. This action CANNOT be undone.');
    if (!confirmDelete) return;
    
    try {
        const response = await authFetch(API_BASE_URL + '/users/profile', {
            method: 'DELETE'
        });
        
        if (!response.ok) {
            const data = await safeJson(response);
            throw new Error(data.error || 'Failed to delete account');
        }
        
        // Clear auth details and sign out
        localStorage.removeItem('biometrck_token');
        localStorage.removeItem('biometrck_username');
        localStorage.removeItem('biometrck_email');
        
        closeAccountModal();
        showToast('Your account was deleted successfully.', false);
        
        // Reset state
        checkAuthState();
    } catch (err) {
        showToast(err.message, true);
    }
}

// ----------------------------------------------------
// MOBILE NAVIGATION LOGIC
// ----------------------------------------------------
function toggleMobileSidebar() {
    const appContainer = document.getElementById('appContainer');
    const overlay = document.getElementById('sidebarOverlay');
    
    const isOpen = appContainer.classList.toggle('sidebar-open');
    if (overlay) {
        overlay.style.display = isOpen ? 'block' : 'none';
    }
}

// Intercept clicks on nav-item inside collapsible menu to auto-close it on mobile
document.querySelectorAll('.nav-links a').forEach(link => {
    link.addEventListener('click', () => {
        const appContainer = document.getElementById('appContainer');
        if (appContainer.classList.contains('sidebar-open')) {
            toggleMobileSidebar();
        }
    });
});

// ----------------------------------------------------
// DESKTOP SIDEBAR COLLAPSIBLE LOGIC
// ----------------------------------------------------
function checkSidebarState() {
    const collapsed = localStorage.getItem('sidebar_collapsed') === 'true';
    const appContainer = document.getElementById('appContainer');
    if (appContainer) {
        if (collapsed) {
            appContainer.classList.add('sidebar-collapsed');
        } else {
            appContainer.classList.remove('sidebar-collapsed');
        }
    }
}

function toggleSidebarDesktop() {
    const appContainer = document.getElementById('appContainer');
    if (appContainer) {
        const isCollapsed = appContainer.classList.toggle('sidebar-collapsed');
        localStorage.setItem('sidebar_collapsed', isCollapsed ? 'true' : 'false');
    }
}

function handleSidebarCollapseBtn() {
    if (window.innerWidth <= 900) {
        toggleMobileSidebar();
    } else {
        toggleSidebarDesktop();
    }
}
