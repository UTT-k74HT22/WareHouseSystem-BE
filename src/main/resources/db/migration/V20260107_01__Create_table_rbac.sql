-- Migration script to create the 'accounts' table
CREATE TABLE accounts
(
    id         CHAR(36) PRIMARY KEY,
    username   VARCHAR(50)  NOT NULL UNIQUE,
    password   VARCHAR(100) NOT NULL,
    status     ENUM('ACTIVE', 'INACTIVE', 'SUSPENDED', 'DELETED') DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50)
);

-- Migration script to create the 'roles' table
CREATE TABLE roles
(
    id          CHAR(36) PRIMARY KEY,
    code        VARCHAR(50)  NOT NULL UNIQUE,
    name        ENUM('ADMIN', 'USER') NOT NULL UNIQUE,
    description TEXT,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by  VARCHAR(50),
    updated_by  VARCHAR(50)
);

-- Migration script to create the 'account_roles' junction table
CREATE TABLE account_roles
(
    account_id CHAR(36),
    role_id    CHAR(36),
    PRIMARY KEY (account_id, role_id),
    FOREIGN KEY (account_id) REFERENCES accounts (id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE
);

-- Migration script to create the 'permissions' table
CREATE TABLE permissions
(
    id          CHAR(36) PRIMARY KEY,
    code        VARCHAR(50)  NOT NULL UNIQUE,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by  VARCHAR(50),
    updated_by  VARCHAR(50)
);

-- Migration script to create the 'role_permissions' junction table
CREATE TABLE role_permissions
(
    role_id       CHAR(36),
    permission_id CHAR(36),
    PRIMARY KEY (role_id, permission_id),
    FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES permissions (id) ON DELETE CASCADE
);

-- Migration script to create the 'user_profiles' table
CREATE TABLE user_profiles
(
    id            CHAR(36) PRIMARY KEY,
    account_id    CHAR(36) UNIQUE,
    first_name    VARCHAR(50),
    last_name     VARCHAR(50),
    email         VARCHAR(100) NOT NULL UNIQUE,
    phone_number  VARCHAR(15),
    address       VARCHAR(255),
    date_of_birth DATE,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by    VARCHAR(50),
    updated_by    VARCHAR(50),
    FOREIGN KEY (account_id) REFERENCES accounts (id) ON DELETE CASCADE
);
