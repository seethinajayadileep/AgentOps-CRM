-- Application accounts for the public login / signup experience.
-- Password hashes only (BCrypt). Never store plaintext passwords.

CREATE TABLE app_users (
    id UUID PRIMARY KEY,
    full_name VARCHAR(200) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
