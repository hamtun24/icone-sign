export default class OverviewModule {
    constructor(apiCall) {
        this.apiCall = apiCall;
    }

    async loadOverview() {
        try {
            const [overview, activity, stats] = await Promise.all([
                this.apiCall('/dashboard/overview'),
                this.apiCall('/dashboard/activity?limit=10'),
                this.apiCall('/dashboard/statistics')
            ]);

            if (overview && overview.success) this.updateOverviewStats(overview.data);
            if (activity && activity.success) this.updateRecentActivity(activity.data);
            if (stats && stats.success) this.updateSystemStats(stats.data);
        } catch (error) {
            console.error('Error loading overview:', error);
        }
    }

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

    formatDate(dateString) {
        if (!dateString) return 'N/A';
        const date = new Date(dateString);
        return date.toLocaleDateString('fr-FR') + ' ' + date.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
    }
}


