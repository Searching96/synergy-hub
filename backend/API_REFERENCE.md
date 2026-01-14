# SynergyHub API Reference

**Version:** 1.0  
**Base URL:** `/api`  
**Authentication:** Most endpoints require JWT Bearer token authentication

---

## Table of Contents

1. [Authentication](#authentication)
2. [Users](#users)
3. [Organizations](#organizations)
4. [Roles](#roles)
5. [Permissions](#permissions)
6. [Projects](#projects)
7. [Sprints](#sprints)
8. [Tasks](#tasks)
9. [Board](#board)
10. [Comments](#comments)
11. [Activity Stream](#activity-stream)
12. [SSO Providers](#sso-providers)
13. [Health Check](#health-check)

---

## Authentication

Base path: `/api/auth`

### Login

```http
POST /api/auth/login
```

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "string"
}
```

**Response:**
```json
{
  "success": true,
  "message": "string",
  "data": {
    "accessToken": "string",
    "refreshToken": "string",
    "user": {
      "id": 1,
      "email": "user@example.com",
      "name": "string"
    }
  }
}
```

### Register

```http
POST /api/auth/register
```

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "string",
  "name": "string"
}
```

**Response:**
```json
{
  "success": true,
  "message": "string",
  "data": {
    "id": 1,
    "email": "user@example.com",
    "name": "string"
  }
}
```

### Refresh Token

```http
POST /api/auth/refresh
```

**Request Body:**
```json
{
  "refreshToken": "string"
}
```

**Response:** Same as login response

### Verify Email

```http
POST /api/auth/verify-email
```

**Request Body:**
```json
{
  "token": "string"
}
```

### Resend Verification Email

```http
POST /api/auth/resend-verification
```

**Request Body:**
```json
{
  "email": "user@example.com"
}
```

### Forgot Password

```http
POST /api/auth/forgot-password
```

**Request Body:**
```json
{
  "email": "user@example.com"
}
```

### Validate Reset Token

```http
POST /api/auth/validate-reset-token
```

**Request Body:**
```json
{
  "token": "string"
}
```

**Response:**
```json
{
  "success": true,
  "data": true
}
```

### Reset Password

```http
POST /api/auth/reset-password
```

**Request Body:**
```json
{
  "token": "string",
  "newPassword": "string"
}
```

### Change Password

```http
POST /api/auth/change-password
```

**Authorization:** Required  
**Request Body:**
```json
{
  "currentPassword": "string",
  "newPassword": "string"
}
```

### Two-Factor Authentication

#### Setup 2FA

```http
POST /api/auth/2fa/setup
```

**Authorization:** Required

**Response:**
```json
{
  "success": true,
  "data": {
    "qrCode": "string",
    "secret": "string"
  }
}
```

#### Verify and Enable 2FA

```http
POST /api/auth/2fa/verify
```

**Authorization:** Required  
**Request Body:**
```json
{
  "code": "string"
}
```

#### Disable 2FA

```http
POST /api/auth/2fa/disable
```

**Authorization:** Required  
**Request Body:**
```json
{
  "password": "string"
}
```

#### Verify 2FA Login

```http
POST /api/auth/2fa/verify-login
```

**Request Body:**
```json
{
  "email": "user@example.com",
  "code": "string",
  "temporaryToken": "string"
}
```

### Sessions

#### Get Active Sessions

```http
GET /api/auth/sessions
```

**Authorization:** Required

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "deviceInfo": "string",
      "ipAddress": "string",
      "lastActivity": "2026-01-06T10:00:00Z",
      "current": true
    }
  ]
}
```

#### Revoke Session

```http
DELETE /api/auth/sessions/{sessionId}
```

**Authorization:** Required

---

## Users

Base path: `/api/users`

### Get Current User

```http
GET /api/users/me
```

**Authorization:** Required

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "email": "user@example.com",
    "name": "string",
    "roles": [],
    "permissions": []
  }
}
```

### Logout

```http
POST /api/users/logout
```

**Authorization:** Required

### Logout All Devices

```http
POST /api/users/logout-all
```

**Authorization:** Required

---

## Organizations

Base path: `/api/organizations`

### Create Organization

```http
POST /api/organizations
```

**Authorization:** Required (GLOBAL_ADMIN)  
**Request Body:**
```json
{
  "name": "string",
  "description": "string"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Organization created successfully",
  "data": {
    "id": 1,
    "name": "string",
    "description": "string",
    "createdAt": "2026-01-06T10:00:00Z"
  }
}
```

### Get Organization

```http
GET /api/organizations/{organizationId}
```

**Authorization:** Required (ORG_ADMIN or GLOBAL_ADMIN)

**Response:** Same as create response

### Update Organization

```http
PUT /api/organizations/{organizationId}
```

**Authorization:** Required (ORG_ADMIN or GLOBAL_ADMIN)  
**Request Body:**
```json
{
  "name": "string",
  "description": "string"
}
```

### Delete Organization

```http
DELETE /api/organizations/{organizationId}
```

**Authorization:** Required (ORG_ADMIN or GLOBAL_ADMIN)

---

## Roles

Base path: `/api/organizations/{organizationId}/roles`

### Create Role

```http
POST /api/organizations/{organizationId}/roles
```

**Authorization:** Required (ORG_ADMIN or GLOBAL_ADMIN)  
**Request Body:**
```json
{
  "name": "string",
  "description": "string"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "string",
    "description": "string",
    "permissions": []
  }
}
```

### Get All Roles

```http
GET /api/organizations/{organizationId}/roles
```

**Authorization:** Required

### Get Role

```http
GET /api/organizations/{organizationId}/roles/{roleId}
```

**Authorization:** Required

### Update Role

```http
PUT /api/organizations/{organizationId}/roles/{roleId}
```

**Authorization:** Required (ORG_ADMIN or GLOBAL_ADMIN)  
**Request Body:**
```json
{
  "name": "string",
  "description": "string"
}
```

### Delete Role

```http
DELETE /api/organizations/{organizationId}/roles/{roleId}
```

**Authorization:** Required (ORG_ADMIN or GLOBAL_ADMIN)

### Assign Permissions to Role

```http
POST /api/organizations/{organizationId}/roles/{roleId}/permissions
```

**Authorization:** Required (ORG_ADMIN or GLOBAL_ADMIN)  
**Request Body:**
```json
{
  "permissionIds": [1, 2, 3]
}
```

---

## Permissions

Base path: `/api/permissions`

### Get All Permissions

```http
GET /api/permissions
```

**Authorization:** Required

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "string",
      "description": "string"
    }
  ]
}
```

---

## Projects

Base path: `/api/projects`

### Create Project

```http
POST /api/projects
```

**Authorization:** Required  
**Request Body:**
```json
{
  "name": "string",
  "description": "string",
  "key": "PROJ",
  "organizationId": 1
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "string",
    "description": "string",
    "key": "PROJ",
    "organizationId": 1,
    "createdAt": "2026-01-06T10:00:00Z"
  }
}
```

### Get User Projects

```http
GET /api/projects
```

**Authorization:** Required

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "string",
      "key": "PROJ",
      "description": "string"
    }
  ]
}
```

### Get Project Details

```http
GET /api/projects/{projectId}
```

**Authorization:** Required

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "string",
    "description": "string",
    "key": "PROJ",
    "members": [],
    "sprints": []
  }
}
```

### Update Project

```http
PUT /api/projects/{projectId}
```

**Authorization:** Required  
**Request Body:**
```json
{
  "name": "string",
  "description": "string"
}
```

### Delete Project

```http
DELETE /api/projects/{projectId}
```

**Authorization:** Required

### Archive Project

```http
PUT /api/projects/{projectId}/archive
```

**Authorization:** Required

### Unarchive Project

```http
PUT /api/projects/{projectId}/unarchive
```

**Authorization:** Required

### Project Membership

#### Add Member

```http
POST /api/projects/{projectId}/members
```

**Authorization:** Required  
**Request Body:**
```json
{
  "userId": 1,
  "roleId": 1
}
```

#### Get Members

```http
GET /api/projects/{projectId}/members
```

**Authorization:** Required

#### Remove Member

```http
DELETE /api/projects/{projectId}/members/{userId}
```

**Authorization:** Required

#### Update Member Role

```http
PUT /api/projects/{projectId}/members/{userId}/role
```

**Authorization:** Required  
**Request Body:**
```json
{
  "roleId": 1
}
```

---

## Sprints

Base path: `/api/sprints`

### Create Sprint

```http
POST /api/sprints
```

**Authorization:** Required  
**Request Body:**
```json
{
  "name": "string",
  "projectId": 1,
  "goal": "string",
  "startDate": "2026-01-06",
  "endDate": "2026-01-20"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "string",
    "goal": "string",
    "status": "PLANNED",
    "startDate": "2026-01-06",
    "endDate": "2026-01-20",
    "projectId": 1
  }
}
```

### Get Sprint

```http
GET /api/sprints/{sprintId}
```

**Authorization:** Required

### Get Sprint Details

```http
GET /api/sprints/{sprintId}/details
```

**Authorization:** Required

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "string",
    "goal": "string",
    "status": "ACTIVE",
    "tasks": []
  }
}
```

### Update Sprint

```http
PUT /api/sprints/{sprintId}
```

**Authorization:** Required  
**Request Body:**
```json
{
  "name": "string",
  "goal": "string",
  "startDate": "2026-01-06",
  "endDate": "2026-01-20"
}
```

### Start Sprint

```http
POST /api/sprints/{sprintId}/start
```

**Authorization:** Required

### Complete Sprint

```http
POST /api/sprints/{sprintId}/complete
```

**Authorization:** Required

### Cancel Sprint

```http
POST /api/sprints/{sprintId}/cancel
```

**Authorization:** Required

### Get Project Sprints

```http
GET /api/projects/{projectId}/sprints
```

**Authorization:** Required

**Query Parameters:**
- `status` (optional): Filter by sprint status (PLANNED, ACTIVE, COMPLETED, CANCELLED)

---

## Tasks

Base path: `/api/tasks`

### Create Task

```http
POST /api/tasks
```

**Authorization:** Required  
**Request Body:**
```json
{
  "title": "string",
  "description": "string",
  "type": "STORY",
  "priority": "MEDIUM",
  "projectId": 1,
  "sprintId": 1,
  "assigneeId": 1,
  "parentTaskId": null
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "title": "string",
    "description": "string",
    "type": "STORY",
    "status": "TODO",
    "priority": "MEDIUM",
    "projectId": 1,
    "sprintId": 1,
    "assigneeId": 1,
    "createdAt": "2026-01-06T10:00:00Z"
  }
}
```

### Get Task

```http
GET /api/tasks/{taskId}
```

**Authorization:** Required

### Get Project Tasks

```http
GET /api/projects/{projectId}/tasks
```

**Authorization:** Required

### Get Sprint Tasks

```http
GET /api/sprints/{sprintId}/tasks
```

**Authorization:** Required

### Get Backlog Tasks

```http
GET /api/projects/{projectId}/backlog
```

**Authorization:** Required

### Get My Tasks

```http
GET /api/tasks/my-tasks
```

**Authorization:** Required

### Get Subtasks

```http
GET /api/tasks/{taskId}/subtasks
```

**Authorization:** Required

### Update Task

```http
PUT /api/tasks/{taskId}
```

**Authorization:** Required  
**Request Body:**
```json
{
  "title": "string",
  "description": "string",
  "type": "STORY",
  "priority": "MEDIUM",
  "status": "IN_PROGRESS"
}
```

### Delete Task

```http
DELETE /api/tasks/{taskId}
```

**Authorization:** Required

### Assign Task

```http
POST /api/tasks/{taskId}/assign
```

**Authorization:** Required  
**Request Body:**
```json
{
  "assigneeId": 1
}
```

### Move Task to Sprint

```http
PUT /api/tasks/{taskId}/sprint
```

**Authorization:** Required  
**Request Body:**
```json
{
  "sprintId": 1
}
```

### Move Task to Backlog

```http
PUT /api/tasks/{taskId}/backlog
```

**Authorization:** Required

### Update Task Status

```http
PUT /api/tasks/{taskId}/status
```

**Authorization:** Required  
**Request Body:**
```json
{
  "status": "IN_PROGRESS"
}
```

---

## Board

Base path: `/api/projects/{projectId}/board`

### Get Board View

```http
GET /api/projects/{projectId}/board
```

**Authorization:** Required

**Response:**
```json
{
  "success": true,
  "message": "Board data retrieved successfully",
  "data": {
    "projectId": 1,
    "activeSprints": [],
    "backlog": []
  }
}
```

---

## Comments

Base path: `/api/tasks/{taskId}/comments`

### Add Comment

```http
POST /api/tasks/{taskId}/comments
```

**Authorization:** Required  
**Request Body:**
```json
{
  "content": "string"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Comment added",
  "data": {
    "id": 1,
    "content": "string",
    "authorId": 1,
    "authorName": "string",
    "createdAt": "2026-01-06T10:00:00Z"
  }
}
```

### Get Comments

```http
GET /api/tasks/{taskId}/comments
```

**Authorization:** Required

**Query Parameters:**
- `page` (default: 0): Page number
- `size` (default: 50, max: 200): Page size

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "content": "string",
      "authorId": 1,
      "authorName": "string",
      "createdAt": "2026-01-06T10:00:00Z"
    }
  ]
}
```

---

## Activity Stream

Base path: `/api/projects/{projectId}/activity`

### Get Project Activity

```http
GET /api/projects/{projectId}/activity
```

**Authorization:** Required

**Query Parameters:**
- `page` (default: 0): Page number
- `size` (default: 20, max: 100): Page size

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "type": "TASK_CREATED",
      "description": "string",
      "userId": 1,
      "userName": "string",
      "timestamp": "2026-01-06T10:00:00Z"
    }
  ]
}
```

---

## SSO Providers

Base path: `/api/organizations/{organizationId}/sso/providers`

### Register SSO Provider

```http
POST /api/organizations/{organizationId}/sso/providers
```

**Authorization:** Required (ORG_ADMIN or GLOBAL_ADMIN)  
**Request Body:**
```json
{
  "providerType": "SAML",
  "providerName": "string",
  "clientId": "string",
  "clientSecret": "string",
  "issuerUrl": "string",
  "authorizationUrl": "string",
  "tokenUrl": "string",
  "userInfoUrl": "string"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "providerType": "SAML",
    "providerName": "string",
    "enabled": true,
    "createdAt": "2026-01-06T10:00:00Z"
  }
}
```

### List SSO Providers

```http
GET /api/organizations/{organizationId}/sso/providers
```

**Authorization:** Required (ORG_ADMIN or GLOBAL_ADMIN)

### Get SSO Provider

```http
GET /api/organizations/{organizationId}/sso/providers/{providerId}
```

**Authorization:** Required (ORG_ADMIN or GLOBAL_ADMIN)

### Update SSO Provider

```http
PUT /api/organizations/{organizationId}/sso/providers/{providerId}
```

**Authorization:** Required (ORG_ADMIN or GLOBAL_ADMIN)  
**Request Body:**
```json
{
  "providerName": "string",
  "clientId": "string",
  "issuerUrl": "string"
}
```

### Enable SSO Provider

```http
PUT /api/organizations/{organizationId}/sso/providers/{providerId}/enable
```

**Authorization:** Required (ORG_ADMIN or GLOBAL_ADMIN)

### Disable SSO Provider

```http
PUT /api/organizations/{organizationId}/sso/providers/{providerId}/disable
```

**Authorization:** Required (ORG_ADMIN or GLOBAL_ADMIN)

### Rotate SSO Secret

```http
POST /api/organizations/{organizationId}/sso/providers/{providerId}/rotate
```

**Authorization:** Required (ORG_ADMIN or GLOBAL_ADMIN)  
**Request Body:**
```json
{
  "newClientSecret": "string"
}
```

### Delete SSO Provider

```http
DELETE /api/organizations/{organizationId}/sso/providers/{providerId}
```

**Authorization:** Required (ORG_ADMIN or GLOBAL_ADMIN)

---

## Health Check

### Get Health Status

```http
GET /api/health
```

**No authorization required**

**Response:**
```json
{
  "success": true,
  "data": {
    "status": "UP",
    "timestamp": "2026-01-06T10:00:00Z",
    "service": "SynergyHub Backend"
  }
}
```

---

## Common Response Format

All API responses follow this structure:

```json
{
  "success": true,
  "message": "Optional message",
  "data": {}
}
```

### Error Response

```json
{
  "success": false,
  "message": "Error description",
  "data": null
}
```

---

## HTTP Status Codes

- `200 OK` - Successful GET, PUT, PATCH requests
- `201 Created` - Successful POST requests
- `204 No Content` - Successful DELETE requests
- `400 Bad Request` - Validation errors
- `401 Unauthorized` - Authentication required
- `403 Forbidden` - Insufficient permissions
- `404 Not Found` - Resource not found
- `500 Internal Server Error` - Server error

---

## Authentication Header

For protected endpoints, include the JWT token in the Authorization header:

```
Authorization: Bearer <your-jwt-token>
```

---

## Validation Constraints

- All IDs must be positive integers
- Email addresses must be valid format
- Passwords must meet security requirements
- Required fields are marked in request bodies
- String length limits apply to most text fields

---

## Pagination

Endpoints supporting pagination use these query parameters:

- `page` - Zero-based page number (default: 0)
- `size` - Number of items per page (varies by endpoint)

---

*Last Updated: January 6, 2026*
