# Database Setup Instructions

## Your PostgreSQL Configuration
- **Host**: localhost
- **Port**: 5432
- **Database**: xmlsign_db
- **User**: postgres
- **Password**: Adelmaskot25

## Quick Setup Options

### Option 1: Automated Setup (Recommended)

#### Windows:
```bash
cd UnifiedOperationsApi/database
connect_and_setup.bat
```

#### Linux/Mac:
```bash
cd UnifiedOperationsApi/database
chmod +x connect_and_setup.sh
./connect_and_setup.sh
```

### Option 2: Manual Setup

#### Step 1: Connect to Database
```bash
psql -h localhost -p 5432 -U postgres -d xmlsign_db
```

#### Step 2: Execute Setup Script
```sql
\i setup_xmlsign_db.sql
```

#### Step 3: Verify Setup
```sql
-- Check tables
\dt

-- Check users
SELECT id, username, email, role FROM users;

-- Exit
\q
```

### Option 3: Command Line (One Command)
```bash
psql -h localhost -p 5432 -U postgres -d xmlsign_db -f setup_xmlsign_db.sql
```

## What the Setup Does

1. **Drops Existing Tables**: Cleans up any existing IconeSign tables
2. **Creates New Schema**: 
   - `users` - User accounts with encrypted credentials
   - `workflow_sessions` - Processing session tracking
   - `workflow_files` - Individual file processing
   - `operation_logs` - Audit trail
3. **Creates Indexes**: Optimizes database performance
4. **Creates Views**: Reporting and statistics views
5. **Inserts Test Data**: Two test users for immediate testing

## Test Users Created

### Admin User
- **Email**: `admin@xmlsign.com`
- **Password**: `admin123`
- **Role**: ADMIN
- **Has Credentials**: Yes (encrypted test credentials)

### Regular User
- **Email**: `user@xmlsign.com`
- **Password**: `user123`
- **Role**: USER
- **Has Credentials**: Yes (encrypted test credentials)

## Verification Commands

After setup, verify everything works:

```sql
-- Connect to database
psql -h localhost -p 5432 -U postgres -d xmlsign_db

-- Check all tables exist
SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' ORDER BY table_name;

-- Check users
SELECT id, username, email, role, is_verified, 
       CASE WHEN ttn_username IS NOT NULL THEN 'Yes' ELSE 'No' END as has_ttn_creds,
       CASE WHEN ance_seal_pin IS NOT NULL THEN 'Yes' ELSE 'No' END as has_ance_creds
FROM users;

-- Check views
SELECT * FROM user_stats;

-- Exit
\q
```

## Spring Boot Configuration

Your `application.properties` is already configured:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/xmlsign_db
spring.datasource.username=postgres
spring.datasource.password=Adelmaskot25
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
```

## Troubleshooting

### Connection Issues
```bash
# Test connection
psql -h localhost -p 5432 -U postgres -d xmlsign_db -c "SELECT version();"
```

### Permission Issues
```sql
-- Grant permissions if needed
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO postgres;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO postgres;
```

### Reset Database
```sql
-- If you need to start over
DROP SCHEMA public CASCADE;
CREATE SCHEMA public;
-- Then run setup_xmlsign_db.sql again
```

## Next Steps

1. **Run the setup script** using one of the options above
2. **Start your Spring Boot application**
3. **Test the frontend sign-in** with the test users
4. **Upload and process XML files** to test the workflow

The database is now ready for the IconeSign application with full user authentication and workflow tracking! 🚀
