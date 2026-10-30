package com.acme.salary.security;

public enum Role {
    /** Everything, plus managing users and reading the audit log. */
    ADMIN,
    /** Creates and edits employees and salaries. */
    HR_MANAGER,
    /** Read-only, e.g. finance or leadership. */
    VIEWER
}
