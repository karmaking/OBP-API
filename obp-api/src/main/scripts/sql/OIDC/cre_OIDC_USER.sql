-- =============================================================================
-- CREATE OIDC_USER
-- =============================================================================
-- This script creates the OIDC user with limited privileges for read-only access

-- Drop the user if they already exist (for re-running the script)
DROP USER IF EXISTS :OIDC_USER;

-- Create the OIDC user with limited privileges
CREATE USER :OIDC_USER WITH
    PASSWORD :OIDC_PASSWORD
    NOSUPERUSER
    NOCREATEDB
    NOCREATEROLE
    NOINHERIT
    LOGIN
    NOREPLICATION
    NOBYPASSRLS;

-- Grant CONNECT privilege on the database
GRANT CONNECT ON DATABASE :DB_NAME TO :OIDC_USER;

-- Grant USAGE on the public schema (or specific schema where authuser exists)
GRANT USAGE ON SCHEMA public TO :OIDC_USER;

\echo 'OIDC user created successfully.'
