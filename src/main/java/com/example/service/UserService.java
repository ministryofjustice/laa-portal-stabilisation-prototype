package com.example.service;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.graph.models.*;
import com.example.model.PaginatedUsers;
import com.example.model.UserModel;
import com.example.model.UserRole;
import com.microsoft.graph.models.AppRole;
import com.microsoft.graph.models.AppRoleAssignment;
import com.microsoft.graph.models.DirectoryRole;
import com.microsoft.graph.models.Invitation;
import com.microsoft.graph.models.InvitedUserMessageInfo;
import com.microsoft.graph.models.ObjectIdentity;
import com.microsoft.graph.models.PasswordProfile;
import com.microsoft.graph.models.ServicePrincipal;
import com.microsoft.graph.models.ServicePrincipalCollectionResponse;
import com.microsoft.graph.models.User;
import com.microsoft.graph.models.UserCollectionResponse;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.kiota.ApiException;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;
import java.util.UUID;

/**
 * userService
 */
@Service
public class UserService {

    private static final String AZURE_CLIENT_ID = System.getenv("AZURE_CLIENT_ID");
    private static final String AZURE_TENANT_ID = System.getenv("AZURE_TENANT_ID");
    private static final String AZURE_CLIENT_SECRET = System.getenv("AZURE_CLIENT_SECRET");
    private static final String APPLICATION_ID = "0ca5b38b-6c4f-404e-b1d0-d0e8d4e0bfd5";
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
        graphClient = getGraphClient();
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
                //servicePrincipal.getAppRoles().forEach(role -> {System.out.println("role: " + role.getDisplayName()); System.out.println("id: " + role.getId());});
                //System.out.println("pName: " + servicePrincipal.getDisplayName());
                //System.out.println("appId: " + servicePrincipal.getAppId());
            }
            System.out.println("resourceId: " + resourceId);
            System.out.println("roleId: " + roleId);
            if (Objects.nonNull(resourceId) && Objects.nonNull(roleId)) {
                AppRoleAssignment appRoleAssignment = new AppRoleAssignment();
                appRoleAssignment.setPrincipalId(UUID.fromString(user.getId()));
                appRoleAssignment.setResourceId(UUID.fromString(resourceId));
                appRoleAssignment.setAppRoleId(roleId);
                graphClient.users().byUserId(user.getId()).appRoleAssignments().post(appRoleAssignment);
            } else {
                //throw error
                System.out.println("throw error: " + email);
            }
        } else {
            //throw error
            System.out.println("throw error: " + email);
        }
        return result;
    }

    /**
     * create User at Entra
     *
     * @return {@code User}
     */
    public User createUser(String username, String email, String password, String application, String role, String office) {

        User user = new User();
        user.setAccountEnabled(true);
        user.setDisplayName(username);
        user.setMail(email);
        user.setOfficeLocation(office);
        ObjectIdentity objectIdentity = new ObjectIdentity();
        objectIdentity.setSignInType("emailAddress");
        //read from login user
        objectIdentity.setIssuer("mojodevlexternal.onmicrosoft.com");
        //read from login user
        objectIdentity.setIssuerAssignedId(email);
        LinkedList<ObjectIdentity> identities = new LinkedList<ObjectIdentity>();
        identities.add(objectIdentity);
        user.setIdentities(identities);
        PasswordProfile passwordProfile = new PasswordProfile();
        passwordProfile.setForceChangePasswordNextSignInWithMfa(true);
        passwordProfile.setPassword(password);
        user.setPasswordProfile(passwordProfile);
        GraphServiceClient graphClient = getGraphClient();
        user = graphClient.users().post(user);
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
            //servicePrincipal.getAppRoles().forEach(role -> {System.out.println("role: " + role.getDisplayName()); System.out.println("id: " + role.getId());});
            //System.out.println("pName: " + servicePrincipal.getDisplayName());
            //System.out.println("appId: " + servicePrincipal.getAppId());
        }
        System.out.println("resourceId: " + resourceId);
        System.out.println("roleId: " + roleId);
        if (Objects.nonNull(resourceId) && Objects.nonNull(roleId)) {
            AppRoleAssignment appRoleAssignment = new AppRoleAssignment();
            appRoleAssignment.setPrincipalId(UUID.fromString(user.getId()));
            appRoleAssignment.setResourceId(UUID.fromString(resourceId));
            appRoleAssignment.setAppRoleId(roleId);
            graphClient.users().byUserId(user.getId()).appRoleAssignments().post(appRoleAssignment);
        } else {
            //throw error
            System.out.println("throw error: " + resourceId);
        }
        return user;
    }

    public User createUser(User user, String password, List<String> roles) {

        user.setAccountEnabled(true);
        ObjectIdentity objectIdentity = new ObjectIdentity();
        objectIdentity.setSignInType("emailAddress");
        //read from login user
        objectIdentity.setIssuer("mojodevlexternal.onmicrosoft.com");
        //read from login user
        objectIdentity.setIssuerAssignedId(user.getMail());
        LinkedList<ObjectIdentity> identities = new LinkedList<ObjectIdentity>();
        identities.add(objectIdentity);
        user.setIdentities(identities);
        PasswordProfile passwordProfile = new PasswordProfile();
        passwordProfile.setForceChangePasswordNextSignInWithMfa(true);
        passwordProfile.setPassword(password);
        user.setPasswordProfile(passwordProfile);
        GraphServiceClient graphClient = getGraphClient();
        user = graphClient.users().post(user);

        ServicePrincipalCollectionResponse principalCollection = graphClient.servicePrincipals().get();
        String resourceId = null;
        UUID roleId = null;
        for (ServicePrincipal servicePrincipal : principalCollection.getValue()) {
            for (AppRole appRole : servicePrincipal.getAppRoles()) {
                if (roles.contains(appRole.getId().toString())) {
                    resourceId = servicePrincipal.getId();
                    roleId = appRole.getId();
                    assignAppRoleToUser(user.getId(), resourceId, roleId.toString());
                }
            }
        }
        return user;
    }

    public List<ServicePrincipal> getServicePrincipals() {
        GraphServiceClient graphClient = getGraphClient();
        return Objects.requireNonNull(graphClient.servicePrincipals().get()).getValue();
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

    @SuppressWarnings("unchecked")
    public Stack<String> getPageHistory(HttpSession session) {
        Stack<String> pageHistory = (Stack<String>) session.getAttribute("pageHistory");
        if (pageHistory == null) {
            pageHistory = new Stack<>();
            session.setAttribute("pageHistory", pageHistory);
        }
        return pageHistory;
    }

    // Retrieves paginated users and manages the page history for next and previous navigation
    // Not working as expected for previous page stream
    public PaginatedUsers getPaginatedUsersWithHistory(Stack<String> pageHistory, int size, String nextPageLink) {
        String previousPageLink = null;

        if (nextPageLink != null) {
            if (!pageHistory.isEmpty()) {
                previousPageLink = pageHistory.pop();
            }
            pageHistory.push(nextPageLink);
        }

        PaginatedUsers paginatedUsers = getAllUsersPaginated(size, nextPageLink, previousPageLink);
        paginatedUsers.setPreviousPageLink(previousPageLink);

        return paginatedUsers;
    }


    public List<DirectoryRole> getDirectoryRolesByUserId(String userId) {
        return Objects.requireNonNull(getGraphClient().users().byUserId(userId).memberOf().get())
                .getValue()
                .stream()
                .filter(obj -> obj instanceof DirectoryRole)
                .map(obj -> (DirectoryRole) obj)
                .collect(Collectors.toList());
    }

    public List<UserRole> getUserAppRolesByUserId(String userId) {
        List<AppRoleAssignment> userAppRoles = getUserAppRoleAssignmentByUserId(userId);
        List<UserRole> userRoles = new ArrayList<>();
        UserRole userRole = new UserRole();

        for (AppRoleAssignment appRole : userAppRoles) {
            ServicePrincipal servicePrincipal = getGraphClient()
                    .servicePrincipals()
                    .byServicePrincipalId(Objects.requireNonNull(appRole.getResourceId()).toString())
                    .get();

            if (servicePrincipal != null) {
                userRole.setAppId(String.valueOf(appRole.getResourceId()));
                userRole.setAppRoleId(String.valueOf(appRole.getAppRoleId()));
                userRole.setAssignmentId(appRole.getId());
                userRole.setAppName(servicePrincipal.getDisplayName());

                String roleName = Objects.requireNonNull(servicePrincipal.getAppRoles()).stream()
                        .filter(role -> Objects.equals(role.getId(), appRole.getAppRoleId()))
                        .map(AppRole::getDisplayName)
                        .findFirst()
                        .orElse("UNKNOWN");

                userRole.setRoleName(roleName);
            } else {
                userRole.setAppName("UNKNOWN");
                userRole.setRoleName("UNKNOWN");
            }

            userRoles.add(userRole);
        }

        return userRoles;
    }

    /**
     * Get App Role Assignments to User by User ID
     *
     * @param userId {@code String}
     * @return {@code List<AppRoleAssignment}
     */
    public List<AppRoleAssignment> getUserAppRoleAssignmentByUserId(String userId) {
        try {
            return Objects.requireNonNull(getGraphClient()
                            .users()
                            .byUserId(userId)
                            .appRoleAssignments()
                            .get())
                    .getValue();
        } catch (ApiException e) {
            logger.error("Error fetching roles: {}", e.getMessage());
            return List.of();
        }
    }

    public void updateUserRoles(String userId, List<String> selectedRoles) {
        List<UserRole> existingRoles = getUserAppRolesByUserId(userId);

        Set<String> currentRoleIds = existingRoles.stream()
                .map(UserRole::getAppRoleId)
                .collect(Collectors.toSet());

        Set<String> selectedRoleIds = new HashSet<>(selectedRoles != null ? selectedRoles : List.of());

        List<UserRole> availableRoles = getAllAvailableRoles();

        for (String roleId : selectedRoleIds) {
            if (!currentRoleIds.contains(roleId)) {
                String appId = findAppIdForRole(roleId, availableRoles);
                assignAppRoleToUser(userId, appId, roleId);
            }
        }

        for (UserRole role : existingRoles) {
            if (!selectedRoleIds.contains(role.getAppRoleId())) {
                removeAppRoleFromUser(userId, role.getAssignmentId());
            }
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

            logger.info("App role successfully removed from user.");
        } catch (Exception e) {
            logger.error("Failed to remove app role: {}", e.getMessage());
        }
    }

    public User getUserById(String userId) {
        GraphServiceClient graphClient = getGraphClient();

        try {
            return graphClient.users().byUserId(userId).get();
        } catch (Exception e) {
            logger.error("Error fetching user with ID: {}", userId, e);
            return null;
        }
    }

    public String formatLastSignInDateTime(OffsetDateTime dateTime) {
        if (dateTime == null) {
            return "N/A";
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm", Locale.ENGLISH);

        return dateTime.format(formatter);
    }

    public List<UserRole> getAllAvailableRoles() {
        List<ServicePrincipal> servicePrincipals = Objects.requireNonNull(getGraphClient()
                        .servicePrincipals()
                        .get())
                .getValue();

        List<UserRole> roles = new ArrayList<>();
        if (!ObjectUtils.isEmpty(servicePrincipals)) {
            for (ServicePrincipal sp : servicePrincipals) {
                if (Objects.equals(sp.getAppId(), APPLICATION_ID)) {
                    for (AppRole role : Objects.requireNonNull(sp.getAppRoles())) {
                        UserRole roleInfo = new UserRole(
                                sp.getId(),
                                sp.getDisplayName(),
                                Objects.requireNonNull(role.getId()).toString(),
                                role.getDisplayName(),
                                null,
                                role.getDisplayName(),
                                null,
                                false
                        );
                        roles.add(roleInfo);
                    }
                }
            }
        }
        return roles;
    }

    public List<UserRole> getAllAvailableRolesForApps(List<String> selectedApps) {
        List<ServicePrincipal> servicePrincipals = Objects.requireNonNull(getGraphClient()
                        .servicePrincipals()
                        .get())
                .getValue();

        List<UserRole> roles = new ArrayList<>();
        if (!ObjectUtils.isEmpty(servicePrincipals)) {
            for (ServicePrincipal sp : servicePrincipals) {
                if (selectedApps.contains(sp.getAppId())) {
                    for (AppRole role : Objects.requireNonNull(sp.getAppRoles())) {
                        UserRole roleInfo = new UserRole(
                                sp.getId(),
                                sp.getDisplayName(),
                                Objects.requireNonNull(role.getId()).toString(),
                                role.getDisplayName(),
                                null,
                                role.getDisplayName(),
                                null,
                                false
                        );
                        roles.add(roleInfo);
                    }
                }
            }
        }
        return roles;
    }

    private String findAppIdForRole(String roleId, List<UserRole> existingRoles) throws IllegalArgumentException {
        Optional<UserRole> userRole = existingRoles.stream()
                .filter(role -> role.getAppRoleId().equals(roleId))
                .findFirst();

        if (userRole.isPresent()) {
            return userRole.get().getAppId();
        } else {
            throw new IllegalArgumentException("App ID not found for role ID: " + roleId);
        }
    }

    private PaginatedUsers getAllUsersPaginated(int pageSize, String nextPageLink, String previousPageLink) {
        GraphServiceClient graphClient = getGraphClient();
        UserCollectionResponse response;

        if (nextPageLink == null || nextPageLink.isEmpty()) {
            response = graphClient.users()
                    .get(requestConfig -> {
                        assert requestConfig.queryParameters != null;
                        requestConfig.queryParameters.top = pageSize;
                        requestConfig.queryParameters.select = new String[]{"displayName", "userPrincipalName", "signInActivity"};
                        requestConfig.queryParameters.count = true;
                    });
        } else {
            response = graphClient.users()
                    .withUrl(previousPageLink)
                    .get();
        }

        List<User> graphUsers = response != null ? response.getValue() : Collections.emptyList();
        List<UserModel> users = List.of();

        if (graphUsers != null && !graphUsers.isEmpty()) {
            users = graphUsers.stream().map(graphUser -> {
                UserModel user = new UserModel();
                user.setId(graphUser.getId());
                user.setEmail(graphUser.getUserPrincipalName());
                user.setFullName(graphUser.getDisplayName());

                if (graphUser.getSignInActivity() != null) {
                    user.setLastLoggedIn(formatLastSignInDateTime(graphUser.getSignInActivity().getLastSignInDateTime()));
                } else {
                    user.setLastLoggedIn("NA");
                }

                return user;
            }).collect(Collectors.toList());
        }

        PaginatedUsers paginatedUsers = new PaginatedUsers();
        paginatedUsers.setUsers(users);
        paginatedUsers.setNextPageLink(response != null && response.getOdataNextLink() != null ? response.getOdataNextLink() : null);

        int totalUsers = Optional.ofNullable(graphClient.users()
                        .count()
                        .get(requestConfig -> requestConfig.headers.add("ConsistencyLevel", "eventual")))
                .orElse(0);

        paginatedUsers.setTotalUsers(totalUsers);

        return paginatedUsers;
    }
}
