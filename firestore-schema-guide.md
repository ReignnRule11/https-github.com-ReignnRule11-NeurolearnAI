# 🛡️ Firestore Multi-Tenant Architecture & Security Guide

This document outlines the **Firestore database schema design, security policies, and tenant-based partitioning** implemented for the NeuroLearn AI enterprise learning platform. It explains how school-level data isolation is cryptographically and logically enforced at the database layer using Firestore Security Rules.

---

## 🏛️ Multi-Tenant Partitioning Model

To ensure robust data isolation between distinct educational institutions (e.g., *Stanford University*, *MIT Engineering*, *UNICEF Global*), NeuroLearn AI uses a **single-instance multi-tenant architecture with logical document-level partitioning**.

### 1. The Partition Key (`tenantId`)
Every document within tenant-sensitive collections contains a mandatory `tenantId` field. This string maps directly to a verified school domain ID (e.g. `stanford_univ`). 
- **Enforcement**: Direct comparisons are executed in every read and write request. If a document's `tenantId` does not match the authenticated user's assigned `tenantId` claim, Firestore immediately rejects the request at the gateway level.
- **Hierarchical Inheritance**: Subcollections (such as the `/decks/{deckId}/cards/{cardId}` branch) inherit their tenant context by querying the parent deck's `tenantId` field.

### 2. Double-Layer Tenant Handshake
To prevent client-side spoofing, user tenancy is validated via a dual-verification sequence:
1. **Primary (Fast Claim)**: Validated against the `tenantId` property injected directly into the user's secure **Firebase Auth Custom Claims** token.
2. **Secondary (Document Lookup)**: If custom claims are not yet propagated, the rules execute a server-side read against `/users/$(request.auth.uid)` to fetch and compare the live, authenticated `tenantId` field.

---

## 🎭 Role-Based Access Control (RBAC) Matrix

Users are assigned one of six rigid security clearance roles, which align with the `UserRole` enum declared in `EnterpriseBackend.kt`:

| Role Name | Enum Mapping | System Clearances & Write Allowances |
| :--- | :--- | :--- |
| **Super Admin** | `SUPER_ADMIN` | Full global database read/write access. Audits security logs, overrides sync configs, and manages all tenant entities. |
| **School Admin** | `SCHOOL_ADMIN` | Configures institutional settings and manages users **only** within their matching `tenantId` partition. Reads local audit logs. |
| **Teacher** | `TEACHER` | Creates learning tracks, organizes decks/cards, and reviews student mastery lists inside their tenant. |
| **Student** | `STUDENT` | Accesses courses, completes flashcard sessions, and publishes verified talent profiles. Writes own data. |
| **Parent** | `PARENT` | Read-only access to matched student performance telemetry, logs, and calendar milestones. |
| **Recruiter** | `RECRUITER` | Read-only access to verified student talent pool directory. Cannot view sensitive institutional course materials. |

---

## 🔒 Security Rules Implementation Details

The corresponding `/firestore.rules` file implements these rules. Below are the key security constructs:

### Utility Helpers
```javascript
// Validates authentication status
function isAuthenticated() {
  return request.auth != null;
}

// Retrieves authenticated user's database document
function getUserDoc() {
  return get(/databases/$(database)/documents/users/$(request.auth.uid)).data;
}

// Validates the tenant partition constraint
function matchesTenant(tenantId) {
  return isAuthenticated() && (
    request.auth.token.tenantId == tenantId || 
    getUserDoc().tenantId == tenantId
  );
}

// Validates the user's role privilege
function hasRole(role) {
  return isAuthenticated() && (
    request.auth.token.role == role || 
    getUserDoc().role == role
  );
}
```

### Tamper-Proof Audit Logging
Audit logs are stored in the `/audit_logs` collection. To prevent logs from being altered during a security incident, the database enforces a **Write-Once-Read-Only** rule for standard accounts, with completely blocked update and delete actions across the entire cluster:
```javascript
match /audit_logs/{logId} {
  allow read: if isAuthenticated() && (
    hasRole('SUPER_ADMIN') || 
    (hasRole('SCHOOL_ADMIN') && matchesTenant(resource.data.tenantId))
  );
  allow create: if isAuthenticated() && matchesTenant(request.resource.data.tenantId);
  
  // TAMPER-PROOF RULE: Updates and deletions are strictly blocked
  allow update, delete: if false;
}
```

---

## 📁 Recommended Indexing Guidelines

To prevent query execution failures, configure the following compound indexes in the Firebase Console:

1. **Decks Search Index**:
   - Collection Path: `decks`
   - Fields: `userId` (Ascending), `createdAt` (Descending)
   - Scope: `COLLECTION`

2. **Tenant Partition Index**:
   - Collection Path: `decks`
   - Fields: `tenantId` (Ascending), `createdAt` (Descending)
   - Scope: `COLLECTION`

3. **Chronological Audit Ledger**:
   - Collection Path: `audit_logs`
   - Fields: `tenantId` (Ascending), `timestamp` (Descending)
   - Scope: `COLLECTION`
