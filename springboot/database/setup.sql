-- IconeSign Database Setup Script
-- Run this as PostgreSQL superuser (postgres)

-- Create database
CREATE DATABASE iconesign_db
    WITH 
    OWNER = postgres
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.UTF-8'
    LC_CTYPE = 'en_US.UTF-8'
    TABLESPACE = pg_default
    CONNECTION LIMIT = -1;

-- Create user
CREATE USER iconesign_user WITH
    LOGIN
    NOSUPERUSER
    CREATEDB
    NOCREATEROLE
    INHERIT
    NOREPLICATION
    CONNECTION LIMIT -1
    PASSWORD 'iconesign_password';

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE iconesign_db TO iconesign_user;

-- Connect to the database (you'll need to do this manually)
-- \c iconesign_db

-- Grant schema privileges
GRANT ALL ON SCHEMA public TO iconesign_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO iconesign_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO iconesign_user;
