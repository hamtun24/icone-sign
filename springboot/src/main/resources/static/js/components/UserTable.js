class UserTable {
    constructor(apiClient, containerId) {
        this.apiClient = apiClient;
        this.containerId = containerId;
    }

    async loadUsers(filters = {}) {
        try {
            const params = new URLSearchParams();
            params.set('page', filters.page != null ? filters.page : 0);
            params.set('size', filters.size != null ? filters.size : 50);
            if (filters.search) params.set('search', filters.search);
            if (filters.role) params.set('role', filters.role);
            if (typeof filters.isActive === 'boolean') params.set('isActive', String(filters.isActive));
            const response = await this.apiClient.call(`/dashboard/users?${params.toString()}`);
            if (response.success) {
                this.render(response.data);
            }
        } catch (error) {
            console.error('Error loading users:', error);
        }
    }

    render(users) {
        const container = document.getElementById(this.containerId);
        if (!container) return;

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

            const statusToggleClass = user.isActive ? 'btn-outline-danger' : 'btn-outline-success';
            const statusToggleIcon = user.isActive ? 'fa-ban' : 'fa-check';
            const statusToggleText = user.isActive ? 'Désactiver' : 'Activer';

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
                        <div class="btn-group" role="group">
                            <button class="btn btn-sm btn-outline-primary" onclick="dashboard.viewUser(${user.id})" title="Voir détails">
                                <i class="fas fa-eye"></i>
                            </button>
                            <button class="btn btn-sm btn-outline-warning" onclick="dashboard.editUser(${user.id})" title="Modifier">
                                <i class="fas fa-edit"></i>
                            </button>
                            <button class="btn btn-sm btn-outline-info" onclick="dashboard.manageCredentials(${user.id})" title="Identifiants">
                                <i class="fas fa-key"></i>
                            </button>
                            <button class="btn btn-sm ${statusToggleClass}" onclick="dashboard.toggleUserStatus(${user.id})" title="${statusToggleText}">
                                <i class="fas ${statusToggleIcon}"></i>
                            </button>
                            <div class="btn-group" role="group">
                                <button class="btn btn-sm btn-outline-secondary dropdown-toggle" data-bs-toggle="dropdown">
                                    <i class="fas fa-ellipsis-v"></i>
                                </button>
                                <ul class="dropdown-menu">
                                    <li><a class="dropdown-item" href="#" onclick="dashboard.resetPassword(${user.id})">
                                        <i class="fas fa-key me-2"></i>Réinitialiser mot de passe
                                    </a></li>
                                    <li><hr class="dropdown-divider"></li>
                                    <li><a class="dropdown-item text-danger" href="#" onclick="dashboard.deleteUser(${user.id})">
                                        <i class="fas fa-trash me-2"></i>Supprimer
                                    </a></li>
                                </ul>
                            </div>
                        </div>
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
}

export default UserTable;