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

-- psql -h localhost -p 5432 -d sandbox -U obp -f OBP-API/obp-api/src/main/scripts/sql/create_oidc_user_and_views.sql


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

-- =============================================================================
-- 1. Connect to the OBP database
-- =============================================================================
\echo 'Connecting to OBP database...'
\c :DB_NAME

-- =============================================================================
-- 2. Create OIDC user role
-- =============================================================================
\echo 'Creating OIDC user role...'

-- Drop the user if it already exists (for re-running the script)
DROP USER IF EXISTS :OIDC_USER;

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

\echo 'OIDC user created successfully.'

-- =============================================================================
-- 3. Create read-only view for authuser table
-- =============================================================================
\echo 'Creating read-only view for OIDC access to authuser...'

-- Drop the view if it already exists
DROP VIEW IF EXISTS v_authuser_oidc CASCADE;

-- Create a read-only view exposing only necessary authuser fields for OIDC
-- TODO: Consider excluding locked users by joining with mappedbadloginattempt table
--       and checking mbadattemptssinceresetorsuccess against max.bad.login.attempts prop
CREATE VIEW v_authuser_oidc AS
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
COMMENT ON VIEW v_authuser_oidc IS 'Read-only view of authuser table for OIDC service access. Only includes validated users. WARNING: Includes password hash and salt for OIDC credential verification - ensure secure access.';

\echo 'OIDC authuser view created successfully.'

-- =============================================================================
-- 4. Grant appropriate permissions to OIDC user
-- =============================================================================
\echo 'Granting permissions to OIDC user...'

-- Grant CONNECT privilege on the database
GRANT CONNECT ON DATABASE :DB_NAME TO :OIDC_USER;

-- Grant USAGE on the public schema (or specific schema where authuser exists)
GRANT USAGE ON SCHEMA public TO :OIDC_USER;

-- Grant SELECT permission on the OIDC view only
GRANT SELECT ON v_authuser_oidc TO :OIDC_USER;

-- Explicitly revoke any other permissions to ensure read-only access
REVOKE ALL PRIVILEGES ON ALL TABLES IN SCHEMA public FROM :OIDC_USER;
REVOKE ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public FROM :OIDC_USER;
REVOKE ALL PRIVILEGES ON ALL FUNCTIONS IN SCHEMA public FROM :OIDC_USER;

-- Grant SELECT on the view again (in case it was revoked above)
GRANT SELECT ON v_authuser_oidc TO :OIDC_USER;

\echo 'Permissions granted successfully.'

-- =============================================================================
-- 5. Create additional security measures
-- =============================================================================
\echo 'Implementing additional security measures...'

-- Set default privileges to prevent future access to new objects
ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON TABLES FROM :OIDC_USER;
ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON SEQUENCES FROM :OIDC_USER;
ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON FUNCTIONS FROM :OIDC_USER;



\echo 'Security measures implemented successfully.'

-- =============================================================================
-- 6. Verify the setup
-- =============================================================================
\echo 'Verifying OIDC setup...'

-- Check if user exists
SELECT 'User exists: ' || CASE WHEN EXISTS (
    SELECT 1 FROM pg_user WHERE usename = :'OIDC_USER'
) THEN 'YES' ELSE 'NO' END AS user_check;

-- Check if view exists and has data
SELECT 'View exists and accessible: ' || CASE WHEN EXISTS (
    SELECT 1 FROM information_schema.views
    WHERE table_name = 'v_authuser_oidc' AND table_schema = 'public'
) THEN 'YES' ELSE 'NO' END AS view_check;

-- Show row count in the view (if accessible)
SELECT 'Validated users count: ' || COUNT(*) AS user_count
FROM v_authuser_oidc;

-- Display the permissions granted to OIDC user
SELECT
    table_schema,
    table_name,
    privilege_type,
    is_grantable
FROM information_schema.role_table_grants
WHERE grantee = :'OIDC_USER'
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
\echo 'Username: ' :OIDC_USER
\echo 'Password: [REDACTED - check script variables]'
\echo ''
\echo 'Available view: v_authuser_oidc'
\echo 'Permissions: SELECT only (read-only access)'
\echo ''
\echo 'Test connection command:'
\echo 'psql -h ' :DB_HOST ' -p ' :DB_PORT ' -d ' :DB_NAME ' -U ' :OIDC_USER ' -c "SELECT COUNT(*) FROM v_authuser_oidc;"'
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
\echo '- The OIDC user has read-only access to validated authuser records only'
\echo '- Connection limit is set to 10 concurrent connections'

\echo ''
\echo '====================================================================='

-- =============================================================================
-- End of script
-- =============================================================================
