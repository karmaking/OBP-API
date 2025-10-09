# Prerequisites

For those of us that don't use postgres every day:

1. You will need to have access to a postgres user that can create roles and views etc.
2. You will probably want that postgres user to have easy access to your file system so you can run this script and tweak it if need be.

That means.

1. You probably want to have a postgres user with the same name as your linux or mac username.

So:

```bash
sudo -u postgres psql
```

```sql
CREATE ROLE <YOURLINUXUSERNAME> WITH LOGIN SUPERUSER CREATEDB CREATEROLE;
```

This step is not required but

```sql
CREATE DATABASE <YOURLINUXUSERNAME> OWNER <YOURLINUXUSERNAME>;
```

now quit with `\q`

now when you:

```bash
psql
```

you will be logged in and have access to your normal home directory.

now connect to the OBP database you want e.g.:

```sql
\c sandbox
```

now run the script from within the psql shell:

```sql
\i ~/Documents/workspace_2024/OBP-API-C/OBP-API/obp-api/src/main/scripts/sql/create_oidc_user_and_views.sql
```

or run it from the linux terminal specifying the database

```bash
psql -d sandbox -f ~/Documents/workspace_2024/OBP-API-C/OBP-API/obp-api/src/main/scripts/sql/create_oidc_user_and_views.sql
```

either way, check the output of the script carefully.
