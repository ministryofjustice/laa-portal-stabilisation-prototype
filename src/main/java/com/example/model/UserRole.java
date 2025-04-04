package com.example.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserRole {
    private String appId;
    private String appName;
    private String appRoleId;
    private String roleName;
    private String assignmentId;
    private String appRoleName;
    private String appRoleAssignmentId;
}
