class AuthManager {
    constructor(apiClient) {
        this.apiClient = apiClient;
    }

    async login(email, password) {
        const response = await this.apiClient.call('/auth/signin', {
            method: 'POST',
            body: JSON.stringify({
                usernameOrEmail: email,
                password: password
            })
        });

        if (response.success && response.data && response.data.token) {
            this.apiClient.setToken(response.data.token);
            return response.data;
        }
        throw new Error(response.message || 'Erreur de connexion');
    }

    logout() {
        this.apiClient.removeToken();
    }

    isAuthenticated() {
        return !!this.apiClient.token;
    }
}

export default AuthManager;