-- =============================================================================
-- SET VARIABLES
-- =============================================================================
-- This script defines all variables used in the OIDC setup scripts
-- Update these values to match your environment and security requirements

-- Database connection parameters (update these to match your OBP configuration)
-- These should match the values in your OBP-API props file (db.url)
\set DB_HOST 'localhost'
\set DB_PORT '5432'
\set DB_NAME 'sandbox'

-- OIDC user credentials
-- ⚠️  SECURITY: Change this to a strong password (20+ chars, mixed case, numbers, symbols)
\set OIDC_USER "oidc_user"
\set OIDC_PASSWORD '''lakij8777fagg'''

-- OIDC admin user credentials (for client administration)
-- ⚠️  SECURITY: Change this to a strong password (20+ chars, mixed case, numbers, symbols)
\set OIDC_ADMIN_USER "oidc_admin"
\set OIDC_ADMIN_PASSWORD '''fhka77uefassEE'''

-- =============================================================================
-- Connect to the OBP database
-- =============================================================================
\echo 'Connecting to OBP database...'
\c :DB_NAME
