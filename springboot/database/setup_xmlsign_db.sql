-- IconeSign Database Setup for xmlsign_db
-- PostgreSQL Database Schema Creation
-- Database: xmlsign_db
-- User: postgres
-- Password: Adelmaskot25

-- Connect to xmlsign_db database first:
-- psql -h localhost -p 5432 -U postgres -d xmlsign_db

-- =====================================================
-- DROP EXISTING TABLES (if they exist)
-- =====================================================
DROP TABLE IF EXISTS operation_logs CASCADE;
DROP TABLE IF EXISTS workflow_files CASCADE;
DROP TABLE IF EXISTS workflow_sessions CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- Drop views if they exist
DROP VIEW IF EXISTS user_stats CASCADE;
DROP VIEW IF EXISTS session_summary CASCADE;

-- Drop functions if they exist
DROP FUNCTION IF EXISTS update_updated_at_column() CASCADE;

-- =====================================================
-- ENABLE UUID EXTENSION
-- =====================================================
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
    
    -- Constraints
    CONSTRAINT users_username_length CHECK (LENGTH(username) >= 3),
    CONSTRAINT users_email_format CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$')
);

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

-- =====================================================
-- CREATE INDEXES
-- =====================================================

-- Users table indexes
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_active ON users(is_active);
CREATE INDEX idx_users_created_at ON users(created_at);

-- Workflow sessions indexes
CREATE INDEX idx_workflow_sessions_session_id ON workflow_sessions(session_id);
CREATE INDEX idx_workflow_sessions_user_id ON workflow_sessions(user_id);
CREATE INDEX idx_workflow_sessions_status ON workflow_sessions(status);
CREATE INDEX idx_workflow_sessions_created_at ON workflow_sessions(created_at);

-- Workflow files indexes
CREATE INDEX idx_workflow_files_session_id ON workflow_files(workflow_session_id);
CREATE INDEX idx_workflow_files_status ON workflow_files(status);
CREATE INDEX idx_workflow_files_stage ON workflow_files(stage);
CREATE INDEX idx_workflow_files_filename ON workflow_files(filename);

-- Operation logs indexes
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
-- SAMPLE DATA (Test Users)
-- =====================================================

-- Insert admin user (password: 'admin123' - BCrypt encoded)
INSERT INTO users (
    username, email, password, first_name, last_name, company_name, 
    role, is_verified, 
    ttn_username, ttn_password, ttn_matricule_fiscal,
    ance_seal_pin, certificate_path
) VALUES (
    'admin', 
    'admin@iconeSign.com', 
    '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', -- password: 'admin123'
    'Admin', 
    'User', 
    'XMLSign Technologies',
    'ADMIN', 
    TRUE,
    'encrypted_ttn_username_admin',
    'encrypted_ttn_password_admin', 
    'encrypted_matricule_fiscal_admin',
    'encrypted_ance_pin_admin',
    'resources/certificates/admin_cert.p12'
);

-- Insert test user (password: 'user123' - BCrypt encoded)
INSERT INTO users (
    username, email, password, first_name, last_name, company_name,
    is_verified,
    ttn_username, ttn_password, ttn_matricule_fiscal,
    ance_seal_pin, certificate_path
) VALUES (
    'testuser', 
    'user@xmlsign.com', 
    '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', -- password: 'user123'
    'Test', 
    'User', 
    'Test Company SARL',
    TRUE,
    'encrypted_ttn_username_test',
    'encrypted_ttn_password_test',
    'encrypted_matricule_fiscal_test', 
    'encrypted_ance_pin_test',
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

-- =====================================================
-- VERIFICATION QUERIES
-- =====================================================

-- Verify tables were created
SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' ORDER BY table_name;

-- Verify users were inserted
SELECT id, username, email, role, is_verified FROM users;

-- Show table sizes
SELECT 
    schemaname,
    tablename,
    attname,
    n_distinct,
    correlation
FROM pg_stats 
WHERE schemaname = 'public' 
ORDER BY tablename, attname;

-- End of setup script
