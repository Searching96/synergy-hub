DROP DATABASE IF EXISTS synergy_hub;
CREATE DATABASE synergy_hub;
USE synergy_hub;

-- Table: organizations
CREATE TABLE organizations (
    org_id      INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    address     VARCHAR(255),
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- Table: users (each user belongs to an organization)
CREATE TABLE users (
    user_id       INT AUTO_INCREMENT PRIMARY KEY,
    organization_id INT NOT NULL,
    name          VARCHAR(100) NOT NULL,
    email         VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    two_factor_enabled BOOLEAN DEFAULT FALSE,
    created_at    DATETIME DEFAULT CURRENT_TIMESTAMP,
    -- Foreign key to organizations (multi-tenant isolation)
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
INSERT INTO users (user_id, organization_id, name, email, password_hash, two_factor_enabled) VALUES
(1, 1, 'Alice Johnson', 'alice@technova.com', 'password123', FALSE),
(2, 1, 'Bob Smith', 'bob@technova.com', 'password123', TRUE),
(3, 2, 'Carol White', 'carol@healthplus.com', 'password123', FALSE),
(4, 2, 'David Young', 'david@healthplus.com', 'password123', FALSE),
(5, 3, 'Eve Jackson', 'eve@edufuture.com', 'password123', FALSE);

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
    description TEXT
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
    project_lead_id INT,  -- user who leads or manages the project (nullable)
    start_date      DATE,
    end_date        DATE,
    status          VARCHAR(20) DEFAULT 'ACTIVE',
    FOREIGN KEY (organization_id) REFERENCES organizations(org_id) ON DELETE CASCADE,
    FOREIGN KEY (project_lead_id) REFERENCES users(user_id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- Table: project_members (which users are part of a project team)
CREATE TABLE project_members (
    project_id INT NOT NULL,
    user_id    INT NOT NULL,
    role       VARCHAR(50),
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
-- role field here is optional descriptive (could also omit since roles are mostly global)
INSERT INTO project_members (project_id, user_id, role) VALUES
-- Members for Project Alpha (proj 1 in org 1)
(1, 1, 'Organization Admin'),    -- Alice (Org Admin) included by default
(1, 2, 'Project Manager'),       -- Bob (Project Manager) is lead
(1, 4, 'Team Member'),           -- David (from another org? Actually user4 is David of HealthPlus, which normally wouldn't join TechNova's project; for example sake we keep within same org usually. Let's use user with same org)
(2, 1, 'Organization Admin'),    -- Project Beta (proj 2 in org 1): Alice
(2, 2, 'Project Manager'),       -- Bob 
(3, 3, 'Team Member'),           -- Health App (proj 3 in org 2): Carol
(3, 4, 'Team Member'),           -- David
(4, 5, 'Guest');                 -- E-Learning (proj 4 in org 3): Eve (Guest)

-- Table: sprints (each sprint belongs to a project)
CREATE TABLE sprints (
    sprint_id    INT AUTO_INCREMENT PRIMARY KEY,
    project_id   INT NOT NULL,
    name         VARCHAR(100) NOT NULL,
    goal         TEXT,
    start_date   DATE NOT NULL,
    end_date     DATE NOT NULL,
    status       ENUM('PLANNED','ACTIVE','COMPLETED') DEFAULT 'PLANNED',
    FOREIGN KEY (project_id) REFERENCES projects(project_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Table: tasks (product backlog items, issues, or sprint tasks)
CREATE TABLE tasks (
    task_id         INT AUTO_INCREMENT PRIMARY KEY,
    project_id      INT NOT NULL,
    sprint_id       INT,  -- if assigned to a sprint (NULL if in backlog)
    title           VARCHAR(255) NOT NULL,
    description     TEXT,
    type            ENUM('Story','Bug','Chore','Task') DEFAULT 'Task',
    status          ENUM('To Do','In Progress','Done') DEFAULT 'To Do',
    priority        ENUM('Low','Medium','High') DEFAULT 'Medium',
    story_points    INT,
    due_date        DATE,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    assignee_id     INT,   -- user responsible for the task (nullable)
    reporter_id     INT,   -- user who created/reported the task
    parent_task_id  INT,   -- for sub-task grouping (self-referential)
    FOREIGN KEY (project_id) REFERENCES projects(project_id) ON DELETE CASCADE,
    FOREIGN KEY (sprint_id)  REFERENCES sprints(sprint_id) ON DELETE SET NULL,
    FOREIGN KEY (assignee_id) REFERENCES users(user_id) ON DELETE SET NULL,
    FOREIGN KEY (reporter_id) REFERENCES users(user_id) ON DELETE SET NULL,
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
-- Tasks for Project Alpha (proj_id=1, org 1)
(1, 1, 1, 'Setup project repository', 'Initialize git repository and CI pipeline', 'Task', 'Done', 'Medium', 1, '2025-10-05', 2, 2, NULL),
(2, 1, 1, 'Implement authentication module', 'Develop login, registration APIs with JWT', 'Story', 'In Progress', 'High', 8, '2025-10-10', 1, 2, NULL),
(3, 1, 1, 'Login page UI', 'Create frontend login page (sub-task of auth module)', 'Task', 'To Do', 'Medium', 3, '2025-10-10', 1, 2, 2),  -- sub-task of task 2
(4, 1, NULL, 'Research OAuth integration', 'Investigate adding Google OAuth2 login', 'Chore', 'To Do', 'Low', 2, '2025-10-20', 2, 1, NULL),  -- backlog item (no sprint)
-- Task for Project Beta (proj_id=2, org 1)
(5, 2, NULL, 'Upgrade server hardware', 'Install new servers for production environment', 'Story', 'To Do', 'High', 5, '2025-11-01', 1, 1, NULL);

-- Insert task dependencies (e.g., Task 2 depends on Task 1)
INSERT INTO task_dependencies (task_id, depends_on_task) VALUES
(2, 1);  -- "Implement authentication module" (task 2) is blocked by "Setup project repository" (task 1)

-- Table: resources
CREATE TABLE resources (
    resource_id   INT AUTO_INCREMENT PRIMARY KEY,
    organization_id INT NOT NULL,
    name          VARCHAR(100) NOT NULL,
    resource_type ENUM('Room','Equipment','License','Other') DEFAULT 'Other',
    details       TEXT,
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

-- Insert sample resources
INSERT INTO resources (resource_id, organization_id, name, resource_type, details) VALUES
(1, 1, 'Conference Room A', 'Room', 'Main office large meeting room'),
(2, 1, '3D Printer', 'Equipment', 'Industrial 3D printer in lab'),
(3, 2, 'MRI Machine', 'Equipment', 'Imaging device in Radiology dept'),
(4, 3, 'Training Room', 'Room', 'Room for workshops and training');

-- Insert sample bookings
INSERT INTO bookings (booking_id, resource_id, user_id, start_time, end_time, purpose) VALUES
(1, 1, 1, '2025-10-05 09:00:00', '2025-10-05 10:00:00', 'Project Alpha kickoff meeting'),   -- Alice booked Conference Room A
(2, 1, 2, '2025-10-05 11:00:00', '2025-10-05 12:00:00', 'Sprint retrospective'),           -- Bob booked Conference Room A later that day
(3, 2, 2, '2025-10-07 14:00:00', '2025-10-07 16:00:00', 'Printing prototype model'),       -- Bob booked 3D Printer
(4, 3, 3, '2025-10-10 08:00:00', '2025-10-10 12:00:00', 'Routine maintenance check'),       -- Carol booked MRI Machine
(5, 4, 5, '2025-05-20 09:00:00', '2025-05-20 17:00:00', 'On-site training event');          -- Eve booked Training Room

-- Table: events (calendar events/meetings)
CREATE TABLE events (
    event_id      INT AUTO_INCREMENT PRIMARY KEY,
    organization_id INT NOT NULL,
    project_id    INT,       -- optional: link to a project if relevant
    title         VARCHAR(200) NOT NULL,
    description   TEXT,
    start_time    DATETIME NOT NULL,
    end_time      DATETIME NOT NULL,
    created_by    INT,       -- user who created the event
    FOREIGN KEY (organization_id) REFERENCES organizations(org_id) ON DELETE CASCADE,
    FOREIGN KEY (project_id) REFERENCES projects(project_id) ON DELETE SET NULL,
    FOREIGN KEY (created_by) REFERENCES users(user_id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- Insert sample events
INSERT INTO events (event_id, organization_id, project_id, title, description, start_time, end_time, created_by) VALUES
(1, 1, 1, 'Sprint 1 Planning', 'Planning meeting for Sprint 1 of Project Alpha', '2025-09-30 10:00:00', '2025-09-30 11:00:00', 2),
(2, 1, NULL, 'TechNova Town Hall', 'Monthly all-hands meeting', '2025-10-15 16:00:00', '2025-10-15 17:00:00', 1),
(3, 2, 3, 'Health App Demo', 'Demonstration of new Health App features to stakeholders', '2025-10-10 09:00:00', '2025-10-10 10:30:00', 3),
(4, 3, 4, 'Workshop: New LMS Training', 'Training session for the new E-Learning platform', '2025-05-20 09:00:00', '2025-05-20 11:00:00', 5);

-- Table: comments (task comments)
CREATE TABLE comments (
    comment_id   INT AUTO_INCREMENT PRIMARY KEY,
    task_id      INT NOT NULL,
    user_id      INT NOT NULL,
    content      TEXT NOT NULL,
    created_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (task_id) REFERENCES tasks(task_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Table: chat_channels
CREATE TABLE chat_channels (
    channel_id   INT AUTO_INCREMENT PRIMARY KEY,
    organization_id INT NOT NULL,
    project_id   INT,   -- if this is a project-specific channel
    task_id      INT,   -- if this is a task-specific channel
    name         VARCHAR(100),
    channel_type ENUM('TEAM','PROJECT','TASK','DIRECT') NOT NULL,
    FOREIGN KEY (organization_id) REFERENCES organizations(org_id) ON DELETE CASCADE,
    FOREIGN KEY (project_id) REFERENCES projects(project_id) ON DELETE CASCADE,
    FOREIGN KEY (task_id) REFERENCES tasks(task_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Table: channel_members (users in a chat channel, for direct or private channels)
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
    parent_message_id INT,   -- for threaded reply (null if top-level message)
    FOREIGN KEY (channel_id) REFERENCES chat_channels(channel_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (parent_message_id) REFERENCES chat_messages(message_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Insert sample comments on tasks
INSERT INTO comments (comment_id, task_id, user_id, content, created_at) VALUES
(1, 2, 1, 'Reminder: Consider password reset feature in auth module.', '2025-10-01 09:00:00'),  -- Alice on Task 2
(2, 2, 2, 'Good point, we will add that in a future sprint.', '2025-10-01 09:30:00'),          -- Bob replies on Task 2
(3, 4, 2, 'Any update on OAuth research? Need info by next week.', '2025-10-08 15:45:00');     -- Bob commenting on Task 4

-- Insert sample chat channels
INSERT INTO chat_channels (channel_id, organization_id, project_id, task_id, name, channel_type) VALUES
(1, 1, NULL, NULL, 'General Chat', 'TEAM'),            -- Org 1 general channel
(2, 1, 1, NULL, 'Project Alpha Chat', 'PROJECT'),      -- Project Alpha channel
(3, 1, 1, 2, 'Auth Module Discussion', 'TASK'),        -- Task-specific channel for Task 2
(4, 1, NULL, NULL, NULL, 'DIRECT');                    -- Direct message channel between two users (members defined below)

-- Insert channel members for channel 4 (direct channel between Alice and Bob)
INSERT INTO channel_members (channel_id, user_id) VALUES
(4, 1),  -- Alice
(4, 2);  -- Bob

-- Insert sample chat messages
-- General channel (Org 1) messages
INSERT INTO chat_messages (message_id, channel_id, user_id, content, sent_at, parent_message_id) VALUES
(1, 1, 2, 'Hello team, welcome to the SynergyHub platform!', '2025-10-01 08:00:00', NULL),
(2, 1, 1, 'Glad to be here! Excited to collaborate.', '2025-10-01 08:05:00', NULL);

-- Project channel (Project Alpha) messages, including a threaded reply
INSERT INTO chat_messages (message_id, channel_id, user_id, content, sent_at, parent_message_id) VALUES
(3, 2, 2, 'Sprint 1 is underway. Please update your task statuses daily.', '2025-10-02 10:00:00', NULL),
(4, 2, 1, 'Sure, will do @Bob.', '2025-10-02 10:02:00', 3);  -- Alice replying to Bob's message (threaded under message_id 3)

-- Direct channel messages (between Alice and Bob)
INSERT INTO chat_messages (message_id, channel_id, user_id, content, sent_at, parent_message_id) VALUES
(5, 4, 1, 'Hey Bob, can you review my code later today?', '2025-10-02 14:00:00', NULL),
(6, 4, 2, 'Yes Alice, I will review it by 5 PM.', '2025-10-02 14:05:00', NULL);



