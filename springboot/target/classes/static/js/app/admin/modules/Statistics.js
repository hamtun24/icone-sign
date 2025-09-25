export default class StatisticsModule {
    constructor(apiCall) {
        this.apiCall = apiCall;
    }

    async loadStatistics() {
        try {
            const response = await this.apiCall('/dashboard/statistics');
            if (response && response.success) {
                this.renderStatisticsSection(response.data);
            }
        } catch (e) {
            console.error('Error loading statistics:', e);
        }
    }

    renderStatisticsSection(data) {
        const userStatsEl = document.getElementById('userStatistics');
        const opStatsEl = document.getElementById('operationStatistics');
        if (userStatsEl) {
            const us = data?.userStatistics || {};
            userStatsEl.innerHTML = `
                <div class="row g-3">
                    <div class="col-6">
                        <div class="p-3 bg-light rounded text-center">
                            <div class="h4 mb-1">${us.totalUsers || 0}</div>
                            <small class="text-muted">Utilisateurs</small>
                        </div>
                    </div>
                    <div class="col-6">
                        <div class="p-3 bg-light rounded text-center">
                            <div class="h4 mb-1">${us.activeUsers || 0}</div>
                            <small class="text-muted">Actifs</small>
                        </div>
                    </div>
                    <div class="col-6">
                        <div class="p-3 bg-light rounded text-center">
                            <div class="h4 mb-1">${us.verifiedUsers || 0}</div>
                            <small class="text-muted">Vérifiés</small>
                        </div>
                    </div>
                    <div class="col-6">
                        <div class="p-3 bg-light rounded text-center">
                            <div class="h4 mb-1">${(us.inactiveUsers != null ? us.inactiveUsers : (us.totalUsers || 0) - (us.activeUsers || 0))}</div>
                            <small class="text-muted">Inactifs</small>
                        </div>
                    </div>
                </div>
            `;
        }
        if (opStatsEl) {
            const ttn = data?.ttnOperations || {};
            const ance = data?.anceSealOperations || {};
            const wfs = data?.workflowStatistics || {};
            opStatsEl.innerHTML = `
                <div class="row g-3">
                    <div class="col-md-4">
                        <div class="card h-100">
                            <div class="card-header">TTN</div>
                            <div class="card-body">
                                <div class="d-flex justify-content-between"><span>Save</span><strong>${ttn.saveCount || 0}</strong></div>
                                <div class="d-flex justify-content-between"><span>Consult</span><strong>${ttn.consultCount || 0}</strong></div>
                            </div>
                        </div>
                    </div>
                    <div class="col-md-4">
                        <div class="card h-100">
                            <div class="card-header">ANCE SEAL</div>
                            <div class="card-body">
                                <div class="d-flex justify-content-between"><span>Sign</span><strong>${ance.signCount || 0}</strong></div>
                                <div class="d-flex justify-content-between"><span>Validate</span><strong>${ance.validateCount || 0}</strong></div>
                                <div class="d-flex justify-content-between"><span>Batch Sign</span><strong>${ance.batchSignCount || 0}</strong></div>
                            </div>
                        </div>
                    </div>
                    <div class="col-md-4">
                        <div class="card h-100">
                            <div class="card-header">Workflows</div>
                            <div class="card-body">
                                <div class="d-flex justify-content-between"><span>Complétés</span><strong>${wfs.completed || 0}</strong></div>
                                <div class="d-flex justify-content-between"><span>Échec</span><strong>${wfs.failed || 0}</strong></div>
                                <div class="d-flex justify-content-between"><span>En cours</span><strong>${wfs.processing || 0}</strong></div>
                            </div>
                        </div>
                    </div>
                </div>
            `;
        }
    }
}


