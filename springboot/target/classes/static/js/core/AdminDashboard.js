import ApiClient from './core/ApiClient.js';
import AuthManager from './core/AuthManager.js';
import UserTable from './components/UserTable.js';

class AdminDashboard {
    constructor() {
        this.apiClient = new ApiClient('/api/v1');
        this.authManager = new AuthManager(this.apiClient);
        this.userTable = new UserTable(this.apiClient, 'usersTableContainer');
        this.init();
    }

    init() {
        this.setupEventListeners();
        this.checkAuthentication();
    }

    setupEventListeners() {
        document.getElementById('loginForm')?.addEventListener('submit', (e) => {
            e.preventDefault();
            this.login();
        });
    }

    checkAuthentication() {
        if (this.authManager.isAuthenticated()) {
            this.showDashboard();
            this.loadDashboard();
        } else {
            this.showLogin();
        }
    }

    showLogin() {
        document.getElementById('loginSection').style.display = 'block';
        document.getElementById('dashboardSection').classList.remove('active');
    }

    showDashboard() {
        document.getElementById('loginSection').style.display = 'none';
        document.getElementById('dashboardSection').classList.add('active');
    }

    async login() {
        const email = document.getElementById('email').value;
        const password = document.getElementById('password').value;
        const alertDiv = document.getElementById('loginAlert');

        try {
            await this.authManager.login(email, password);
            this.showDashboard();
            this.loadDashboard();
        } catch (error) {
            alertDiv.textContent = error.message;
            alertDiv.style.display = 'block';
        }
    }

    logout() {
        this.authManager.logout();
        this.showLogin();
    }

    async loadDashboard() {
        await Promise.all([
            this.loadOverview(),
            this.userTable.loadUsers()
        ]);
    }

    // ... other methods remain the same ...
}

export default AdminDashboard;