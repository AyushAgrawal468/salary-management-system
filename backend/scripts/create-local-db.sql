-- Local development setup. Run once as the postgres superuser:
--   psql -U postgres -f backend/scripts/create-local-db.sql
-- Dev-only credentials; real environments set DB_URL / DB_USERNAME / DB_PASSWORD.
CREATE ROLE salary_app LOGIN PASSWORD 'salary_app';
CREATE DATABASE salary OWNER salary_app;
CREATE DATABASE salary_test OWNER salary_app;
