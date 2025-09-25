-- IconeSign Database Schema
-- PostgreSQL Database Creation Script
-- Version: 1.0
-- Date: 2024-08-24

-- Create database (run this separately as superuser)
-- CREATE DATABASE iconesign_db;
-- CREATE USER iconesign_user WITH PASSWORD 'iconesign_password';
-- GRANT ALL PRIVILEGES ON DATABASE iconesign_db TO iconesign_user;

-- Connect to iconesign_db database before running the rest

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- =====================================================
-- USERS TABLE
-- =====================================================
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    company_name VARCHAR(255),
    
    -- TTN Credentials (encrypted)
    ttn_username TEXT,
    ttn_password TEXT,
    ttn_matricule_fiscal TEXT,
    
    -- ANCE SEAL Credentials (encrypted)
    ance_seal_pin TEXT,
    ance_seal_alias TEXT,
    certificate_path VARCHAR(500),
    
    -- User status and metadata
    is_active BOOLEAN DEFAULT TRUE,
    is_verified BOOLEAN DEFAULT FALSE,
    role VARCHAR(20) DEFAULT 'USER' CHECK (role IN ('USER', 'ADMIN')),
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP,
    
    -- Indexes
    CONSTRAINT users_username_length CHECK (LENGTH(username) >= 3),
    CONSTRAINT users_email_format CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$')
);

-- Create indexes for users table
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_active ON users(is_active);
CREATE INDEX idx_users_created_at ON users(created_at);

-- =====================================================
-- WORKFLOW SESSIONS TABLE
-- =====================================================
CREATE TABLE workflow_sessions (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(255) UNIQUE NOT NULL,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    
    -- Session status
    status VARCHAR(20) DEFAULT 'INITIALIZING' CHECK (status IN ('INITIALIZING', 'PROCESSING', 'COMPLETED', 'FAILED')),
    current_stage VARCHAR(20) DEFAULT 'SIGN' CHECK (current_stage IN ('SIGN', 'SAVE', 'VALIDATE', 'TRANSFORM', 'PACKAGE')),
    
    -- Progress tracking
    overall_progress INTEGER DEFAULT 0 CHECK (overall_progress >= 0 AND overall_progress <= 100),
    total_files INTEGER DEFAULT 0,
    successful_files INTEGER DEFAULT 0,
    failed_files INTEGER DEFAULT 0,
    
    -- Messages and results
    message TEXT,
    error_message TEXT,
    zip_download_url VARCHAR(500),
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);

-- Create indexes for workflow_sessions table
CREATE INDEX idx_workflow_sessions_session_id ON workflow_sessions(session_id);
CREATE INDEX idx_workflow_sessions_user_id ON workflow_sessions(user_id);
CREATE INDEX idx_workflow_sessions_status ON workflow_sessions(status);
CREATE INDEX idx_workflow_sessions_created_at ON workflow_sessions(created_at);

-- =====================================================
-- WORKFLOW FILES TABLE
-- =====================================================
CREATE TABLE workflow_files (
    id BIGSERIAL PRIMARY KEY,
    workflow_session_id BIGINT NOT NULL REFERENCES workflow_sessions(id) ON DELETE CASCADE,
    
    -- File information
    filename VARCHAR(255) NOT NULL,
    file_size BIGINT,
    
    -- Processing status
    status VARCHAR(20) DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')),
    stage VARCHAR(20) DEFAULT 'SIGN' CHECK (stage IN ('SIGN', 'SAVE', 'VALIDATE', 'TRANSFORM', 'PACKAGE')),
    progress INTEGER DEFAULT 0 CHECK (progress >= 0 AND progress <= 100),
    
    -- Error handling
    error_message TEXT,
    
    -- Processing results
    ttn_invoice_id VARCHAR(100),
    signed_xml_path VARCHAR(500),
    validation_report_path VARCHAR(500),
    html_report_path VARCHAR(500),
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);

-- Create indexes for workflow_files table
CREATE INDEX idx_workflow_files_session_id ON workflow_files(workflow_session_id);
CREATE INDEX idx_workflow_files_status ON workflow_files(status);
CREATE INDEX idx_workflow_files_stage ON workflow_files(stage);
CREATE INDEX idx_workflow_files_filename ON workflow_files(filename);

-- =====================================================
-- OPERATION LOGS TABLE (for audit trail)
-- =====================================================
CREATE TABLE operation_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    session_id VARCHAR(255),
    
    -- Operation details
    operation_type VARCHAR(50) NOT NULL,
    operation_status VARCHAR(20) NOT NULL CHECK (operation_status IN ('SUCCESS', 'FAILURE', 'WARNING')),
    file_size BIGINT,
    filename VARCHAR(255),
    
    -- Additional details
    details TEXT,
    error_message TEXT,
    processing_time_ms BIGINT,
    
    -- Timestamp
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for operation_logs table
CREATE INDEX idx_operation_logs_user_id ON operation_logs(user_id);
CREATE INDEX idx_operation_logs_session_id ON operation_logs(session_id);
CREATE INDEX idx_operation_logs_operation_type ON operation_logs(operation_type);
CREATE INDEX idx_operation_logs_created_at ON operation_logs(created_at);

-- =====================================================
-- TRIGGERS FOR AUTOMATIC TIMESTAMP UPDATES
-- =====================================================

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create triggers for updated_at
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_workflow_sessions_updated_at BEFORE UPDATE ON workflow_sessions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_workflow_files_updated_at BEFORE UPDATE ON workflow_files
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- =====================================================
-- SAMPLE DATA (Optional - for testing)
-- =====================================================

-- Insert a test admin user (password: 'admin123' - BCrypt encoded)
INSERT INTO users (
    username, email, password, first_name, last_name, company_name, 
    role, is_verified, 
    ttn_username, ttn_password, ttn_matricule_fiscal,
    ance_seal_pin, certificate_path
) VALUES (
    'admin', 
    'admin@iconesign.com', 
    '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', -- password: 'admin123'
    'Admin', 
    'User', 
    'IconeSign Technologies',
    'ADMIN', 
    TRUE,
    'encrypted_ttn_username_here',
    'encrypted_ttn_password_here', 
    'encrypted_matricule_fiscal_here',
    'encrypted_ance_pin_here',
    'resources/certificates/admin_cert.p12'
);

-- Insert a test regular user (password: 'user123' - BCrypt encoded)
INSERT INTO users (
    username, email, password, first_name, last_name, company_name,
    is_verified,
    ttn_username, ttn_password, ttn_matricule_fiscal,
    ance_seal_pin, certificate_path
) VALUES (
    'testuser', 
    'user@iconesign.com', 
    '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', -- password: 'user123'
    'Test', 
    'User', 
    'Test Company SARL',
    TRUE,
    'encrypted_ttn_username_here',
    'encrypted_ttn_password_here',
    'encrypted_matricule_fiscal_here', 
    'encrypted_ance_pin_here',
    'resources/certificates/test_cert.p12'
);

-- =====================================================
-- VIEWS FOR REPORTING
-- =====================================================

-- User statistics view
CREATE VIEW user_stats AS
SELECT 
    u.id,
    u.username,
    u.email,
    u.company_name,
    u.created_at,
    u.last_login,
    COUNT(ws.id) as total_sessions,
    COUNT(CASE WHEN ws.status = 'COMPLETED' THEN 1 END) as completed_sessions,
    COUNT(CASE WHEN ws.status = 'FAILED' THEN 1 END) as failed_sessions,
    COALESCE(SUM(ws.total_files), 0) as total_files_processed
FROM users u
LEFT JOIN workflow_sessions ws ON u.id = ws.user_id
GROUP BY u.id, u.username, u.email, u.company_name, u.created_at, u.last_login;

-- Session summary view
CREATE VIEW session_summary AS
SELECT 
    ws.session_id,
    ws.status,
    ws.current_stage,
    ws.overall_progress,
    ws.total_files,
    ws.successful_files,
    ws.failed_files,
    ws.created_at,
    ws.completed_at,
    u.username,
    u.company_name,
    EXTRACT(EPOCH FROM (COALESCE(ws.completed_at, CURRENT_TIMESTAMP) - ws.created_at)) as duration_seconds
FROM workflow_sessions ws
JOIN users u ON ws.user_id = u.id;

-- =====================================================
-- GRANTS AND PERMISSIONS
-- =====================================================

-- Grant permissions to iconesign_user
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO iconesign_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO iconesign_user;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO iconesign_user;

-- Grant permissions on views
GRANT SELECT ON user_stats TO iconesign_user;
GRANT SELECT ON session_summary TO iconesign_user;

-- =====================================================
-- COMMENTS FOR DOCUMENTATION
-- =====================================================

COMMENT ON TABLE users IS 'User accounts with encrypted credentials for TTN and ANCE SEAL';
COMMENT ON TABLE workflow_sessions IS 'Workflow processing sessions tracking overall progress';
COMMENT ON TABLE workflow_files IS 'Individual files within workflow sessions';
COMMENT ON TABLE operation_logs IS 'Audit trail for all operations performed';

COMMENT ON COLUMN users.ttn_username IS 'Encrypted TTN username';
COMMENT ON COLUMN users.ttn_password IS 'Encrypted TTN password';
COMMENT ON COLUMN users.ance_seal_pin IS 'Encrypted ANCE SEAL PIN';
COMMENT ON COLUMN workflow_sessions.session_id IS 'Unique session identifier for API tracking';
COMMENT ON COLUMN workflow_files.ttn_invoice_id IS 'TTN invoice ID extracted from SOAP response';

-- End of schema creation script
