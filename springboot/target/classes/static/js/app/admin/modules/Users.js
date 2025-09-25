export default class UsersModule {
    constructor(apiCall, userTable) {
        this.apiCall = apiCall;
        this.userTable = userTable;
        this.currentViewingUserId = null;
    }

    async loadUsers(filters = {}) {
        await this.userTable.loadUsers(filters);
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

    formatDate(dateString) {
        if (!dateString) return 'N/A';
        const date = new Date(dateString);
        return date.toLocaleDateString('fr-FR') + ' ' + date.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
    }
}


