-- =============================================================================
-- GIVE READ ACCESS TO OBP USERS
-- =============================================================================
-- This orchestration script grants OIDC_USER read access to user-related views
-- by including the necessary component scripts

-- Include variable definitions and database connection
\i set_and_connect.sql

-- Create the OIDC user if it doesn't exist
\i cre_OIDC_USER.sql

-- Create the v_oidc_users view (which includes GRANT SELECT to OIDC_USER)
\i cre_v_oidc_users.sql

\echo 'Bye from give_read_access_to_obp_users.sql'
