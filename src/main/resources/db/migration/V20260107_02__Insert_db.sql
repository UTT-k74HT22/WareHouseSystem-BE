INSERT INTO roles (id, code, name, description)
VALUES (
           UUID(),
           'ROLE_ADMIN',
           'ADMIN',
           'Administrator role'
       )
    ON DUPLICATE KEY UPDATE name = name;

INSERT INTO accounts (
    id,
    username,
    password,
    status
)
VALUES (
           UUID(),
           'admin',
           '$2a$10$2FrGg2/7Rtr7lQWRZ9UNV..WQblwoUUgJgOxWYnhP.okeEd3Jo5si',
           'ACTIVE'
       );

INSERT INTO account_roles (account_id, role_id)
SELECT a.id, r.id
FROM accounts a
         JOIN roles r ON r.name = 'ADMIN'
WHERE a.username = 'admin';


INSERT INTO user_profiles (
    id,
    account_id,
    first_name,
    last_name,
    email
)
SELECT
    UUID(),
    a.id,
    'System',
    'Administrator',
    'admin@whs.local'
FROM accounts a
WHERE a.username = 'admin';
