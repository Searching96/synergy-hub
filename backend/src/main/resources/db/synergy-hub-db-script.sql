DROP DATABASE IF EXISTS synergy_hub;
CREATE DATABASE synergy_hub;
USE synergy_hub;

-- Table: organizations
CREATE TABLE organizations (
    org_id                  INT AUTO_INCREMENT PRIMARY KEY,
    name                    VARCHAR(100) NOT NULL UNIQUE,
    address                 VARCHAR(255),
    contact_email           VARCHAR(100),
    invite_code             VARCHAR(20) UNIQUE,
    invite_code_expires_at  DATETIME,
    created_at              DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_org_invite_code (invite_code),
    INDEX idx_org_contact_email (contact_email)
) ENGINE=InnoDB;

-- Table: users (each user belongs to an organization)
CREATE TABLE users (
    user_id             INT AUTO_INCREMENT PRIMARY KEY,
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
    INDEX idx_email (email)
) ENGINE=InnoDB;

-- Table: user_organizations (many-to-many user-org relationship)
CREATE TABLE user_organizations (
    user_id INT NOT NULL,
    organization_id INT NOT NULL,
    joined_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    status ENUM('ACTIVE', 'INACTIVE', 'SUSPENDED') NOT NULL DEFAULT 'ACTIVE',
    PRIMARY KEY (user_id, organization_id),
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (organization_id) REFERENCES organizations(org_id) ON DELETE CASCADE,
    INDEX idx_user_orgs (user_id),
    INDEX idx_org_users (organization_id),
    INDEX idx_primary_org (user_id, is_primary)
) ENGINE=InnoDB;

-- Table: join_requests (users requesting to join organizations)
CREATE TABLE join_requests (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    user_id         INT NOT NULL,
    organization_id INT NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    requested_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    approved_by     INT,
    approved_at     DATETIME,
    INDEX idx_join_request_user (user_id),
    INDEX idx_join_request_org (organization_id),
    INDEX idx_join_request_status (status),
    UNIQUE KEY uk_user_org (user_id, organization_id),
    CONSTRAINT fk_join_request_user 
        FOREIGN KEY (user_id) 
        REFERENCES users(user_id) 
        ON DELETE CASCADE,
    CONSTRAINT fk_join_request_org 
        FOREIGN KEY (organization_id) 
        REFERENCES organizations(org_id) 
        ON DELETE CASCADE,
    CONSTRAINT fk_join_request_approver 
        FOREIGN KEY (approved_by) 
        REFERENCES users(user_id) 
        ON DELETE SET NULL
) ENGINE=InnoDB;

-- Insert sample organizations
INSERT INTO organizations (org_id, name, address, contact_email) VALUES
(1, 'TechNova Corp', '1234 Elm Street, Metropolis', 'admin@technova.com'),
(2, 'HealthPlus Inc', '9876 Oak Ave, Gotham', 'admin@healthplus.com'),
(3, 'EduFuture Labs', '500 Academy Lane, Star City', 'admin@edufuture.com');

-- Insert sample users (with organization linkage)
INSERT INTO users (user_id, name, email, password_hash, two_factor_enabled, email_verified) VALUES
(1, 'Alice Johnson', 'alice@technova.com', '$2a$10$dummyhash1', FALSE, TRUE),
(2, 'Bob Smith', 'bob@technova.com', '$2a$10$dummyhash2', TRUE, TRUE),
(3, 'Carol White', 'carol@healthplus.com', '$2a$10$dummyhash3', FALSE, TRUE),
(4, 'David Young', 'david@healthplus.com', '$2a$10$dummyhash4', FALSE, TRUE),
(5, 'Eve Jackson', 'eve@edufuture.com', '$2a$10$dummyhash5', FALSE, FALSE);

-- Table: roles (per-organization roles)
CREATE TABLE roles (
    role_id         INT AUTO_INCREMENT PRIMARY KEY,
    organization_id INT NOT NULL,
    name            VARCHAR(50) NOT NULL,
    description     TEXT,
    UNIQUE KEY uk_role_org_name (organization_id, name),
    CONSTRAINT fk_role_org FOREIGN KEY (organization_id)
        REFERENCES organizations(org_id)
        ON DELETE CASCADE
) ENGINE=InnoDB;

-- Table: permissions (individual actionable permissions)
CREATE TABLE permissions (
    perm_id   INT AUTO_INCREMENT PRIMARY KEY,
    name      VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    INDEX idx_permission_name (name)
) ENGINE=InnoDB;

CREATE TABLE role_permissions (
    role_id INT NOT NULL,
    perm_id INT NOT NULL,
    PRIMARY KEY (role_id, perm_id),
    FOREIGN KEY (role_id) REFERENCES roles(role_id) ON DELETE CASCADE,
    FOREIGN KEY (perm_id) REFERENCES permissions(perm_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE user_roles (
    user_id INT NOT NULL,
    role_id INT NOT NULL,
    organization_id INT NOT NULL,
    PRIMARY KEY (user_id, role_id, organization_id),
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(role_id) ON DELETE CASCADE,
    FOREIGN KEY (organization_id) REFERENCES organizations(org_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Insert sample roles per organization (org_id, name)
INSERT INTO roles (role_id, organization_id, name, description) VALUES
-- Org 1: TechNova Corp
(1, 1, 'ORG_ADMIN', 'Organization Admin with full access to manage roles and settings'),
(2, 1, 'PROJECT_MANAGER', 'Manages projects and sprints'),
(3, 1, 'TEAM_MEMBER', 'Regular team member with task access'),
(4, 1, 'GUEST', 'External or view-only collaborator'),
-- Org 2: HealthPlus Inc
(5, 2, 'ORG_ADMIN', 'Organization Admin with full access to manage roles and settings'),
(6, 2, 'PROJECT_MANAGER', 'Manages projects and sprints'),
(7, 2, 'TEAM_MEMBER', 'Regular team member with task access'),
(8, 2, 'GUEST', 'External or view-only collaborator'),
-- Org 3: EduFuture Labs
(9, 3, 'ORG_ADMIN', 'Organization Admin with full access to manage roles and settings'),
(10, 3, 'PROJECT_MANAGER', 'Manages projects and sprints'),
(11, 3, 'TEAM_MEMBER', 'Regular team member with task access'),
(12, 3, 'GUEST', 'External or view-only collaborator');

-- Insert sample permissions
INSERT INTO permissions (perm_id, name, description) VALUES
(1, 'MANAGE_ORG_SETTINGS', 'Manage organization settings and users'),
(2, 'CREATE_PROJECT', 'Create new projects'),
(3, 'EDIT_TASK', 'Edit and update tasks'),
(4, 'VIEW_PROJECT', 'View project and tasks'),
(5, 'MANAGE_SPRINTS', 'Create and manage sprints');

INSERT INTO role_permissions (role_id, perm_id) VALUES
-- Org 1 roles
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5), -- ORG_ADMIN all perms
(2, 2), (2, 3), (2, 4), (2, 5),         -- PROJECT_MANAGER
(3, 3), (3, 4),                         -- TEAM_MEMBER
(4, 4),                                 -- GUEST
-- Org 2 roles
(5, 1), (5, 2), (5, 3), (5, 4), (5, 5),
(6, 2), (6, 3), (6, 4), (6, 5),
(7, 3), (7, 4),
(8, 4),
-- Org 3 roles
(9, 1), (9, 2), (9, 3), (9, 4), (9, 5),
(10, 2), (10, 3), (10, 4), (10, 5),
(11, 3), (11, 4),
(12, 4);

INSERT INTO user_roles (user_id, role_id, organization_id) VALUES
-- Org 1 users
(1, 1, 1),  -- Alice is ORG_ADMIN in org 1
(2, 2, 1),  -- Bob is PROJECT_MANAGER in org 1
-- Org 2 users
(3, 7, 2),  -- Carol is TEAM_MEMBER in org 2
(4, 7, 2),  -- David is TEAM_MEMBER in org 2
-- Org 3 users
(5, 12, 3); -- Eve is GUEST in org 3

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
    type            ENUM('EPIC','STORY','BUG','TASK','SUBTASK') NOT NULL DEFAULT 'TASK',
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
    epic_id         INT,
    archived        BOOLEAN NOT NULL DEFAULT FALSE,
    INDEX idx_task_sprint (sprint_id),
    INDEX idx_task_assignee (assignee_id),
    INDEX idx_task_project (project_id),
    INDEX idx_task_status (status),
    INDEX idx_task_parent (parent_task_id),
    INDEX idx_task_epic (epic_id),
    INDEX idx_task_reporter (reporter_id),
    FOREIGN KEY (project_id) REFERENCES projects(project_id) ON DELETE CASCADE,
    FOREIGN KEY (sprint_id)  REFERENCES sprints(sprint_id) ON DELETE SET NULL,
    FOREIGN KEY (assignee_id) REFERENCES users(user_id) ON DELETE SET NULL,
    FOREIGN KEY (reporter_id) REFERENCES users(user_id) ON DELETE RESTRICT,
    FOREIGN KEY (parent_task_id) REFERENCES tasks(task_id) ON DELETE CASCADE,
    FOREIGN KEY (epic_id) REFERENCES tasks(task_id) ON DELETE SET NULL
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

-- Table: attachments (file attachments for tasks)
CREATE TABLE attachments (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    task_id         INT NOT NULL,
    file_name       VARCHAR(255) NOT NULL,
    file_size       BIGINT NOT NULL,
    file_type       VARCHAR(100) NOT NULL,
    file_key        VARCHAR(500) NOT NULL,
    file_url        TEXT NOT NULL,
    thumbnail_url   TEXT,
    bucket_name     VARCHAR(100) NOT NULL,
    uploaded_by     INT NOT NULL,
    uploaded_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         BOOLEAN DEFAULT FALSE,
    deleted_at      TIMESTAMP,
    INDEX idx_attachment_task_id (task_id),
    INDEX idx_attachment_uploaded_by (uploaded_by),
    INDEX idx_attachment_uploaded_at (uploaded_at),
    CONSTRAINT fk_attachment_task FOREIGN KEY (task_id) REFERENCES tasks(task_id) ON DELETE CASCADE,
    CONSTRAINT fk_attachment_user FOREIGN KEY (uploaded_by) REFERENCES users(user_id) ON DELETE RESTRICT
) ENGINE=InnoDB;

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

-- Table: teams
CREATE TABLE teams (
    team_id         INT AUTO_INCREMENT PRIMARY KEY,
    organization_id INT NOT NULL,
    name            VARCHAR(100) NOT NULL,
    description     TEXT,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_team_organization (organization_id),
    FOREIGN KEY (organization_id) REFERENCES organizations(org_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Table: team_members
CREATE TABLE team_members (
    team_id INT NOT NULL,
    user_id INT NOT NULL,
    PRIMARY KEY(team_id, user_id),
    FOREIGN KEY (team_id) REFERENCES teams(team_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;