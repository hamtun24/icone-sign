// IconeSign Admin Dashboard JavaScript
class AdminDashboard {
constructor() {
    this.baseUrl = '';  // ✅ all endpoints start here
    this.authUrl = `/auth`;
    this.dashboardUrl = `${this.baseUrl}/dashboard`;
    this.token = localStorage.getItem('admin_token');
    this.currentUser = null;
    this.init();
}

    init() {
        this.setupEventListeners();
        this.checkAuthentication();
    }

    setupEventListeners() {
        // Login form
        document.getElementById('loginForm').addEventListener('submit', (e) => {
            e.preventDefault();
            this.login();
        });
    }

    checkAuthentication() {
        if (this.token) {
            this.showDashboard();
            this.loadOverview();
        } else {
            this.showLogin();
        }
    }

    showLogin() {
        document.getElementById('loginSection').style.display = 'block';
        document.getElementById('dashboardSection').classList.remove('active');
        //document.getElementById('userDropdown').style.display = 'none';
    }

    showDashboard() {
        document.getElementById('loginSection').style.display = 'none';
        document.getElementById('dashboardSection').classList.add('active');
        //document.getElementById('userDropdown').style.display = 'block';
    }

    async login() {
        const email = document.getElementById('email').value;
        const password = document.getElementById('password').value;
        const alertDiv = document.getElementById('loginAlert');

        try {
            console.log("**************",`${this.authUrl}`,"***********")
            const response = await fetch(`${this.authUrl}/signin`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    usernameOrEmail: email,
                    password: password
                })
            });

            const data = await response.json();

            if (data.success && data.data && data.data.token) {
                this.token = data.data.token;
                localStorage.setItem('admin_token', this.token);
                this.currentUser = data.data.user;
                
                // Update UI
                document.getElementById('currentUser').textContent = 
                    this.currentUser.username || this.currentUser.email;
                
                this.showDashboard();
                this.loadOverview();
                
                alertDiv.style.display = 'none';
            } else {
                this.showError(alertDiv, data.message || 'Erreur de connexion');
            }
        } catch (error) {
            this.showError(alertDiv, 'Erreur réseau: ' + error.message);
        }
    }

    logout() {
        localStorage.removeItem('admin_token');
        this.token = null;
        this.currentUser = null;
        this.showLogin();
    }

    showError(element, message) {
        element.textContent = message;
        element.style.display = 'block';
    }

    async apiCall(endpoint, options = {}) {
        const config = {
            headers: {
                'Authorization': `Bearer ${this.token}`,
                'Content-Type': 'application/json',
                ...options.headers
            },
            ...options
        };

        // Support both /dashboard and /api/v1 endpoints
        let url = this.dashboardUrl ? `${this.dashboardUrl}${endpoint}` : `${this.baseUrl}${endpoint}`;
        if (endpoint.startsWith('/auth') || endpoint.startsWith('/dashboard')) {
            url = `${this.baseUrl}${endpoint}`;
        }

        const response = await fetch(url, config);

        if (response.status === 401) {
            this.logout();
            setTimeout(() => { window.location.reload(); }, 500);
            throw new Error('Session expirée');
        }

        return response.json();
    }



    async loadOverview() {
        try {
            // Load dashboard overview
            const overview = await this.apiCall('/overview');
            
            if (overview.success) {
                this.updateOverviewStats(overview.data);
            }

            // Load recent activity
            const activity = await this.apiCall('/activity?limit=10');
            if (activity.success) {
                this.updateRecentActivity(activity.data);
            }

            // Load system statistics
            const stats = await this.apiCall('/statistics');
            if (stats.success) {
                this.updateSystemStats(stats.data);
            }

        } catch (error) {
            console.error('Error loading overview:', error);
        }
    }

    updateOverviewStats(data) {
        // Update stat cards
        document.getElementById('totalUsers').textContent = data.users?.total || 0;
        document.getElementById('activeUsers').textContent = data.users?.active || 0;
        document.getElementById('totalSessions').textContent = data.sessions?.total || 0;
        document.getElementById('successRate').textContent = data.operations?.successRate || '0%';
    }

    updateRecentActivity(data) {
        const container = document.getElementById('recentActivity');
        
        if (data.recentOperations && data.recentOperations.length > 0) {
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
        
        let html = '<div class="row g-3">';
        
        // TTN Operations
        if (data.ttnOperations) {
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
        
        // ANCE Operations
        if (data.anceSealOperations) {
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

    async loadUsers() {
        try {
            const response = await this.apiCall('/users?page=0&size=50');
            
            if (response.success) {
                this.updateUsersTable(response.data);
            }
        } catch (error) {
            console.error('Error loading users:', error);
        }
    }

    updateUsersTable(users) {
        const container = document.getElementById('usersTableContainer');
        
        let html = `
            <div class="table-responsive">
                <table class="table table-hover">
                    <thead class="table-dark">
                        <tr>
                            <th>ID</th>
                            <th>Utilisateur</th>
                            <th>Email</th>
                            <th>Rôle</th>
                            <th>Statut</th>
                            <th>Identifiants</th>
                            <th>Dernière connexion</th>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
        `;
        
        users.forEach(user => {
            const statusBadge = user.isActive ? 
                '<span class="badge bg-success">Actif</span>' : 
                '<span class="badge bg-danger">Inactif</span>';
            
            const roleBadge = user.role === 'ADMIN' ? 
                '<span class="badge bg-primary badge-role">Admin</span>' : 
                '<span class="badge bg-secondary badge-role">Utilisateur</span>';
            
            const credentialsBadge = user.hasCredentials ? 
                '<span class="badge bg-success">Complet</span>' : 
                '<span class="badge bg-warning">Incomplet</span>';
            
            html += `
                <tr>
                    <td>${user.id}</td>
                    <td>
                        <div class="fw-bold">${user.firstName} ${user.lastName}</div>
                        <small class="text-muted">${user.username}</small>
                    </td>
                    <td>${user.email}</td>
                    <td>${roleBadge}</td>
                    <td>${statusBadge}</td>
                    <td>${credentialsBadge}</td>
                    <td>${user.lastLogin ? this.formatDate(user.lastLogin) : 'Jamais'}</td>
                    <td>
                        <button class="btn btn-sm btn-outline-primary" onclick="dashboard.viewUser(${user.id})">
                            <i class="fas fa-eye"></i>
                        </button>
                        <button class="btn btn-sm btn-outline-warning" onclick="dashboard.editUser(${user.id})">
                            <i class="fas fa-edit"></i>
                        </button>
                    </td>
                </tr>
            `;
        });
        
        html += '</tbody></table></div>';
        container.innerHTML = html;
    }

    formatDate(dateString) {
        if (!dateString) return 'N/A';
        const date = new Date(dateString);
        return date.toLocaleDateString('fr-FR') + ' ' + date.toLocaleTimeString('fr-FR', {
            hour: '2-digit',
            minute: '2-digit'
        });
    }

    async refreshOverview() {
        await this.loadOverview();
    }

    async viewUser(userId) {
        try {
            const response = await this.apiCall(`/users/${userId}`);
            
            if (response.success) {
                this.showUserModal(response.data);
            }
        } catch (error) {
            console.error('Error loading user details:', error);
        }
    }

    showUserModal(user) {
        // This would show a modal with user details
        alert(`Détails utilisateur:\nNom: ${user.firstName} ${user.lastName}\nEmail: ${user.email}\nRôle: ${user.role}`);
    }

    editUser(userId) {
        // This would show an edit modal
        alert(`Édition utilisateur ID: ${userId}`);
    }
}

// Global functions for HTML onclick events
function showSection(section) {
    // Hide all sections
    document.querySelectorAll('.content-section').forEach(el => {
        el.classList.remove('active');
    });
    
    // Remove active class from nav links
    document.querySelectorAll('.sidebar .nav-link').forEach(el => {
        el.classList.remove('active');
    });
    
    // Show selected section
    document.getElementById(section + 'Section').classList.add('active');
    
    // Add active class to clicked nav link
    event.target.classList.add('active');
    
    // Load section data
   switch(section) {
    case 'overview':
        dashboard.loadOverview();
        break;
    case 'users':
        dashboard.loadUsers();
        break;
    case 'sessions':
        dashboard.loadSessions();
        break;
    case 'activity':
        dashboard.loadActivity();
        break;
    case 'statistics':
        dashboard.loadStatistics();
        break;
    case 'settings':
        dashboard.loadSettings();
        break;
}
}

function refreshOverview() {
    dashboard.refreshOverview();
}

function logout() {
    dashboard.logout();
}



// Initialize dashboard when page loads
let dashboard;
document.addEventListener('DOMContentLoaded', function() {
    dashboard = new AdminDashboard();
});
