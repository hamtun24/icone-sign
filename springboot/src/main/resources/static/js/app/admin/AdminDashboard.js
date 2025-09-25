import ApiClient from '../../core/ApiClient.js';
import AuthManager from '../../core/AuthManager.js';
import UserTable from '../../components/UserTable.js';
import OverviewModule from './modules/Overview.js';
import ActivityModule from './modules/Activity.js';
import StatisticsModule from './modules/Statistics.js';
import UsersModule from './modules/Users.js';

class AdminDashboard {
    constructor() {
        this.api = new ApiClient('/api/v1');
        this.auth = new AuthManager(this.api);
        this.userTable = new UserTable(this.api, 'usersTableContainer');
        this.currentUser = null;
        this.currentViewingUserId = null;
        this.overview = new OverviewModule((...args) => this.apiCall(...args));
        this.activity = new ActivityModule((...args) => this.apiCall(...args));
        this.statistics = new StatisticsModule((...args) => this.apiCall(...args));
        this.users = new UsersModule((...args) => this.apiCall(...args), this.userTable);
        this.init();
    }

    init() {
        this.setupEventListeners();
        this.checkAuthentication();
    }

    setupEventListeners() {
        const loginForm = document.getElementById('loginForm');
        if (loginForm) {
            loginForm.addEventListener('submit', (e) => {
                e.preventDefault();
                this.login();
            });
        }
    }

    checkAuthentication() {
        if (this.auth.isAuthenticated()) {
            this.showDashboard();
            this.overview.loadOverview();
        } else {
            this.showLogin();
        }
    }

    showLogin() {
        const login = document.getElementById('loginSection');
        const dash = document.getElementById('dashboardSection');
        if (login) login.style.display = 'block';
        if (dash) dash.classList.remove('active');
    }

    showDashboard() {
        const login = document.getElementById('loginSection');
        const dash = document.getElementById('dashboardSection');
        if (login) login.style.display = 'none';
        if (dash) dash.classList.add('active');
    }

    async login() {
        const email = document.getElementById('email').value;
        const password = document.getElementById('password').value;
        const alertDiv = document.getElementById('loginAlert');

        try {
            const data = await this.auth.login(email, password);
            this.currentUser = data.user || null;
            const currentUserEl = document.getElementById('currentUser');
            if (currentUserEl && this.currentUser) {
                currentUserEl.textContent = this.currentUser.username || this.currentUser.email || '';
            }
            this.showDashboard();
            this.overview.loadOverview();
            if (alertDiv) alertDiv.style.display = 'none';
        } catch (error) {
            if (alertDiv) {
                alertDiv.textContent = error.message;
                alertDiv.style.display = 'block';
            }
        }
    }

    logout() {
        this.auth.logout();
        this.currentUser = null;
        this.showLogin();
    }

    async apiCall(endpoint, options = {}) {
        return this.api.call(endpoint, options);
    }

    async loadOverview() { await this.overview.loadOverview(); }

    async loadActivity(limit = 50) { await this.activity.loadActivity(limit); }

    renderActivitySection({ data = [], limit = 50 } = {}) {
        const container = document.getElementById('activityTableContainer');
        if (!container) return;
        const recentOperations = Array.isArray(data) ? data : [];

        const uniqueTypes = [...new Set(recentOperations.map(op => op.operationType).filter(Boolean))];

        let html = `
            <div class="mb-3 d-flex flex-wrap gap-2 align-items-end">
                <div class="flex-grow-1">
                    <label class="form-label mb-1">Recherche</label>
                    <input id="activitySearch" class="form-control form-control-custom" placeholder="Texte libre (type, détails, utilisateur, requestId, ip)">
                </div>
                <div>
                    <label class="form-label mb-1">Statut</label>
                    <select id="activityStatus" class="form-select form-control-custom">
                        <option value="">Tous</option>
                        <option value="SUCCESS">SUCCESS</option>
                        <option value="FAILURE">FAILURE</option>
                    </select>
                </div>
                <div>
                    <label class="form-label mb-1">Type</label>
                    <select id="activityType" class="form-select form-control-custom">
                        <option value="">Tous</option>
                        ${uniqueTypes.map(t => `<option value="${t}">${t}</option>`).join('')}
                    </select>
                </div>
                <div>
                    <label class="form-label mb-1">Limiter</label>
                    <select id="activityLimit" class="form-select form-control-custom">
                        ${[10,25,50,100].map(v => `<option value="${v}" ${v===Number(limit)?'selected':''}>${v}</option>`).join('')}
                    </select>
                </div>
                <div>
                    <button id="activityRefresh" class="btn btn-outline-primary btn-custom"><i class="fas fa-sync-alt me-2"></i>Actualiser</button>
                </div>
            </div>
            <div class="table-responsive">
                <table class="table table-sm table-hover">
                    <thead class="table-light">
                        <tr>
                            <th>Heure</th>
                            <th>Statut</th>
                            <th>Type</th>
                            <th>Utilisateur</th>
                            <th>IP</th>
                            <th>Durée (ms)</th>
                            <th>RequestId</th>
                            <th>Détails</th>
                        </tr>
                    </thead>
                    <tbody id="activityTbody"></tbody>
                </table>
            </div>
        `;

        container.innerHTML = html;

        const renderRows = () => {
            const tbody = document.getElementById('activityTbody');
            if (!tbody) return;
            const q = (document.getElementById('activitySearch')?.value || '').toLowerCase();
            const s = document.getElementById('activityStatus')?.value || '';
            const t = document.getElementById('activityType')?.value || '';
            const lim = Number(document.getElementById('activityLimit')?.value || limit);

            const filtered = recentOperations.filter(op => {
                if (s && String(op.status) !== s) return false;
                if (t && String(op.operationType) !== t) return false;
                if (q) {
                    const hay = [op.operationType, op.details, op.user, op.username, op.requestId, op.ip]
                        .filter(Boolean).join(' ').toLowerCase();
                    if (!hay.includes(q)) return false;
                }
                return true;
            }).slice(0, lim);

            if (filtered.length === 0) {
                tbody.innerHTML = '<tr><td colspan="8" class="text-center text-muted">Aucun log</td></tr>';
                return;
            }

            tbody.innerHTML = filtered.map(op => {
                const statusClass = op.status === 'SUCCESS' ? 'bg-success' : (op.status === 'FAILURE' ? 'bg-danger' : 'bg-secondary');
                const statusBadge = `<span class=\"badge ${statusClass}\">${op.status || '-'}</span>`;
                const duration = (op.durationMs != null) ? op.durationMs : (op.duration != null ? op.duration : '-');
                return `
                    <tr>
                        <td>${this.formatDate(op.timestamp)}</td>
                        <td>${statusBadge}</td>
                        <td>${op.operationType || '-'}</td>
                        <td>${op.user || op.username || '-'}</td>
                        <td>${op.ip || '-'}</td>
                        <td>${duration}</td>
                        <td><code>${op.requestId || '-'}</code></td>
                        <td>${op.details || op.message || '-'}</td>
                    </tr>
                `;
            }).join('');
        };

        renderRows();
        document.getElementById('activitySearch')?.addEventListener('input', renderRows);
        document.getElementById('activityStatus')?.addEventListener('change', renderRows);
        document.getElementById('activityType')?.addEventListener('change', renderRows);
        document.getElementById('activityLimit')?.addEventListener('change', renderRows);
        document.getElementById('activityRefresh')?.addEventListener('click', () => {
            const lim = Number(document.getElementById('activityLimit')?.value || limit);
            const el = document.getElementById('activityTableContainer');
            if (el) el.innerHTML = '<div class="spinner-border spinner-border-custom text-primary" role="status"></div><p class="mt-2">Chargement...</p>';
            this.loadActivity(lim);
        });
    }

    async loadStatistics() { await this.statistics.loadStatistics(); }

    renderStatisticsSection(data) { this.statistics.renderStatisticsSection(data); }

    updateOverviewStats(data) {
        const setText = (id, value) => {
            const el = document.getElementById(id);
            if (el) el.textContent = value;
        };
        setText('totalUsers', data && data.users ? (data.users.total || 0) : 0);
        setText('activeUsers', data && data.users ? (data.users.active || 0) : 0);
        setText('totalSessions', data && data.sessions ? (data.sessions.total || 0) : 0);
        setText('successRate', data && data.operations ? (data.operations.successRate || '0%') : '0%');
    }

    updateRecentActivity(data) {
        const container = document.getElementById('recentActivity');
        if (!container) return;
        if (data && data.recentOperations && data.recentOperations.length > 0) {
            let html = '<div class="list-group list-group-flush">';
            data.recentOperations.slice(0, 5).forEach(op => {
                const statusClass = op.status === 'SUCCESS' ? 'success' : 'danger';
                const icon = op.status === 'SUCCESS' ? 'check-circle' : 'exclamation-circle';
                html += `
                    <div class="list-group-item border-0 px-0">
                        <div class="d-flex align-items-center">
                            <i class="fas fa-${icon} text-${statusClass} me-2"></i>
                            <div class="flex-grow-1">
                                <small class="text-muted">${op.operationType}</small>
                                <div class="small">${op.details || 'Opération terminée'}</div>
                            </div>
                            <small class="text-muted">${this.formatDate(op.timestamp)}</small>
                        </div>
                    </div>
                `;
            });
            html += '</div>';
            container.innerHTML = html;
        } else {
            container.innerHTML = '<p class="text-muted text-center">Aucune activité récente</p>';
        }
    }

    updateSystemStats(data) {
        const container = document.getElementById('systemStats');
        if (!container) return;
        let html = '<div class="row g-3">';
        if (data && data.ttnOperations) {
            html += `
                <div class="col-6">
                    <div class="text-center p-2 bg-light rounded">
                        <div class="h5 mb-1">${data.ttnOperations.saveCount || 0}</div>
                        <small class="text-muted">TTN Save</small>
                    </div>
                </div>
                <div class="col-6">
                    <div class="text-center p-2 bg-light rounded">
                        <div class="h5 mb-1">${data.ttnOperations.consultCount || 0}</div>
                        <small class="text-muted">TTN Consult</small>
                    </div>
                </div>
            `;
        }
        if (data && data.anceSealOperations) {
            html += `
                <div class="col-6">
                    <div class="text-center p-2 bg-light rounded">
                        <div class="h5 mb-1">${data.anceSealOperations.signCount || 0}</div>
                        <small class="text-muted">ANCE Sign</small>
                    </div>
                </div>
                <div class="col-6">
                    <div class="text-center p-2 bg-light rounded">
                        <div class="h5 mb-1">${data.anceSealOperations.validateCount || 0}</div>
                        <small class="text-muted">ANCE Validate</small>
                    </div>
                </div>
            `;
        }
        html += '</div>';
        container.innerHTML = html;
    }

    async loadUsers(filters = {}) {
        await this.userTable.loadUsers(filters);
    }

    formatDate(dateString) {
        if (!dateString) return 'N/A';
        const date = new Date(dateString);
        return date.toLocaleDateString('fr-FR') + ' ' + date.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
    }

    async viewUser(userId) {
        try {
            const response = await this.apiCall(`/dashboard/users/${userId}`);
            if (response.success) {
                this.showUserModal(response.data);
            }
        } catch (e) {
            console.error('Error loading user details:', e);
        }
    }

    showUserModal(user) {
        const content = document.getElementById('userDetailsContent');
        if (!content) return;
        const credentialsStatus = user.hasCredentials ?
            '<span class="badge bg-success">Complet</span>' :
            '<span class="badge bg-warning">Incomplet</span>';
        const statusBadge = user.isActive ?
            '<span class="badge bg-success">Actif</span>' :
            '<span class="badge bg-danger">Inactif</span>';
        const verifiedBadge = user.isVerified ?
            '<span class="badge bg-success">Vérifié</span>' :
            '<span class="badge bg-warning">Non vérifié</span>';
        content.innerHTML = `
            <div class="row">
                <div class="col-md-6">
                    <div class="card">
                        <div class="card-header">
                            <h6><i class="fas fa-user me-2"></i>Informations personnelles</h6>
                        </div>
                        <div class="card-body">
                            <table class="table table-borderless">
                                <tr><td><strong>ID:</strong></td><td>${user.id}</td></tr>
                                <tr><td><strong>Nom d'utilisateur:</strong></td><td>${user.username}</td></tr>
                                <tr><td><strong>Email:</strong></td><td>${user.email}</td></tr>
                                <tr><td><strong>Prénom:</strong></td><td>${user.firstName}</td></tr>
                                <tr><td><strong>Nom:</strong></td><td>${user.lastName}</td></tr>
                                <tr><td><strong>Entreprise:</strong></td><td>${user.companyName || 'N/A'}</td></tr>
                                <tr><td><strong>Rôle:</strong></td><td><span class="badge ${user.role === 'ADMIN' ? 'bg-primary' : 'bg-secondary'}">${user.role}</span></td></tr>
                                <tr><td><strong>Statut:</strong></td><td>${statusBadge}</td></tr>
                                <tr><td><strong>Vérifié:</strong></td><td>${verifiedBadge}</td></tr>
                                <tr><td><strong>Identifiants:</strong></td><td>${credentialsStatus}</td></tr>
                            </table>
                        </div>
                    </div>
                </div>
                <div class="col-md-6">
                    <div class="card">
                        <div class="card-header">
                            <h6><i class="fas fa-clock me-2"></i>Informations de connexion</h6>
                        </div>
                        <div class="card-body">
                            <table class="table table-borderless">
                                <tr><td><strong>Créé le:</strong></td><td>${this.formatDate(user.createdAt)}</td></tr>
                                <tr><td><strong>Mis à jour le:</strong></td><td>${this.formatDate(user.updatedAt)}</td></tr>
                                <tr><td><strong>Dernière connexion:</strong></td><td>${user.lastLogin ? this.formatDate(user.lastLogin) : 'Jamais'}</td></tr>
                            </table>
                        </div>
                    </div>
                    <div class="card mt-3">
                        <div class="card-header">
                            <h6><i class="fas fa-key me-2"></i>Identifiants configurés</h6>
                        </div>
                        <div class="card-body">
                            <div class="row">
                                <div class="col-6">
                                    <div class="text-center p-2 ${user.ttnUsername ? 'bg-success' : 'bg-light'} rounded mb-2">
                                        <i class="fas fa-building ${user.ttnUsername ? 'text-white' : 'text-muted'}"></i>
                                        <div class="small ${user.ttnUsername ? 'text-white' : 'text-muted'}">TTN</div>
                                    </div>
                                </div>
                                <div class="col-6">
                                    <div class="text-center p-2 ${user.anceSealPin ? 'bg-success' : 'bg-light'} rounded mb-2">
                                        <i class="fas fa-certificate ${user.anceSealPin ? 'text-white' : 'text-muted'}"></i>
                                        <div class="small ${user.anceSealPin ? 'text-white' : 'text-muted'}">ANCE SEAL</div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        `;
        this.currentViewingUserId = user.id;
        new bootstrap.Modal(document.getElementById('userDetailsModal')).show();
    }

    async editUser(userId) {
        try {
            const response = await this.apiCall(`/dashboard/users/${userId}`);
            if (response.success) {
                const user = response.data;
                document.getElementById('editUserId').value = user.id;
                document.getElementById('editEmail').value = user.email;
                document.getElementById('editFirstName').value = user.firstName;
                document.getElementById('editLastName').value = user.lastName;
                document.getElementById('editCompany').value = user.companyName || '';
                document.getElementById('editRole').value = user.role;
                document.getElementById('editIsActive').checked = user.isActive;
                document.getElementById('editIsVerified').checked = user.isVerified;
                new bootstrap.Modal(document.getElementById('editUserModal')).show();
            }
        } catch (e) {
            console.error('Error loading user for edit:', e);
            alert('Erreur lors du chargement des données utilisateur');
        }
    }

    async manageCredentials(userId) {
        try {
            const response = await this.apiCall(`/dashboard/users/${userId}`);
            if (response.success) {
                const user = response.data;
                document.getElementById('credentialsUserId').value = user.id;
                document.getElementById('credTtnUsername').value = user.ttnUsername || '';
                document.getElementById('credTtnPassword').value = user.ttnPassword || '';
                document.getElementById('credTtnMatricule').value = user.ttnMatriculeFiscal || '';
                document.getElementById('credAncePin').value = user.anceSealPin || '';
                document.getElementById('credAnceAlias').value = user.anceSealAlias || '';
                document.getElementById('credCertPath').value = user.certificatePath || '';
                new bootstrap.Modal(document.getElementById('credentialsModal')).show();
            }
        } catch (e) {
            console.error('Error loading user credentials:', e);
            alert('Erreur lors du chargement des identifiants');
        }
    }

    async toggleUserStatus(userId) {
        if (!confirm('Êtes-vous sûr de vouloir changer le statut de cet utilisateur ?')) return;
        try {
            const response = await this.apiCall(`/dashboard/users/${userId}/toggle-status`, { method: 'PUT' });
            if (response.success) {
                alert(response.message);
                this.loadUsers();
            } else {
                alert('Erreur: ' + response.message);
            }
        } catch (e) {
            console.error('Error toggling user status:', e);
            alert('Erreur lors du changement de statut');
        }
    }

    async resetPassword(userId) {
        if (!confirm('Êtes-vous sûr de vouloir réinitialiser le mot de passe de cet utilisateur ?')) return;
        try {
            const response = await this.apiCall(`/dashboard/users/${userId}/reset-password`, { method: 'PUT' });
            if (response.success) {
                alert(`Mot de passe réinitialisé avec succès!\nNouveau mot de passe: ${response.newPassword}`);
            } else {
                alert('Erreur: ' + response.message);
            }
        } catch (e) {
            console.error('Error resetting password:', e);
            alert('Erreur lors de la réinitialisation du mot de passe');
        }
    }

    async deleteUser(userId) {
        if (!confirm('Êtes-vous sûr de vouloir supprimer cet utilisateur ? Cette action est irréversible.')) return;
        try {
            const response = await this.apiCall(`/dashboard/users/${userId}`, { method: 'DELETE' });
            if (response.success) {
                alert(response.message);
                this.loadUsers();
            } else {
                alert('Erreur: ' + response.message);
            }
        } catch (e) {
            console.error('Error deleting user:', e);
            alert('Erreur lors de la suppression de l\'utilisateur');
        }
    }

    async createUser(userData) {
        try {
            const response = await this.apiCall('/dashboard/users', { method: 'POST', body: JSON.stringify(userData) });
            if (response.success) {
                alert('Utilisateur créé avec succès!');
                const modal = bootstrap.Modal.getInstance(document.getElementById('createUserModal'));
                if (modal) modal.hide();
                this.loadUsers();
                const form = document.getElementById('createUserForm');
                if (form) form.reset();
            } else {
                alert('Erreur: ' + response.message);
            }
        } catch (e) {
            console.error('Error creating user:', e);
            alert('Erreur lors de la création de l\'utilisateur');
        }
    }

    async updateUser(userId, userData) {
        try {
            const response = await this.apiCall(`/dashboard/users/${userId}`, { method: 'PUT', body: JSON.stringify(userData) });
            if (response.success) {
                alert('Utilisateur mis à jour avec succès!');
                const modal = bootstrap.Modal.getInstance(document.getElementById('editUserModal'));
                if (modal) modal.hide();
                this.loadUsers();
            } else {
                alert('Erreur: ' + response.message);
            }
        } catch (e) {
            console.error('Error updating user:', e);
            alert('Erreur lors de la mise à jour de l\'utilisateur');
        }
    }

    async updateCredentials(userId, credentialsData) {
        try {
            const response = await this.apiCall(`/dashboard/users/${userId}/credentials`, { method: 'PUT', body: JSON.stringify(credentialsData) });
            if (response.success) {
                alert('Identifiants mis à jour avec succès!');
                const modal = bootstrap.Modal.getInstance(document.getElementById('credentialsModal'));
                if (modal) modal.hide();
                this.loadUsers();
            } else {
                alert('Erreur: ' + response.message);
            }
        } catch (e) {
            console.error('Error updating credentials:', e);
            alert('Erreur lors de la mise à jour des identifiants');
        }
    }

    async refreshOverview() {
        await this.loadOverview();
    }
}

export default AdminDashboard;


