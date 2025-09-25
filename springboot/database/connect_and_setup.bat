@echo off
echo =====================================================
echo IconeSign Database Setup for xmlsign_db
echo =====================================================
echo.
echo Database Configuration:
echo Host: localhost
echo Port: 5432
echo Database: xmlsign_db
echo User: postgres
echo Password: Adelmaskot25
echo.
echo This script will:
echo 1. Connect to your PostgreSQL database
echo 2. Drop existing tables (if any)
echo 3. Create new tables for IconeSign
echo 4. Insert test users
echo 5. Create indexes and views
echo.
pause

echo.
echo Connecting to PostgreSQL and executing setup script...
echo.

psql -h localhost -p 5432 -U postgres -d xmlsign_db -f setup_xmlsign_db.sql

echo.
echo =====================================================
echo Setup completed!
echo =====================================================
echo.
echo Test Users Created:
echo 1. Admin User:
echo    - Email: admin@xmlsign.com
echo    - Password: admin123
echo    - Role: ADMIN
echo.
echo 2. Test User:
echo    - Email: user@xmlsign.com  
echo    - Password: user123
echo    - Role: USER
echo.
echo You can now start your Spring Boot application!
echo.
pause
