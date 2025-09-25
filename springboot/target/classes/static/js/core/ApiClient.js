class ApiClient {
    constructor(baseUrl = '/api/v1') {
        this.baseUrl = baseUrl;
        this.token = localStorage.getItem('admin_token');
    }

    setToken(token) {
        this.token = token;
        localStorage.setItem('admin_token', token);
    }

    removeToken() {
        this.token = null;
        localStorage.removeItem('admin_token');
    }

    async call(endpoint, options = {}) {
        const config = {
            headers: {
                'Content-Type': 'application/json',
                ...options.headers
            },
            ...options
        };

        if (this.token) {
            config.headers['Authorization'] = `Bearer ${this.token}`;
        }

        let url = `${this.baseUrl}${endpoint}`;
        const response = await fetch(url, config);

        if (response.status === 401) {
            this.removeToken();
            throw new Error('Session expirée');
        }

        return response.json();
    }
}

export default ApiClient;