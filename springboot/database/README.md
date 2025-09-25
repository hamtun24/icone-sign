# IconeSign Database Setup

This directory contains the PostgreSQL database schema and setup scripts for the IconeSign application.

## Prerequisites

- PostgreSQL 12+ installed and running
- Access to PostgreSQL superuser account (usually `postgres`)

## Quick Setup

### 1. Create Database and User

Connect to PostgreSQL as superuser and run:

```bash
psql -U postgres -f setup.sql
```

### 2. Create Schema

Connect to the iconesign_db database and create the schema:

```bash
psql -U iconesign_user -d iconesign_db -f schema.sql
```

## Manual Setup

### Step 1: Create Database and User

```sql
-- Connect as postgres superuser
psql -U postgres

-- Create database
CREATE DATABASE iconesign_db;

-- Create user
CREATE USER iconesign_user WITH PASSWORD 'iconesign_password';

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE iconesign_db TO iconesign_user;

-- Exit
\q
```

### Step 2: Connect to Database

```bash
psql -U iconesign_user -d iconesign_db
```

### Step 3: Run Schema Script

```sql
-- Copy and paste the contents of schema.sql
-- Or use: \i schema.sql
```

## Database Structure

### Core Tables

1. **users** - User accounts with encrypted credentials
   - Basic user information (username, email, password)
   - Encrypted TTN credentials (username, password, matricule fiscal)
   - Encrypted ANCE SEAL credentials (PIN, alias, certificate path)
   - User status and metadata

2. **workflow_sessions** - Workflow processing sessions
   - Session tracking with unique session IDs
   - Overall progress and status
   - File counts and completion statistics
   - Error messages and download URLs

3. **workflow_files** - Individual files within sessions
   - File metadata (name, size)
   - Processing status and progress per stage
   - Result paths (signed XML, validation reports, HTML)
   - TTN invoice IDs

4. **operation_logs** - Audit trail
   - All operations performed
   - Success/failure tracking
   - Performance metrics

### Views

- **user_stats** - User statistics and activity summary
- **session_summary** - Session processing summary with duration

## Default Users

The schema includes two test users:

### Admin User
- **Username**: `admin`
- **Email**: `admin@iconesign.com`
- **Password**: `admin123`
- **Role**: ADMIN

### Test User
- **Username**: `testuser`
- **Email**: `user@iconesign.com`
- **Password**: `user123`
- **Role**: USER

## Configuration

Update your `application.properties` with:

```properties
# PostgreSQL Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/iconesign_db
spring.datasource.username=iconesign_user
spring.datasource.password=iconesign_password
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA Configuration
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
```

## Security Notes

1. **Credentials Encryption**: All TTN and ANCE SEAL credentials are encrypted using AES encryption
2. **Password Hashing**: User passwords are hashed using BCrypt
3. **JWT Authentication**: API access is secured with JWT tokens
4. **Database Security**: Use strong passwords and restrict database access

## Maintenance

### Backup Database

```bash
pg_dump -U iconesign_user iconesign_db > iconesign_backup.sql
```

### Restore Database

```bash
psql -U iconesign_user -d iconesign_db < iconesign_backup.sql
```

### Monitor Performance

```sql
-- Check active sessions
SELECT * FROM session_summary WHERE status = 'PROCESSING';

-- User activity
SELECT * FROM user_stats ORDER BY total_sessions DESC;

-- Recent operations
SELECT * FROM operation_logs ORDER BY created_at DESC LIMIT 50;
```

## Troubleshooting

### Connection Issues

1. Check PostgreSQL is running: `sudo systemctl status postgresql`
2. Verify database exists: `psql -U postgres -l`
3. Test connection: `psql -U iconesign_user -d iconesign_db`

### Permission Issues

```sql
-- Grant missing permissions
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO iconesign_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO iconesign_user;
```

### Reset Database

```sql
-- Drop and recreate (WARNING: This deletes all data)
DROP DATABASE iconesign_db;
-- Then run setup.sql and schema.sql again
```

## Development

For development, you may want to:

1. Set `spring.jpa.hibernate.ddl-auto=update` to auto-create tables
2. Enable SQL logging: `spring.jpa.show-sql=true`
3. Use H2 in-memory database for testing

## Production Deployment

1. Use strong, unique passwords
2. Set up SSL/TLS connections
3. Configure connection pooling
4. Set up regular backups
5. Monitor database performance
6. Use `spring.jpa.hibernate.ddl-auto=validate` in production
