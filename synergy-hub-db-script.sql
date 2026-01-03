DROP DATABASE IF EXISTS synergy_hub;
CREATE DATABASE synergy_hub;
USE synergy_hub;

-- Table: organizations
CREATE TABLE organizations (
    org_id      INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    address     VARCHAR(255),
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- Table: users (each user belongs to an organization)
CREATE TABLE users (
    user_id             INT AUTO_INCREMENT PRIMARY KEY,
    organization_id     INT NOT NULL,
    name                VARCHAR(100) NOT NULL,
    email               VARCHAR(100) NOT NULL UNIQUE,
    password_hash       VARCHAR(255) NOT NULL,
    two_factor_enabled  BOOLEAN NOT NULL DEFAULT FALSE,
    account_locked      BOOLEAN NOT NULL DEFAULT FALSE,
    lock_until          DATETIME,
    email_verified      BOOLEAN NOT NULL DEFAULT FALSE,
    last_login          DATETIME,
    failed_login_attempts INT NOT NULL DEFAULT 0,
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_email (email),
    INDEX idx_organization (organization_id),
    CONSTRAINT fk_user_org FOREIGN KEY (organization_id)
        REFERENCES organizations(org_id)
        ON DELETE CASCADE
) ENGINE=InnoDB;

-- Insert sample organizations
INSERT INTO organizations (org_id, name, address) VALUES
(1, 'TechNova Corp', '1234 Elm Street, Metropolis'),
(2, 'HealthPlus Inc', '9876 Oak Ave, Gotham'),
(3, 'EduFuture Labs', '500 Academy Lane, Star City');

-- Insert sample users (with organization linkage)
INSERT INTO users (user_id, organization_id, name, email, password_hash, two_factor_enabled, email_verified) VALUES
(1, 1, 'Alice Johnson', 'alice@technova.com', '$2a$10$dummyhash1', FALSE, TRUE),
(2, 1, 'Bob Smith', 'bob@technova.com', '$2a$10$dummyhash2', TRUE, TRUE),
(3, 2, 'Carol White', 'carol@healthplus.com', '$2a$10$dummyhash3', FALSE, TRUE),
(4, 2, 'David Young', 'david@healthplus.com', '$2a$10$dummyhash4', FALSE, TRUE),
(5, 3, 'Eve Jackson', 'eve@edufuture.com', '$2a$10$dummyhash5', FALSE, FALSE);

-- Table: roles (predefined roles like Admin, Manager, Member, Guest)
CREATE TABLE roles (
    role_id   INT AUTO_INCREMENT PRIMARY KEY,
    name      VARCHAR(50) NOT NULL UNIQUE,
    description TEXT
) ENGINE=InnoDB;

-- Table: permissions (individual actionable permissions)
CREATE TABLE permissions (
    perm_id   INT AUTO_INCREMENT PRIMARY KEY,
    name      VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    INDEX idx_permission_name (name)
) ENGINE=InnoDB;

-- Table: role_permissions (linking roles to permissions, many-to-many)
CREATE TABLE role_permissions (
    role_id INT NOT NULL,
    perm_id INT NOT NULL,
    PRIMARY KEY (role_id, perm_id),
    FOREIGN KEY (role_id) REFERENCES roles(role_id) ON DELETE CASCADE,
    FOREIGN KEY (perm_id) REFERENCES permissions(perm_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Table: user_roles (assigning roles to users, many-to-many)
CREATE TABLE user_roles (
    user_id INT NOT NULL,
    role_id INT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(role_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Insert sample roles
INSERT INTO roles (role_id, name, description) VALUES
(1, 'Organization Admin', 'Admin of the organization with full access'),
(2, 'Project Manager', 'Manages projects and sprints'),
(3, 'Team Member', 'Regular team member with task access'),
(4, 'Guest', 'External or view-only collaborator');

-- Insert sample permissions
INSERT INTO permissions (perm_id, name, description) VALUES
(1, 'MANAGE_ORG_SETTINGS', 'Manage organization settings and users'),
(2, 'CREATE_PROJECT', 'Create new projects'),
(3, 'EDIT_TASK', 'Edit and update tasks'),
(4, 'VIEW_PROJECT', 'View project and tasks'),
(5, 'MANAGE_SPRINTS', 'Create and manage sprints');

-- Map roles to permissions (role_permissions)
-- e.g., Admin has all permissions, Project Manager can create projects, manage sprints, etc.
INSERT INTO role_permissions (role_id, perm_id) VALUES
-- Admin (role 1) gets perm 1,2,3,4,5
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5),
-- Project Manager (role 2) gets perm 2,3,4,5 (create project, edit tasks, view projects, manage sprints)
(2, 2), (2, 3), (2, 4), (2, 5),
-- Team Member (role 3) gets perm 3,4 (edit tasks, view projects)
(3, 3), (3, 4),
-- Guest (role 4) gets perm 4 (view projects only)
(4, 4);

-- Assign roles to users (user_roles)
-- Alice (user 1) as Org Admin, Bob (2) as Project Manager, Carol (3) as Team Member, David (4) as Team Member, Eve (5) as Guest.
INSERT INTO user_roles (user_id, role_id) VALUES
(1, 1),  -- Alice is Org Admin
(2, 2),  -- Bob is Project Manager
(3, 3),  -- Carol is Team Member
(4, 3),  -- David is Team Member
(5, 4);  -- Eve is Guest

-- Table: projects
CREATE TABLE projects (
    project_id      INT AUTO_INCREMENT PRIMARY KEY,
    organization_id INT NOT NULL,
    name            VARCHAR(100) NOT NULL,
    description     TEXT,
    project_lead_id INT,
    start_date      DATE,
    end_date        DATE,
    status          ENUM('PLANNING','ACTIVE','COMPLETED','ON_HOLD','ARCHIVED') NOT NULL DEFAULT 'ACTIVE',
    FOREIGN KEY (organization_id) REFERENCES organizations(org_id) ON DELETE CASCADE,
    FOREIGN KEY (project_lead_id) REFERENCES users(user_id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- Table: project_members (which users are part of a project team)
CREATE TABLE project_members (
    project_id INT NOT NULL,
    user_id    INT NOT NULL,
    role       ENUM('PROJECT_LEAD','DEVELOPER','TESTER','DESIGNER','SCRUM_MASTER','PRODUCT_OWNER','BUSINESS_ANALYST','STAKEHOLDER') NOT NULL,
    PRIMARY KEY (project_id, user_id),
    FOREIGN KEY (project_id) REFERENCES projects(project_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Insert sample projects
INSERT INTO projects (project_id, organization_id, name, description, project_lead_id, start_date, end_date) VALUES
(1, 1, 'Project Alpha', 'New software development project', 2, '2025-09-01', '2025-12-31'),
(2, 1, 'Project Beta', 'Internal infrastructure upgrade', 1, '2025-08-15', '2025-11-30'),
(3, 2, 'Health App', 'Mobile app for health tracking', 3, '2025-07-01', '2025-10-30'),
(4, 3, 'E-Learning Platform', 'Online learning portal project', 5, '2025-01-01', '2025-06-30');

-- Insert sample project membership (project_members)
INSERT INTO project_members (project_id, user_id, role) VALUES
(1, 1, 'PROJECT_LEAD'),
(1, 2, 'DEVELOPER'),
(2, 1, 'PROJECT_LEAD'),
(2, 2, 'DEVELOPER'),
(3, 3, 'PROJECT_LEAD'),
(3, 4, 'DEVELOPER'),
(4, 5, 'STAKEHOLDER');

-- Table: sprints (each sprint belongs to a project)
CREATE TABLE sprints (
    sprint_id    INT AUTO_INCREMENT PRIMARY KEY,
    project_id   INT NOT NULL,
    name         VARCHAR(100) NOT NULL,
    goal         TEXT,
    start_date   DATE NOT NULL,
    end_date     DATE NOT NULL,
    status       ENUM('PLANNING','ACTIVE','COMPLETED','CANCELLED') NOT NULL DEFAULT 'PLANNING',
    FOREIGN KEY (project_id) REFERENCES projects(project_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Table: tasks (product backlog items, issues, or sprint tasks)
CREATE TABLE tasks (
    task_id         INT AUTO_INCREMENT PRIMARY KEY,
    project_id      INT NOT NULL,
    sprint_id       INT,
    title           VARCHAR(200) NOT NULL,
    description     TEXT,
    type            ENUM('STORY','BUG','CHORE','TASK') NOT NULL DEFAULT 'TASK',
    status          ENUM('TO_DO','IN_PROGRESS','IN_REVIEW','DONE','BLOCKED','CANCELLED','BACKLOG') NOT NULL DEFAULT 'TO_DO',
    priority        ENUM('LOW','MEDIUM','HIGH','CRITICAL') NOT NULL DEFAULT 'MEDIUM',
    story_points    INT,
    estimated_hours DECIMAL(10,2),
    actual_hours    DECIMAL(10,2),
    due_date        DATETIME(6),
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    assignee_id     INT,
    reporter_id     INT NOT NULL,
    parent_task_id  INT,
    archived        BOOLEAN NOT NULL DEFAULT FALSE,
    INDEX idx_task_sprint (sprint_id),
    INDEX idx_task_assignee (assignee_id),
    INDEX idx_task_project (project_id),
    INDEX idx_task_status (status),
    INDEX idx_task_parent (parent_task_id),
    INDEX idx_task_reporter (reporter_id),
    FOREIGN KEY (project_id) REFERENCES projects(project_id) ON DELETE CASCADE,
    FOREIGN KEY (sprint_id)  REFERENCES sprints(sprint_id) ON DELETE SET NULL,
    FOREIGN KEY (assignee_id) REFERENCES users(user_id) ON DELETE SET NULL,
    FOREIGN KEY (reporter_id) REFERENCES users(user_id) ON DELETE RESTRICT,
    FOREIGN KEY (parent_task_id) REFERENCES tasks(task_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Table: task_dependencies (many-to-many self relationship for task blocking)
CREATE TABLE task_dependencies (
    task_id         INT NOT NULL,
    depends_on_task INT NOT NULL,
    PRIMARY KEY (task_id, depends_on_task),
    FOREIGN KEY (task_id) REFERENCES tasks(task_id) ON DELETE CASCADE,
    FOREIGN KEY (depends_on_task) REFERENCES tasks(task_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Insert a sample sprint for Project Alpha
INSERT INTO sprints (sprint_id, project_id, name, goal, start_date, end_date, status) VALUES
(1, 1, 'Sprint 1', 'Implement core features', '2025-10-01', '2025-10-15', 'ACTIVE');

-- Insert sample tasks
INSERT INTO tasks (task_id, project_id, sprint_id, title, description, type, status, priority, story_points, due_date, assignee_id, reporter_id, parent_task_id) VALUES
(1, 1, 1, 'Setup project repository', 'Initialize git repository and CI pipeline', 'TASK', 'DONE', 'MEDIUM', 1, '2025-10-05', 2, 2, NULL),
(2, 1, 1, 'Implement authentication module', 'Develop login, registration APIs with JWT', 'STORY', 'IN_PROGRESS', 'HIGH', 8, '2025-10-10', 1, 2, NULL),
(3, 1, 1, 'Login page UI', 'Create frontend login page (sub-task of auth module)', 'TASK', 'TO_DO', 'MEDIUM', 3, '2025-10-10', 1, 2, 2),
(4, 1, NULL, 'Research OAuth integration', 'Investigate adding Google OAuth2 login', 'TASK', 'TO_DO', 'LOW', 2, '2025-10-20', 2, 1, NULL),
(5, 2, NULL, 'Upgrade server hardware', 'Install new servers for production environment', 'STORY', 'TO_DO', 'HIGH', 5, '2025-11-01', 1, 1, NULL);

-- Insert task dependencies (e.g., Task 2 depends on Task 1)
INSERT INTO task_dependencies (task_id, depends_on_task) VALUES
(2, 1);

-- Table: user_sessions (JWT session tracking)
CREATE TABLE user_sessions (
    session_id          INT AUTO_INCREMENT PRIMARY KEY,
    user_id             INT NOT NULL,
    token_id            VARCHAR(255) NOT NULL UNIQUE,
    user_agent          VARCHAR(500),
    ip_address          VARCHAR(45),
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at          DATETIME NOT NULL,
    last_accessed_at    DATETIME NOT NULL,
    revoked             BOOLEAN NOT NULL DEFAULT FALSE,
    INDEX idx_token (token_id),
    INDEX idx_user_active (user_id, revoked, expires_at),
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Table: two_factor_secrets (encrypted TOTP secrets)
CREATE TABLE two_factor_secrets (
    secret_id   INT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT NOT NULL UNIQUE,
    secret      VARCHAR(512) NOT NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_2fa_user (user_id),
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Table: backup_codes (2FA backup codes)
CREATE TABLE backup_codes (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT NOT NULL,
    code        VARCHAR(255) NOT NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    used        BOOLEAN NOT NULL DEFAULT FALSE,
    INDEX idx_backup_user_used (user_id, used),
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Table: password_reset_tokens
CREATE TABLE password_reset_tokens (
    token_id    INT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT NOT NULL,
    token       VARCHAR(255) NOT NULL UNIQUE,
    expiry_time DATETIME NOT NULL,
    used        BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_token (token),
    INDEX idx_expiry (expiry_time),
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Table: email_verifications
CREATE TABLE email_verifications (
    verification_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id         INT NOT NULL,
    token           VARCHAR(255) NOT NULL UNIQUE,
    verified        BOOLEAN NOT NULL DEFAULT FALSE,
    expiry_time     DATETIME NOT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_token (token),
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Table: login_attempts (track failed login attempts)
CREATE TABLE login_attempts (
    attempt_id  INT AUTO_INCREMENT PRIMARY KEY,
    email       VARCHAR(100) NOT NULL,
    ip_address  VARCHAR(45),
    success     BOOLEAN NOT NULL,
    attempted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_email_time (email, attempted_at),
    INDEX idx_ip_time (ip_address, attempted_at)
) ENGINE=InnoDB;

-- Table: audit_logs (system audit trail)
CREATE TABLE audit_logs (
    audit_log_id    INT AUTO_INCREMENT PRIMARY KEY,
    user_id         INT,
    event_type      VARCHAR(100) NOT NULL,
    event_details   TEXT,
    ip_address      VARCHAR(45),
    user_agent      VARCHAR(500),
    project_id      INT,
    timestamp       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_audit_user (user_id),
    INDEX idx_audit_timestamp (timestamp),
    INDEX idx_audit_event_type (event_type),
    INDEX idx_audit_project (project_id),
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- Table: sso_providers (Single Sign-On providers)
CREATE TABLE sso_providers (
    provider_id     INT AUTO_INCREMENT PRIMARY KEY,
    org_id          INT NOT NULL,
    provider_type   ENUM('SAML','OIDC','OAUTH2') NOT NULL,
    provider_name   VARCHAR(100) NOT NULL,
    client_id       VARCHAR(255) NOT NULL,
    client_secret   VARCHAR(512) NOT NULL,
    metadata_url    VARCHAR(500),
    enabled         BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (org_id) REFERENCES organizations(org_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Table: sso_user_mappings (Maps SSO external IDs to internal users)
CREATE TABLE sso_user_mappings (
    mapping_id      INT AUTO_INCREMENT PRIMARY KEY,
    user_id         INT NOT NULL,
    provider_id     INT NOT NULL,
    external_id     VARCHAR(255) NOT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_provider_external (provider_id, external_id),
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (provider_id) REFERENCES sso_providers(provider_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Table: comments (task comments)
CREATE TABLE comments (
    comment_id   INT AUTO_INCREMENT PRIMARY KEY,
    task_id      INT NOT NULL,
    user_id      INT NOT NULL,
    content      TEXT NOT NULL,
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (task_id) REFERENCES tasks(task_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Insert sample comments on tasks
INSERT INTO comments (comment_id, task_id, user_id, content, created_at) VALUES
(1, 2, 1, 'Reminder: Consider password reset feature in auth module.', '2025-10-01 09:00:00'),
(2, 2, 2, 'Good point, we will add that in a future sprint.', '2025-10-01 09:30:00'),
(3, 4, 2, 'Any update on OAuth research? Need info by next week.', '2025-10-08 15:45:00');

-- ============================================================
-- OPTIONAL TABLES (Not yet implemented in backend entities)
-- Uncomment these when ready to implement resource booking/chat
-- ============================================================

-- Table: resources
CREATE TABLE resources (
    resource_id     INT AUTO_INCREMENT PRIMARY KEY,
    organization_id INT NOT NULL,
    name            VARCHAR(100) NOT NULL,
    resource_type   ENUM('ROOM','EQUIPMENT','LICENSE','OTHER') NOT NULL DEFAULT 'OTHER',
    details         TEXT,
    FOREIGN KEY (organization_id) REFERENCES organizations(org_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Table: bookings (resource reservations)
CREATE TABLE bookings (
    booking_id   INT AUTO_INCREMENT PRIMARY KEY,
    resource_id  INT NOT NULL,
    user_id      INT NOT NULL,
    start_time   DATETIME NOT NULL,
    end_time     DATETIME NOT NULL,
    purpose      VARCHAR(255),
    FOREIGN KEY (resource_id) REFERENCES resources(resource_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id)     REFERENCES users(user_id)      ON DELETE RESTRICT
) ENGINE=InnoDB;

-- Table: events (calendar events/meetings)
CREATE TABLE events (
    event_id        INT AUTO_INCREMENT PRIMARY KEY,
    organization_id INT NOT NULL,
    project_id      INT,
    title           VARCHAR(200) NOT NULL,
    description     TEXT,
    start_time      DATETIME NOT NULL,
    end_time        DATETIME NOT NULL,
    created_by      INT NOT NULL,
    FOREIGN KEY (organization_id) REFERENCES organizations(org_id) ON DELETE CASCADE,
    FOREIGN KEY (project_id) REFERENCES projects(project_id) ON DELETE SET NULL,
    FOREIGN KEY (created_by) REFERENCES users(user_id) ON DELETE RESTRICT
) ENGINE=InnoDB;

-- Table: chat_channels
CREATE TABLE chat_channels (
    channel_id   INT AUTO_INCREMENT PRIMARY KEY,
    organization_id INT NOT NULL,
    project_id   INT,
    task_id      INT,
    name         VARCHAR(100),
    channel_type ENUM('TEAM','PROJECT','TASK','DIRECT') NOT NULL,
    FOREIGN KEY (organization_id) REFERENCES organizations(org_id) ON DELETE CASCADE,
    FOREIGN KEY (project_id) REFERENCES projects(project_id) ON DELETE CASCADE,
    FOREIGN KEY (task_id) REFERENCES tasks(task_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Table: channel_members
CREATE TABLE channel_members (
    channel_id INT NOT NULL,
    user_id    INT NOT NULL,
    PRIMARY KEY(channel_id, user_id),
    FOREIGN KEY (channel_id) REFERENCES chat_channels(channel_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Table: chat_messages
CREATE TABLE chat_messages (
    message_id      INT AUTO_INCREMENT PRIMARY KEY,
    channel_id      INT NOT NULL,
    user_id         INT NOT NULL,
    content         TEXT NOT NULL,
    sent_at         DATETIME DEFAULT CURRENT_TIMESTAMP,
    parent_message_id INT,
    FOREIGN KEY (channel_id) REFERENCES chat_channels(channel_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (parent_message_id) REFERENCES chat_messages(message_id) ON DELETE CASCADE
) ENGINE=InnoDB;