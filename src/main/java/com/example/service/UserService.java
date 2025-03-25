package com.example.service;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.example.utils.RandomPasswordGenerator;
import com.microsoft.graph.models.*;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.kiota.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * userService
 */
@Service
public class UserService {

    private static final String AZURE_CLIENT_ID = System.getenv("AZURE_CLIENT_ID");
    private static final String AZURE_TENANT_ID = System.getenv("AZURE_TENANT_ID");
    private static final String AZURE_CLIENT_SECRET = System.getenv("AZURE_CLIENT_SECRET");
    private static final String USER = "d59460b7-75f6-41e9-acfb-2c87fa5a9e3f";//pijian.liao1@mojodevlexternal.onmicrosoft.com
    private static GraphServiceClient graphClient;

    Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * create User at Entra
     *
     * @return {@code User}
     */
    public static Invitation inviteUser(String email, String application, String role, String office) {

        Invitation invitation = new Invitation();
        invitation.setInvitedUserEmailAddress(email);
        invitation.setInviteRedirectUrl("http://localhost:8080");
        InvitedUserMessageInfo invitedUserMessageInfo = new InvitedUserMessageInfo();
        invitedUserMessageInfo.setCustomizedMessageBody("Welcome to LAA");
        invitation.setInvitedUserMessageInfo(invitedUserMessageInfo);
        invitation.setSendInvitationMessage(true);
        GraphServiceClient graphClient = getGraphClient();
        Invitation result = graphClient.invitations().post(invitation);
        if (Objects.nonNull(result) && Objects.nonNull(result.getInvitedUser())) {
            User user = result.getInvitedUser();
            user.setOfficeLocation(office);
            graphClient.users()
                    .byUserId(user.getId()).patch(user);

            ServicePrincipalCollectionResponse principalCollection = graphClient.servicePrincipals().get();
            String resourceId = null;
            UUID roleId = null;
            for (ServicePrincipal servicePrincipal : principalCollection.getValue()) {
                if (application.equals(servicePrincipal.getDisplayName())) {
                    for (AppRole appRole : servicePrincipal.getAppRoles()) {
                        if (role.equals(appRole.getDisplayName())) {
                            resourceId = servicePrincipal.getId();
                            roleId = appRole.getId();
                        }
                    }
                }
//                servicePrincipal.getAppRoles().forEach(role -> {System.out.println("role: " + role.getDisplayName()); System.out.println("id: " + role.getId());});
//                System.out.println("pName: " + servicePrincipal.getDisplayName());
//                System.out.println("appId: " + servicePrincipal.getAppId());
            }
            System.out.println("resourceId: " + resourceId);
            System.out.println("roleId: " + roleId);
            if (Objects.nonNull(resourceId) && Objects.nonNull(roleId)) {
                AppRoleAssignment appRoleAssignment = new AppRoleAssignment();
                appRoleAssignment.setPrincipalId(UUID.fromString(user.getId()));
                appRoleAssignment.setResourceId(UUID.fromString(resourceId));
                appRoleAssignment.setAppRoleId(roleId);
                graphClient.users().byUserId(user.getId()).appRoleAssignments().post(appRoleAssignment);
            } else{
                //throw error
            }
        } else {
            //throw error
        }
        return result;
    }

    /**
     * create User at Entra
     *
     * @return {@code User}
     */
    public static User createUser(String username, String email, String password) {

        User user = new User();
        user.setAccountEnabled(true);
        user.setDisplayName(username);
        user.setMail(email);
        LinkedList<ObjectIdentity> identities = new LinkedList<ObjectIdentity>();
        ObjectIdentity objectIdentity = new ObjectIdentity();
        objectIdentity.setSignInType("emailAddress");
        //read from login user
        objectIdentity.setIssuer("mojodevlexternal.onmicrosoft.com");
        //read from login user
        objectIdentity.setIssuerAssignedId(email);
        identities.add(objectIdentity);
        user.setIdentities(identities);
        PasswordProfile passwordProfile = new PasswordProfile();
        passwordProfile.setForceChangePasswordNextSignInWithMfa(true);
        passwordProfile.setPassword(password);
        user.setPasswordProfile(passwordProfile);
        GraphServiceClient graphClient = getGraphClient();
        return graphClient.users().post(user);
    }



    /**
     * Get Authenticated Graph Client for API usage
     *
     * @return Usable and authenticated Graph Client
     */
    private static GraphServiceClient getGraphClient() {
        if (graphClient == null) {

            // Create secret
            final ClientSecretCredential credential = new ClientSecretCredentialBuilder()
                    .clientId(AZURE_CLIENT_ID).tenantId(AZURE_TENANT_ID).clientSecret(AZURE_CLIENT_SECRET).build();

            final String[] scopes = new String[]{"https://graph.microsoft.com/.default"};

            graphClient = new GraphServiceClient(credential, scopes);
        }

        return graphClient;
    }

    public List<Map<String, Object>> getUserAppRolesByUserId(String userId) {
        List<AppRoleAssignment> userAppRoles = getUserAppRoleAssignmentByUserId(userId);
        List<Map<String, Object>> roleDetails = new ArrayList<>();

        for (AppRoleAssignment appRole : userAppRoles) {
            ServicePrincipal servicePrincipal = getGraphClient()
                    .servicePrincipals()
                    .byServicePrincipalId(String.valueOf(appRole.getResourceId()))
                    .get();

            Map<String, Object> roleInfo = new HashMap<>();
            roleInfo.put("appId", appRole.getResourceId());

            if (servicePrincipal == null) {
                roleInfo.put("appName", "UNKNOWN");
                roleInfo.put("roleName", "UNKNOWN");
            } else {
                roleInfo.put("appName", servicePrincipal.getDisplayName());

                // Find the actual role name based on appRoleId
                String roleName = servicePrincipal.getAppRoles().stream()
                        .filter(role -> role.getId().equals(appRole.getAppRoleId()))
                        .map(AppRole::getDisplayName)
                        .findFirst()
                        .orElse("UNKNOWN");

                roleInfo.put("roleName", roleName);
            }
            roleDetails.add(roleInfo);
        }
        return roleDetails;
    }

    /**
     * Returns all Users from Entra
     * <p>
     * Limitations - only returns 100 users currently
     * </p>
     *
     * @return {@code List<User>}
     */
    public List<User> getAllUsers() {
        UserCollectionResponse response = getGraphClient().users().get();
        return response != null ? response.getValue() : Collections.emptyList();
    }

    public List<DirectoryRole> getDirectoryRolesByUserId(String userId) {
        return Objects.requireNonNull(getGraphClient().users().byUserId(userId).memberOf().get())
                .getValue()
                .stream()
                .filter(obj -> obj instanceof DirectoryRole)
                .map(obj -> (DirectoryRole) obj)
                .collect(Collectors.toList());
    }

    /**
     * Get App Role Assignments to User by User ID
     *
     * @param userId {@code String}
     * @return {@code List<AppRoleAssignment}
     */
    public List<AppRoleAssignment> getUserAppRoleAssignmentByUserId(String userId) {
        try {
            return getGraphClient()
                    .users()
                    .byUserId(userId)
                    .appRoleAssignments()
                    .get()
                    .getValue();
        } catch (ApiException e) {
            System.err.println("Error fetching roles: " + e.getMessage());
            return List.of();
        }
    }

    public void assignAppRoleToUser(String userId, String appId, String appRoleId) {
        AppRoleAssignment appRoleAssignment = new AppRoleAssignment();
        appRoleAssignment.setPrincipalId(UUID.fromString(userId));
        appRoleAssignment.setResourceId(UUID.fromString(appId));
        appRoleAssignment.setAppRoleId(UUID.fromString(appRoleId));

        try {
            getGraphClient()
                    .users()
                    .byUserId(userId)
                    .appRoleAssignments()
                    .post(appRoleAssignment);

            System.out.println("App role successfully assigned to user.");
        } catch (Exception e) {
            System.err.println("Failed to assign app role: " + e.getMessage());
        }
    }

    public void removeAppRoleFromUser(String userId, String appRoleAssignmentId) {
        try {
            getGraphClient()
                    .users()
                    .byUserId(userId)
                    .appRoleAssignments()
                    .byAppRoleAssignmentId(appRoleAssignmentId)
                    .delete();

            System.out.println("App role successfully removed from user.");
        } catch (Exception e) {
            System.err.println("Failed to remove app role: " + e.getMessage());
        }
    }
}
