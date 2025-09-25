// Global dashboard instance
let dashboardInstance;

// Expose global functions used by onclick handlers
window.refreshOverview = () => {
    dashboardInstance?.refreshOverview();
};

window.logout = () => {
    dashboardInstance?.logout();
};

window.showSection = (section) => {
    dashboardInstance?.showSection(section);
};

window.viewUser = (userId) => {
    dashboardInstance?.viewUser(userId);
};

window.editUser = (userId) => {
    dashboardInstance?.editUser(userId);
};

export function initGlobalDashboard(dashboard) {
    dashboardInstance = dashboard;
}