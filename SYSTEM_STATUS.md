# SynergyHub - System Status Documentation

## Overview
SynergyHub is a collaborative project management system with real-time communication capabilities, built on Spring Boot backend and React TypeScript frontend. The system supports multi-organization workspaces with role-based access control, agile project management features, and integrated video conferencing.

---

## Architecture

### Technology Stack
- **Backend**: Spring Boot, MySQL, WebSocket, JWT Authentication, LiveKit for video conferencing
- **Frontend**: React with TypeScript, Vite, TailwindCSS, WebSocket client
- **Infrastructure**: Docker, Caddy reverse proxy, S3-compatible storage

### Key Architecture Patterns
- Multi-tenant organization model
- Role-Based Access Control (RBAC)
- RESTful API design
- WebSocket for real-time features
- JWT-based authentication with refresh tokens

---

## Implemented Features

### 1. Authentication & Authorization

#### User Authentication
- **Registration**: Email-based registration with email verification
- **Login**: Email/password authentication with optional 2FA support
- **Email Verification**: Token-based email verification system
- **Password Management**: 
  - Forgot password flow with email-based token reset
  - Password reset with token validation
  - Change password for authenticated users
  - Resend verification email functionality
- **Session Management**:
  - JWT access tokens with refresh token mechanism
  - Multi-device session tracking
  - View all active sessions
  - Revoke individual sessions
  - Revoke all other sessions except current

#### Two-Factor Authentication
- Enable/disable 2FA
- TOTP-based authentication
- Backup codes generation and verification
- QR code generation for authenticator apps

#### Single Sign-On (SSO)
- OAuth2 integration (Google, GitHub)
- Organization-level SSO provider management
- Multiple SSO providers per organization
- Enable/disable SSO providers
- Secret rotation for SSO credentials
- SSO user mapping and automatic account linking

### 2. Organization Management

#### Organization Lifecycle
- Create organization
- Update organization details (name, address, contact email)
- Delete organization
- Check if user belongs to an organization

#### Organization Membership
- Join organization via invite code
- Request to join via organization email
- Generate time-limited invite codes
- Organization context switching

### 3. Team Management

#### Team Operations
- Create teams within organization
- List organization teams
- View team details with member information
- Delete teams
- Add members to teams
- Remove members from teams

### 4. Role-Based Access Control (RBAC)

#### Role Management
- Create custom roles within organization
- List all roles in organization
- View role details with assigned permissions
- Update role metadata and permissions
- Delete custom roles (system roles protected)
- Assign multiple permissions to roles

#### Permission Management
- Fetch all available system permissions
- Granular permission assignment
- Permission-based access control throughout system

### 5. Project Management

#### Project Lifecycle
- Create projects within organization
- Update project details (name, description, key, dates)
- Delete projects
- Archive/unarchive projects
- Search and filter projects
- Paginated project listing

#### Project Access
- Get project details
- Check project membership
- View project timeline
- View project activity stream

#### Project Members
- Add members to project
- Remove members from project
- Update member roles
- List project members

### 6. Sprint Management (Agile)

#### Sprint Operations
- Create sprints within projects
- Update sprint details (name, goal, dates)
- Start sprint (activate)
- Complete sprint
- Cancel sprint
- Delete sprints

#### Sprint Views
- Get sprint by ID
- Get sprint details with task breakdown
- List sprints by project
- Filter sprints by status (PLANNED, ACTIVE, COMPLETED, CANCELLED)

### 7. Task/Issue Management

#### Task Hierarchy
- Support for multiple issue types (EPIC, STORY, TASK, BUG, SUBTASK)
- Parent-child relationships (Epic > Story > Task/Subtask)
- Epic children tracking

#### Task Operations
- Create tasks/issues
- Update task details
- Delete tasks
- Archive/unarchive tasks
- Assign/unassign tasks to users
- Update task status (TO_DO, IN_PROGRESS, IN_REVIEW, DONE, CANCELLED)
- Set task priority (LOW, MEDIUM, HIGH, CRITICAL)
- Set story points

#### Task Organization
- Move tasks to sprint
- Move tasks to backlog
- Get tasks by project
- Get tasks by sprint
- Get backlog tasks
- Get user's assigned tasks
- Get task subtasks

#### Task Details
- Get single task with full details
- View task relationships
- Track task assignee
- Manage task dates (start date, due date)

### 8. Board View (Kanban)

#### Board Features
- Get project board view
- Organized by sprint status
- Shows active sprints with tasks grouped by status
- Backlog section for unplanned tasks
- Real-time task status visualization

### 9. Timeline View (Gantt-style)

#### Timeline Features
- Project timeline visualization
- Sprint timeline with start/end dates
- Task timeline with date ranges
- Configurable time window (default 6 months)
- Completion percentage tracking
- Sprint and task progress indicators

### 10. Comments & Collaboration

#### Comment Features
- Add comments to tasks
- Update existing comments
- Delete comments
- Paginated comment listing (default 50 per page)
- Author information with each comment
- Timestamp tracking

### 11. File Attachments

#### Attachment Management
- Upload files to tasks
- S3-compatible storage backend
- Get task attachments
- Delete attachments
- Generate download URLs
- File metadata tracking (filename, size, content type)
- Upload progress tracking

### 12. Activity Stream

#### Activity Tracking
- Project-level activity logging
- Track all significant events:
  - Project creation/updates
  - Task creation/updates/status changes
  - Sprint lifecycle events
  - Member additions/removals
  - Comment activity
- Paginated activity feed (default 20 per page)
- User attribution for all activities
- Timestamp for each activity

### 13. Real-Time Chat

#### Chat Features
- Project-scoped chat rooms
- Send text messages
- Real-time message delivery via WebSocket
- Message history retrieval
- User presence indicators
- Message metadata (sender, timestamp)

### 14. Video Meetings

#### Meeting Management
- Create scheduled meetings
- Create instant meetings
- Join meetings
- Get meeting details
- List project meetings
- Generate meeting join tokens

#### Meeting Features
- LiveKit integration for WebRTC
- Project-scoped meetings
- Meeting status tracking (SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED)
- Participant tracking
- Host/organizer designation
- Meeting codes for joining
- Scheduled start time support

### 15. User Profile Management

#### Profile Operations
- Get current user profile
- Update profile information
- Change password (authenticated users)
- Profile picture/avatar support

### 16. Security Features

#### Security Measures
- IP address tracking for all security-sensitive operations
- User agent tracking
- Login attempt monitoring
- Password strength validation
- Account lockout protection
- Email verification requirement
- Rate limiting on authentication endpoints
- Secure token storage
- HTTPS enforcement via Caddy

---

## User Flows

### Authentication Flow

#### New User Registration
1. User fills registration form (name, email, password)
2. System validates input and creates unverified account
3. Verification email sent to user
4. User clicks verification link in email
5. Email verified, account activated
6. User can now log in

#### Login Flow
1. User enters email and password
2. If 2FA enabled, user prompted for TOTP code
3. System validates credentials
4. JWT access token and refresh token issued
5. User redirected to dashboard or organization selection
6. Session tracked in backend

#### Password Reset Flow
1. User clicks "Forgot Password"
2. User enters email address
3. Reset token sent to email
4. User clicks reset link
5. User enters new password
6. Token validated and password updated
7. User redirected to login

### Organization Setup Flow

#### First-Time User
1. After login, system checks if user has organization
2. If no organization, show welcome screen with two options:
   - Create new organization
   - Join existing organization
3. For create: User fills organization details, organization created
4. For join: User enters invite code or organization email
5. User assigned to organization with default member role

#### Organization Management
1. Admin accesses organization settings
2. Admin can update organization details
3. Admin generates invite codes for new members
4. Invite codes shared with team members
5. New members join using invite codes

### Project Management Flow

#### Project Creation
1. User (with appropriate permissions) navigates to projects page
2. Clicks "Create Project"
3. Fills project details (name, key, description, dates)
4. Project created within organization
5. User automatically added as project member
6. User can invite team members to project

#### Working with Projects
1. User selects project from projects list
2. Lands on project board view (default)
3. Can navigate between:
   - Board (Kanban view)
   - Backlog (issue list)
   - Timeline (Gantt view)
   - Activity (event stream)
   - Chat (team communication)
   - Meetings (video conferences)
   - Settings (project configuration)

### Sprint & Task Management Flow

#### Sprint Workflow
1. User creates sprint with name, goal, and dates
2. Sprint starts in PLANNED status
3. User adds tasks from backlog to sprint
4. User starts sprint, status changes to ACTIVE
5. Team works on sprint tasks, updating statuses
6. User completes sprint when work done
7. Incomplete tasks can be moved to next sprint or backlog

#### Task Creation & Management
1. User creates task from backlog or board view
2. Fills task details (title, description, type, priority)
3. Task appears in backlog
4. User can:
   - Drag task to sprint
   - Assign to team member
   - Set story points
   - Add subtasks
   - Update status
   - Add comments
   - Attach files
5. Task moves through workflow: TO_DO → IN_PROGRESS → IN_REVIEW → DONE

### Board View Usage
1. User navigates to project board
2. Sees columns for each status (TO_DO, IN_PROGRESS, IN_REVIEW, DONE)
3. Active sprint tasks shown grouped by status
4. Backlog section shows unplanned tasks
5. User can drag-drop tasks between columns to update status
6. Real-time updates reflect across all team members

### Timeline Planning
1. User navigates to timeline view
2. Sees Gantt-style visualization of sprints and tasks
3. Sprints shown as bars with start/end dates
4. Tasks with dates shown on timeline
5. Can adjust view window (months ahead)
6. Progress indicators show completion status

### Real-Time Collaboration

#### Chat Flow
1. User navigates to project chat
2. Existing messages loaded from history
3. User types and sends message
4. Message broadcast to all project members via WebSocket
5. Other users see message appear in real-time
6. Messages persisted to database

#### Meeting Flow
1. User creates meeting (instant or scheduled)
2. Meeting code generated
3. User shares meeting link with team
4. Participants join meeting via unique join token
5. LiveKit handles WebRTC connection
6. Video/audio streams established
7. Meeting metadata tracked in database

### RBAC Flow

#### Role Management
1. Organization admin navigates to roles settings
2. Views list of existing roles
3. Creates custom role with specific name
4. Selects permissions to assign to role
5. Saves role
6. Role available for user assignment

#### User Permission Assignment
1. Admin assigns user to project/organization role
2. System checks user permissions for each action
3. Actions allowed/denied based on role permissions
4. Permissions evaluated at organization and project levels

### Activity Monitoring
1. User navigates to project activity feed
2. System displays chronological event stream
3. Shows who did what and when
4. Includes task updates, comments, sprint changes, etc.
5. Paginated for performance
6. Real-time updates when new activities occur

---

## Data Models

### Core Entities
- **User**: User accounts with authentication credentials
- **Organization**: Top-level tenant entity
- **Team**: Groups of users within organization
- **Project**: Work containers within organization
- **Sprint**: Time-boxed work periods
- **Task**: Work items with hierarchy support
- **Comment**: Task discussions
- **Attachment**: File uploads on tasks
- **Role**: Permission containers
- **Permission**: Granular access rights
- **UserSession**: Active authentication sessions
- **ActivityLog/AuditLog**: System event tracking
- **ChatMessage**: Real-time communication
- **Meeting**: Video conference sessions
- **SsoProvider**: OAuth2 provider configurations
- **UserOrganization**: User-organization membership mapping
- **ProjectMember**: User-project membership mapping

### Security Entities
- **TwoFactorSecret**: TOTP secrets for 2FA
- **BackupCode**: 2FA recovery codes
- **EmailVerification**: Email verification tokens
- **PasswordResetToken**: Password reset tokens
- **LoginAttempt**: Failed login tracking
- **SsoUserMapping**: OAuth user identity mapping

---

## Integration Points

### External Services
- **Email Service**: SMTP for transactional emails (verification, password reset)
- **LiveKit**: WebRTC server for video meetings
- **S3 Storage**: File attachment storage
- **OAuth2 Providers**: Google and GitHub SSO

### WebSocket Endpoints
- Chat messages: Real-time project chat delivery
- Meeting updates: Participant join/leave notifications
- Task updates: Real-time board synchronization

---

## Current Limitations & Notes

### Known Constraints
- Single organization per user (can be in only one org at a time)
- No task time tracking/logging
- No recurring meetings
- No meeting recording storage (LiveKit handles this separately)
- No custom workflow states (fixed: TO_DO, IN_PROGRESS, IN_REVIEW, DONE, CANCELLED)
- No bulk operations for tasks
- No task templates
- No project templates
- No advanced filtering/search on tasks beyond basic queries
- No reporting/analytics dashboards
- No webhook support for external integrations
- No API rate limiting visible to users
- No notification system (email/push) for task assignments or mentions

### Development Status
- All core features implemented and functional
- WebSocket integration active for chat
- LiveKit integration active for meetings
- RBAC fully enforced on backend
- Frontend implements all backend features
- Authentication flows complete with 2FA and SSO
- File upload/download operational with S3 backend

---

## Security Posture

### Implemented Security Controls
- JWT-based stateless authentication
- Refresh token rotation
- Session management with device tracking
- IP address logging for audit trail
- User agent tracking
- 2FA support with TOTP
- Email verification requirement
- Password reset with token expiration
- Role-based authorization
- Permission-based access control
- Secure SSO integration
- HTTPS enforcement
- CORS configuration

### Authentication Mechanisms
- Email/password (primary)
- OAuth2 (Google, GitHub)
- TOTP 2FA (optional)
- SSO (organization-level)

---

## API Structure

### Base URL Pattern
All API endpoints follow pattern: `/api/{resource}`

### Authentication
All endpoints except public auth endpoints require:
- `Authorization: Bearer {jwt_token}` header

### Organization Context
Many endpoints require organization context via:
- User's organization membership
- Organization ID in path: `/api/organizations/{orgId}/*`

### Common Response Format
All responses follow consistent structure with ApiResponse wrapper containing success status, message, and data payload.

---

## Frontend Architecture

### Key Pages
- Authentication pages (login, register, email verification, password reset)
- Organization welcome/setup
- Projects listing and details
- Board view (Kanban)
- Backlog view (issue list)
- Timeline view (Gantt)
- Activity feed
- Chat room
- Meeting room
- Settings (user profile, organization, security, project settings)
- Teams management
- RBAC management

### State Management
- Local storage for auth tokens and user data
- React hooks for component state
- Context providers for global state (organization, user)

### Real-Time Features
- WebSocket connection for chat
- LiveKit client for video meetings
- Automatic reconnection handling

---

## Next Steps for Future Agent

This document provides a complete snapshot of the current system implementation. All features described are functional and tested based on the existing codebase. When planning new features or modifications, refer to this document as the baseline system state.

Key areas for potential enhancement:
- Advanced analytics and reporting
- Notification system
- Custom workflows
- Time tracking
- Project/task templates
- Advanced search and filtering
- Webhook integrations
- Mobile applications
- Offline support
- Enhanced collaboration features (mentions, reactions, threads)
