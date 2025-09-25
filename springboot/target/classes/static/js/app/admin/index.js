import AdminDashboard from './AdminDashboard.js';

// Initialize dashboard when page loads
let dashboard;
document.addEventListener('DOMContentLoaded', function() {
    dashboard = new AdminDashboard();
    // Expose minimal globals used by inline HTML onclick attributes
    window.dashboard = dashboard;
    window.showSection = function(section) {
        document.querySelectorAll('.content-section').forEach(el => el.classList.remove('active'));
        document.querySelectorAll('.sidebar .nav-link').forEach(el => el.classList.remove('active'));
        const sectionEl = document.getElementById(section + 'Section');
        if (sectionEl) sectionEl.classList.add('active');
        if (window.event && window.event.target) {
            window.event.target.classList.add('active');
        }
        switch(section) {
            case 'overview':
                dashboard.loadOverview();
                break;
            case 'users':
                dashboard.loadUsers();
                break;
            case 'sessions':
                break;
            case 'activity':
                window.loadActivity();
                break;
            case 'statistics':
                window.loadStatistics();
                break;
            case 'settings':
                break;
        }
    };
    window.refreshOverview = function() { dashboard.refreshOverview(); };
    window.logout = function() {
        if (dashboard && typeof dashboard.logout === 'function') {
            dashboard.logout();
        } else {
            localStorage.removeItem('admin_token');
            window.location.reload();
        }
    };
    window.createUser = function() {
        const form = document.getElementById('createUserForm');
        if (form) form.reset();
        new bootstrap.Modal(document.getElementById('createUserModal')).show();
    };
    // Apply user filters
    const applyBtn = document.getElementById('applyUserFilters');
    if (applyBtn) {
        applyBtn.addEventListener('click', function() {
            const search = (document.getElementById('userSearch')?.value || '').trim();
            const role = document.getElementById('userRole')?.value || '';
            const statusVal = document.getElementById('userStatus')?.value || '';
            const isActive = statusVal === '' ? undefined : (statusVal === 'true');
            dashboard.loadUsers({ search, role, isActive, page: 0, size: 50 });
        });
    }
    // Enter key triggers filter
    const searchInput = document.getElementById('userSearch');
    if (searchInput) {
        searchInput.addEventListener('keydown', function(e) {
            if (e.key === 'Enter') {
                e.preventDefault();
                applyBtn?.click();
            }
        });
    }
    window.submitCreateUser = function() {
        const form = document.getElementById('createUserForm');
        if (!form.checkValidity()) { form.reportValidity(); return; }
        const userData = {
            username: document.getElementById('createUsername').value,
            email: document.getElementById('createEmail').value,
            firstName: document.getElementById('createFirstName').value,
            lastName: document.getElementById('createLastName').value,
            companyName: document.getElementById('createCompany').value,
            role: document.getElementById('createRole').value,
            password: document.getElementById('createPassword').value,
            isActive: document.getElementById('createIsActive').checked,
            isVerified: document.getElementById('createIsVerified').checked,
            ttnUsername: document.getElementById('createTtnUsername').value,
            ttnPassword: document.getElementById('createTtnPassword').value,
            ttnMatriculeFiscal: document.getElementById('createTtnMatricule').value,
            anceSealPin: document.getElementById('createAncePin').value,
            anceSealAlias: document.getElementById('createAnceAlias').value,
            certificatePath: document.getElementById('createCertPath').value
        };
        dashboard.createUser(userData);
    };
    window.submitEditUser = function() {
        const form = document.getElementById('editUserForm');
        if (!form.checkValidity()) { form.reportValidity(); return; }
        const userId = document.getElementById('editUserId').value;
        const userData = {
            email: document.getElementById('editEmail').value,
            firstName: document.getElementById('editFirstName').value,
            lastName: document.getElementById('editLastName').value,
            companyName: document.getElementById('editCompany').value,
            role: document.getElementById('editRole').value,
            isActive: document.getElementById('editIsActive').checked,
            isVerified: document.getElementById('editIsVerified').checked
        };
        dashboard.updateUser(userId, userData);
    };
    window.submitCredentials = function() {
        const userId = document.getElementById('credentialsUserId').value;
        const credentialsData = {
            ttnUsername: document.getElementById('credTtnUsername').value,
            ttnPassword: document.getElementById('credTtnPassword').value,
            ttnMatriculeFiscal: document.getElementById('credTtnMatricule').value,
            anceSealPin: document.getElementById('credAncePin').value,
            anceSealAlias: document.getElementById('credAnceAlias').value,
            certificatePath: document.getElementById('credCertPath').value
        };
        dashboard.updateCredentials(userId, credentialsData);
    };
    window.editUserFromDetails = function() {
        const modal = bootstrap.Modal.getInstance(document.getElementById('userDetailsModal'));
        if (modal) modal.hide();
        setTimeout(() => {
            dashboard.editUser(dashboard.currentViewingUserId);
        }, 300);
    };
    window.loadSessions = function() {
        const el = document.getElementById('sessionsTableContainer');
        if (el) el.innerHTML = '<p class="text-center text-muted">Fonctionnalité sessions à implémenter</p>';
    };
    window.loadActivity = function() {
        const el = document.getElementById('activityTableContainer');
        if (el) {
            el.innerHTML = '<div class="spinner-border spinner-border-custom text-primary" role="status"></div><p class="mt-2">Chargement...</p>';
        }
        dashboard.loadActivity(50);
    };
    window.loadStatistics = function() {
        const u = document.getElementById('userStatistics');
        const o = document.getElementById('operationStatistics');
        if (u) u.innerHTML = '<div class="spinner-border spinner-border-custom text-primary" role="status"></div><p class="mt-2">Chargement...</p>';
        if (o) o.innerHTML = '<div class="spinner-border spinner-border-custom text-primary" role="status"></div><p class="mt-2">Chargement...</p>';
        dashboard.loadStatistics();
    };
    window.clearCache = function() { alert('Fonctionnalité de vidage du cache à implémenter'); };
    window.exportData = function() { alert('Fonctionnalité d\'export à implémenter'); };
    window.viewLogs = function() { alert('Fonctionnalité de visualisation des logs à implémenter'); };
});


