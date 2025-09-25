export default class ActivityModule {
    constructor(apiCall) {
        this.apiCall = apiCall;
        this._activityAll = [];
    }

    async loadActivity(limit = 50) {
        try {
            const response = await this.apiCall(`/dashboard/activity?limit=${encodeURIComponent(limit)}`);
            if (response && response.success) {
                this._activityAll = Array.isArray(response.data?.recentOperations) ? response.data.recentOperations : [];
                this.renderActivitySection({ data: this._activityAll, limit });
            }
        } catch (e) {
            console.error('Error loading activity:', e);
        }
    }

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

    formatDate(dateString) {
        if (!dateString) return 'N/A';
        const date = new Date(dateString);
        return date.toLocaleDateString('fr-FR') + ' ' + date.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
    }
}


