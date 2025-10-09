-- =============================================================================
-- GIVE READ ACCESS TO OBP USERS AND WRITE ACCESS TO OBP CONSUMERS
-- =============================================================================
-- This orchestration script grants OIDC_ADMIN_USER read access to user-related views
-- and full CRUD access to consumer/client management
-- by including the necessary component scripts

-- Include variable definitions and database connection
\i set_and_connect.sql

-- Create the OIDC users
-- TODO check if we need both here.
\i cre_OIDC_USER.sql
\i cre_OIDC_ADMIN_USER.sql

-- Create all three views (which include the necessary GRANT statements)
\i cre_v_oidc_users.sql
\i cre_v_oidc_clients.sql
\i cre_v_oidc_admin_clients.sql

\echo 'Bye from give_read_access_to_obp_users_and_write_access_to_obp_consumers.sql'
