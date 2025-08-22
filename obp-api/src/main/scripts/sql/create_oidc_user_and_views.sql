-- HOW TO RUN THIS SCRIPT

-- For those of us that don't use postgres every day:

-- 1) You will need to have access to a postgres user that can create roles and views etc.
-- 2) You will probably want that postgres user to have easy access to your file system so you can run this script and tweak it if need be.

--That means.

--1) You probably want to have a postgres user with the same name as your linux or mac username.

--So:


--sudo -u postgres psql

--CREATE ROLE <YOURLINUXUSERNAME> WITH LOGIN SUPERUSER CREATEDB CREATEROLE;


--this step is not required but

--CREATE DATABASE <YOURLINUXUSERNAME> OWNER <YOURLINUXUSERNAME>;

--now quit with \q

--now psql

--now you will be logged in and have access to your normal home directory.

--now connect to the OBP database you want e.g.:

--\c sandbox

--now run the script from within the psql shell:

--\i ~/Documents/workspace_2024/OBP-API-C/OBP-API/obp-api/src/main/scripts/sql/create_oidc_user_and_views.sql


--or run it from the linux terminal specifying the database

--psql -d sandbox -f ~/Documents/workspace_2024/OBP-API-C/OBP-API/obp-api/src/main/scripts/sql/create_oidc_user_and_views.sql

--either way, check the output of the script carefully.

--you might want to login as the oidc_user and try the two views you have access to.

-- =============================================================================
-- OBP-API OIDC User Setup Script
-- =============================================================================
-- This script creates a dedicated OIDC database user and provides read-only
-- access to the authuser table via a view.
--
-- ⚠️  SECURITY WARNING: This view exposes password hashes and salts!
-- Only run this script if you understand the security implications.
--
-- Prerequisites:
-- 1. Run this script as a PostgreSQL superuser or database owner
-- 2. Ensure the OBP database exists and authuser table is created
-- 3. Update the database connection parameters below as needed
-- 4. IMPORTANT: Review and implement additional security measures below
--
-- Required Security Measures:
-- 1. Use SSL/TLS encrypted connections to the database
-- 2. Restrict database access by IP address in pg_hba.conf
-- 3. Use a very strong password for the OIDC user
-- 4. Monitor and audit access to this view
-- 5. Consider regular password rotation for the OIDC user
--
-- Usage:
-- psql -h localhost -p 5432 -d your_obp_database -U your_admin_user -f create_oidc_user_and_views.sql

-- e.g.

-- psql -h localhost -p 5432 -d sandbox -U obp -f ~/Documents/workspace_2024/OBP-API-C/OBP-API/obp-api/src/main/scripts/sql/create_oidc_user_and_views.sql


--psql -d sandbox -f ~/Documents/workspace_2024/OBP-API-C/OBP-API/obp-api/src/main/scripts/sql/create_oidc_user_and_views.sql

-- If any difficulties see the TOP OF THIS FILE for step by step instructions.
-- =============================================================================

-- Database connection parameters (update these to match your OBP configuration)
-- These should match the values in your OBP-API props file (db.url)
\set DB_HOST 'localhost'
\set DB_PORT '5432'
\set DB_NAME 'sandbox'

-- OIDC user credentials
-- ⚠️  SECURITY: Change this to a strong password (20+ chars, mixed case, numbers, symbols)
\set OIDC_USER 'oidc_user'
\set OIDC_PASSWORD 'CHANGE_THIS_TO_A_VERY_STRONG_PASSWORD_2024!'

-- OIDC admin user credentials (for client administration)
-- ⚠️  SECURITY: Change this to a strong password (20+ chars, mixed case, numbers, symbols)
\set OIDC_ADMIN_USER 'oidc_admin'
\set OIDC_ADMIN_PASSWORD 'CHANGE_THIS_TO_A_VERY_STRONG_ADMIN_PASSWORD_2025!'

-- =============================================================================
-- 1. Connect to the OBP database
-- =============================================================================
\echo 'Connecting to OBP database...'
\c :DB_NAME

-- =============================================================================
-- 2. Create OIDC user role
-- =============================================================================
\echo 'Creating OIDC user role...'

-- Drop the users if they already exist (for re-running the script)
DROP USER IF EXISTS :OIDC_USER;
DROP USER IF EXISTS :OIDC_ADMIN_USER;

-- Create the OIDC user with limited privileges
CREATE USER :OIDC_USER WITH
    PASSWORD :'OIDC_PASSWORD'
    NOSUPERUSER
    NOCREATEDB
    NOCREATEROLE
    NOINHERIT
    LOGIN
    NOREPLICATION
    NOBYPASSRLS;

-- Set connection limit for the OIDC user
ALTER USER :OIDC_USER CONNECTION LIMIT 10;

-- Create the OIDC admin user with limited privileges
CREATE USER :OIDC_ADMIN_USER WITH
    PASSWORD :'OIDC_ADMIN_PASSWORD'
    NOSUPERUSER
    NOCREATEDB
    NOCREATEROLE
    NOINHERIT
    LOGIN
    NOREPLICATION
    NOBYPASSRLS;

-- Set connection limit for the OIDC admin user
ALTER USER :OIDC_ADMIN_USER CONNECTION LIMIT 5;

\echo 'OIDC users created successfully.'

-- =============================================================================
-- 3. Create read-only view for authuser table
-- =============================================================================
\echo 'Creating read-only view for OIDC access to authuser...'

-- Drop the view if it already exists
DROP VIEW IF EXISTS v_oidc_users CASCADE;

-- Create a read-only view exposing only necessary authuser fields for OIDC
-- TODO: Consider excluding locked users by joining with mappedbadloginattempt table
--       and checking mbadattemptssinceresetorsuccess against max.bad.login.attempts prop
CREATE VIEW v_oidc_users AS
SELECT
    id,
    username,
    firstname,
    lastname,
    email,
    uniqueid,
    validated,
    provider,
    password_pw,
    password_slt,
    createdat,
    updatedat
FROM authuser
WHERE validated = true  -- Only expose validated users to OIDC service
ORDER BY username;

-- Add comment to the view for documentation
COMMENT ON VIEW v_oidc_users IS 'Read-only view of authuser table for OIDC service access. Only includes validated users and excludes sensitive fields like password hashes. WARNING: Includes password hash and salt for OIDC credential verification - ensure secure access.';

\echo 'OIDC users view created successfully.'

-- =============================================================================
-- 3b. Create read-only view for consumer table (OIDC clients)
-- =============================================================================
\echo 'Creating read-only view for OIDC access to consumers...'

-- Drop the view if it already exists
DROP VIEW IF EXISTS v_oidc_clients CASCADE;

-- Create a read-only view exposing necessary consumer fields for OIDC
-- Note: Some OIDC-specific fields like grant_types and scopes may not exist in current schema
-- TODO: Add grant_types and scopes fields to consumer table if needed for full OIDC compliance
CREATE VIEW v_oidc_clients AS
SELECT
    COALESCE(consumerid, id::varchar) as client_id,  -- Use consumerId if available, otherwise id
    secret as client_secret,
    redirecturl as redirect_uris,
    'authorization_code,refresh_token' as grant_types,  -- Default OIDC grant types
    'openid,profile,email' as scopes,                   -- Default OIDC scopes
    name as client_name,
    'code' as response_types,
    'client_secret_post' as token_endpoint_auth_method,
    createdat as created_at
FROM consumer
WHERE isactive = true  -- Only expose active consumers to OIDC service
ORDER BY client_name;

-- Add comment to the view for documentation
COMMENT ON VIEW v_oidc_clients IS 'Read-only view of consumer table for OIDC service access. Only includes active consumers. Note: grant_types and scopes are hardcoded defaults - consider adding these fields to consumer table for full OIDC compliance.';

\echo 'OIDC clients view created successfully.'

-- =============================================================================
-- 3c. Create read-write view for consumer table administration (OIDC clients admin)
-- =============================================================================
\echo 'Creating admin view for OIDC client management...'

-- Drop the view if it already exists
DROP VIEW IF EXISTS v_oidc_admin_clients CASCADE;

-- Create a view that exposes all consumer fields for full CRUD operations
CREATE VIEW v_oidc_admin_clients AS
SELECT
    id,
    COALESCE(consumerid, id::varchar) as consumer_id,
    key,
    secret,
    azp,
    aud,
    iss,
    sub,
    isactive,
    name,
    apptype,
    description,
    developeremail,
    redirecturl,
    logourl,
    userauthenticationurl,
    createdbyuserid,
    persecondcalllimit,
    perminutecalllimit,
    perhourcalllimit,
    perdaycalllimit,
    perweekcalllimit,
    permonthcalllimit,
    clientcertificate,
    company,
    createdat,
    updatedat
FROM consumer
ORDER BY name;

-- Add comment to the view for documentation
COMMENT ON VIEW v_oidc_admin_clients IS 'Full admin view of consumer table for OIDC service administration. Provides complete CRUD access to all consumer fields for client management operations.';

\echo 'OIDC admin clients view created successfully.'

-- =============================================================================
-- 4. Grant appropriate permissions to OIDC user
-- =============================================================================
\echo 'Granting permissions to OIDC user...'

-- Grant CONNECT privilege on the database
GRANT CONNECT ON DATABASE :DB_NAME TO :OIDC_USER;
GRANT CONNECT ON DATABASE :DB_NAME TO :OIDC_ADMIN_USER;

-- Grant USAGE on the public schema (or specific schema where authuser exists)
GRANT USAGE ON SCHEMA public TO :OIDC_USER;
GRANT USAGE ON SCHEMA public TO :OIDC_ADMIN_USER;

-- Grant SELECT permission on the OIDC views (oidc_user - read-only access)
GRANT SELECT ON v_oidc_users TO :OIDC_USER;
GRANT SELECT ON v_oidc_clients TO :OIDC_USER;

-- Grant full CRUD permissions on the admin view and underlying consumer table (oidc_admin_user only)
GRANT SELECT, INSERT, UPDATE, DELETE ON consumer TO :OIDC_ADMIN_USER;
GRANT SELECT, INSERT, UPDATE, DELETE ON v_oidc_admin_clients TO :OIDC_ADMIN_USER;

-- Explicitly revoke any other permissions to ensure proper access control
REVOKE ALL PRIVILEGES ON ALL TABLES IN SCHEMA public FROM :OIDC_USER;
REVOKE ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public FROM :OIDC_USER;
REVOKE ALL PRIVILEGES ON ALL FUNCTIONS IN SCHEMA public FROM :OIDC_USER;

REVOKE ALL PRIVILEGES ON ALL TABLES IN SCHEMA public FROM :OIDC_ADMIN_USER;
REVOKE ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public FROM :OIDC_ADMIN_USER;
REVOKE ALL PRIVILEGES ON ALL FUNCTIONS IN SCHEMA public FROM :OIDC_ADMIN_USER;

-- Grant permissions on the views again (in case they were revoked above)
-- OIDC_USER: Read-only access to users and active clients
GRANT SELECT ON v_oidc_users TO :OIDC_USER;
GRANT SELECT ON v_oidc_clients TO :OIDC_USER;

-- OIDC_ADMIN_USER: Full CRUD access to client administration
GRANT SELECT, INSERT, UPDATE, DELETE ON consumer TO :OIDC_ADMIN_USER;
GRANT SELECT, INSERT, UPDATE, DELETE ON v_oidc_admin_clients TO :OIDC_ADMIN_USER;

\echo 'Permissions granted successfully.'

-- =============================================================================
-- 5. Create additional security measures
-- =============================================================================
\echo 'Implementing additional security measures...'

-- Set default privileges to prevent future access to new objects
ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON TABLES FROM :OIDC_USER;
ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON SEQUENCES FROM :OIDC_USER;
ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON FUNCTIONS FROM :OIDC_USER;

ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON TABLES FROM :OIDC_ADMIN_USER;
ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON SEQUENCES FROM :OIDC_ADMIN_USER;
ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON FUNCTIONS FROM :OIDC_ADMIN_USER;



\echo 'Security measures implemented successfully.'

-- =============================================================================
-- 6. Verify the setup
-- =============================================================================
\echo 'Verifying OIDC setup...'

-- Check if users exist
SELECT 'OIDC User exists: ' || CASE WHEN EXISTS (
    SELECT 1 FROM pg_user WHERE usename = :'OIDC_USER'
) THEN 'YES' ELSE 'NO' END AS oidc_user_check;

SELECT 'OIDC Admin User exists: ' || CASE WHEN EXISTS (
    SELECT 1 FROM pg_user WHERE usename = :'OIDC_ADMIN_USER'
) THEN 'YES' ELSE 'NO' END AS oidc_admin_user_check;

-- Check if views exist and have data
SELECT 'Users view exists: ' || CASE WHEN EXISTS (
    SELECT 1 FROM information_schema.views
    WHERE table_name = 'v_oidc_users' AND table_schema = 'public'
) THEN 'YES' ELSE 'NO' END AS users_view_check;

SELECT 'Clients view exists: ' || CASE WHEN EXISTS (
    SELECT 1 FROM information_schema.views
    WHERE table_name = 'v_oidc_clients' AND table_schema = 'public'
) THEN 'YES' ELSE 'NO' END AS clients_view_check;

SELECT 'Admin clients view exists: ' || CASE WHEN EXISTS (
    SELECT 1 FROM information_schema.views
    WHERE table_name = 'v_oidc_admin_clients' AND table_schema = 'public'
) THEN 'YES' ELSE 'NO' END AS admin_clients_view_check;

-- Show row counts in the views (if accessible)
SELECT 'Validated users count: ' || COUNT(*) AS user_count
FROM v_oidc_users;

SELECT 'Active clients count: ' || COUNT(*) AS client_count
FROM v_oidc_clients;

SELECT 'Total clients count (admin view): ' || COUNT(*) AS total_client_count
FROM v_oidc_admin_clients;

-- Display the permissions granted to OIDC users
SELECT 'OIDC_USER permissions:' AS permission_info;
SELECT
    table_schema,
    table_name,
    privilege_type,
    is_grantable
FROM information_schema.role_table_grants
WHERE grantee = :'OIDC_USER'
ORDER BY table_schema, table_name;

SELECT 'OIDC_ADMIN_USER permissions:' AS permission_info;
SELECT
    table_schema,
    table_name,
    privilege_type,
    is_grantable
FROM information_schema.role_table_grants
WHERE grantee = :'OIDC_ADMIN_USER'
ORDER BY table_schema, table_name;

-- =============================================================================
-- 7. Display connection information
-- =============================================================================
\echo ''
\echo '====================================================================='
\echo 'OIDC User Setup Complete!'
\echo '====================================================================='
\echo ''
\echo 'Connection details for your OIDC service:'
\echo 'Database Host: ' :DB_HOST
\echo 'Database Port: ' :DB_PORT
\echo 'Database Name: ' :DB_NAME
\echo ''
\echo 'OIDC User (read-only):'
\echo 'Username: ' :OIDC_USER
\echo 'Password: [REDACTED - check script variables]'
\echo 'Available views: v_oidc_users, v_oidc_clients'
\echo 'Permissions: SELECT only (read-only access)'
\echo ''
\echo 'OIDC Admin User (full CRUD for client management):'
\echo 'Username: ' :OIDC_ADMIN_USER
\echo 'Password: [REDACTED - check script variables]'
\echo 'Available views: v_oidc_admin_clients'
\echo 'Permissions: SELECT, INSERT, UPDATE, DELETE (full CRUD access)'
\echo ''
\echo 'Test connection commands:'
\echo '# OIDC User (read-only):'
\echo 'psql -h ' :DB_HOST ' -p ' :DB_PORT ' -d ' :DB_NAME ' -U ' :OIDC_USER ' -c "SELECT COUNT(*) FROM v_oidc_users;"'
\echo 'psql -h ' :DB_HOST ' -p ' :DB_PORT ' -d ' :DB_NAME ' -U ' :OIDC_USER ' -c "SELECT COUNT(*) FROM v_oidc_clients;"'
\echo '# OIDC Admin User (full CRUD):'
\echo 'psql -h ' :DB_HOST ' -p ' :DB_PORT ' -d ' :DB_NAME ' -U ' :OIDC_ADMIN_USER ' -c "SELECT COUNT(*) FROM v_oidc_admin_clients;"'
\echo ''
\echo '====================================================================='
\echo '⚠️  CRITICAL SECURITY WARNINGS ⚠️'
\echo '====================================================================='
\echo 'This view exposes PASSWORD HASHES AND SALTS - implement these measures:'
\echo ''
\echo '1. DATABASE CONNECTION SECURITY:'
\echo '   - Configure SSL/TLS encryption in postgresql.conf'
\echo '   - Add "sslmode=require" to OIDC service connection string'
\echo '   - Use certificate-based authentication if possible'
\echo ''
\echo '2. ACCESS CONTROL:'
\echo '   - Restrict access by IP in pg_hba.conf:'
\echo '     "hostssl dbname oidc_user your.oidc.server.ip/32 md5"'
\echo '   - Use firewall rules to limit database port (5432) access'
\echo ''
\echo '3. MONITORING & AUDITING:'
\echo '   - Enable PostgreSQL query logging'
\echo '   - Monitor failed login attempts'
\echo '   - Set up alerts for unusual access patterns'
\echo '   - Regularly review access logs'
\echo ''
\echo '4. PASSWORD SECURITY:'
\echo '   - Use a strong password for oidc_user (min 20 chars, mixed case, symbols)'
\echo '   - Rotate the password regularly (e.g., quarterly)'
\echo '   - Store password securely (vault/secrets manager)'
\echo ''
\echo '5. ADDITIONAL RECOMMENDATIONS:'
\echo '   - Consider using connection pooling with authentication'
\echo '   - Implement rate limiting on the OIDC service side'
\echo '   - Use read-only database replicas if possible'
\echo '   - Regular security audits of database access'
\echo ''
\echo 'BASIC INFO:'
\echo '- OIDC_USER: Read-only access to validated authuser records and active clients'
\echo '- OIDC_ADMIN_USER: Full CRUD access to all client records for administration'
\echo '- OIDC_USER connection limit: 10 concurrent connections'
\echo '- OIDC_ADMIN_USER connection limit: 5 concurrent connections'
\echo '- Client view uses hardcoded grant_types and scopes (consider adding to schema)'

\echo ''
\echo '====================================================================='

-- =============================================================================
-- End of script
-- =============================================================================
