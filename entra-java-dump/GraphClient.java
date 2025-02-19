package entrajava;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.graph.models.User;
import com.microsoft.graph.models.Group;
import com.microsoft.graph.models.ServicePrincipal;
import com.microsoft.graph.models.DirectoryRole;
import com.microsoft.graph.models.AppRole;
import com.microsoft.graph.models.AppRoleAssignment;
import com.microsoft.graph.models.Application;

import io.github.cdimascio.dotenv.Dotenv;
import kotlin.NotImplementedError;

import java.util.List;
import java.security.Provider.Service;
import java.util.ArrayList;
import com.microsoft.kiota.ApiException;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * GRADLE
 * // Entra
 * implementation 'com.microsoft.graph:microsoft-graph:6.28.0'
 * implementation 'com.microsoft.azure:msal4j:1.13.8'
 * implementation 'com.azure:azure-identity:1.15.0'
 * 
 * // Dotenv
 * implementation 'io.github.cdimascio:java-dotenv:5.2.2'
 * 
 * // Testing
 * testImplementation 'org.junit.jupiter:junit-jupiter:5.8.2'
 * testImplementation 'org.mockito:mockito-core:4.5.1'
 * testRuntimeOnly 'org.junit.jupiter:junit-jupiter-engine:5.8.2'
 */

public class GraphClient {

    private static GraphServiceClient graphClient;

    /**
     * Get Authenticated Graph Client for API usage
     * 
     * @return Usable and authenticated Graph Client
     */
    public static GraphServiceClient getGraphClient() {

        // Create Graph Client if not already
        if (graphClient == null) {
            // Load environment variables from .env
            Dotenv dotenv = Dotenv.load();
            String tenantId = dotenv.get("AZURE_TENANT_ID");
            String clientId = dotenv.get("AZURE_CLIENT_ID");
            String clientSecret = dotenv.get("AZURE_CLIENT_SECRET");

            // Create secret
            final ClientSecretCredential credential = new ClientSecretCredentialBuilder()
                    .clientId(clientId).tenantId(tenantId).clientSecret(clientSecret).build();

            // Load default scopes
            final String[] scopes = new String[] { "https://graph.microsoft.com/.default" };

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
    public static List<User> getAllUsers() {
        var users = getGraphClient().users().get().getValue();

        return users;
    }

    /**
     * Get a user with their user_id
     * <p>
     * Use .isPresent() on returned object to ensure user is found
     * </p>
     * 
     * @param user_id {@code String} User ID in Entra
     * @return {@code Optional<User>}
     */
    public static Optional<User> getUserById(String user_id) {
        try {
            return Optional.ofNullable(getGraphClient().users().byUserId(user_id).get());
        } catch (ApiException e) {
            if (e.getResponseStatusCode() == 404) { // User not found
                System.out.println("User not found: " + user_id);
                return Optional.empty();
            } else {
                System.err.println("Unexpected error: " + e.getMessage());
                throw e; // Rethrow for other errors
            }
        }
    }

    /**
     * Returns all Groups from Entra
     * <p>
     * Limitations - only returns 100 Groups currently
     * </p>
     * 
     * @return {@code List<Group>}
     */
    public static List<Group> getAllGroups() {
        var groups = getGraphClient().groups().get().getValue();

        return groups;
    }

    /**
     * Get a group with it's group ID
     * <p>
     * Use .isPresent() on returned object to ensure group is found
     * </p>
     * 
     * @param group_id {@code String} Group ID in Entra
     * @return {@code Optional<Group>}
     */
    public static Optional<Group> getGroupById(String group_id) {
        try {
            return Optional.ofNullable(getGraphClient().groups().byGroupId(group_id).get());
        } catch (ApiException e) {
            if (e.getResponseStatusCode() == 404) { // Group not found
                System.out.println("Group not found: " + group_id);
                return Optional.empty();
            } else {
                System.err.println("Unexpected error: " + e.getMessage());
                throw e; // Rethrow for other errors
            }
        }
    }

    /**
     * Return Members of a Group
     * 
     * @param group {@code Group}
     * @return {@code List<User>}
     */
    public static List<User> getMembersOfGroup(Group group) {
        return getMembersOfGroup(group.getId());
    }

    /**
     * Return Members of a Group by ID
     * 
     * @param group_id {@code String}
     * @return {@code List<User>}
     */
    public static List<User> getMembersOfGroup(String group_id) {
        try {
            return getGraphClient()
                    .groups()
                    .byGroupId(group_id)
                    .members()
                    .get()
                    .getValue()
                    .stream()
                    .filter(obj -> obj instanceof User) // Filter to include only users
                    .map(obj -> (User) obj) // Cast to User
                    .collect(Collectors.toList()); // Collect into a List
        } catch (ApiException e) {
            System.err.println("Error fetching group members: " + e.getMessage());
            return List.of(); // Return an empty list if there's an error
        }
    }

    /**
     * Get Groups attached to a user
     * 
     * @param user {@code User}
     * @return {@code List<Group>}
     */
    public static List<Group> getUsersGroup(User user) {
        return getUsersGroupByUserId(user.getId());
    }

    /**
     * Get Groups attached to a user
     * 
     * @param user_id {@code String}
     * @return {@code List<Group>}
     */
    public static List<Group> getUsersGroupByUserId(String user_id) {
        try {
            return getGraphClient().users().byUserId(user_id).memberOf().get().getValue().stream()
                    .filter(obj -> obj instanceof Group).map(obj -> (Group) obj).collect(Collectors.toList());
        } catch (ApiException e) {
            System.err.println("Error fetching groups: " + e.getMessage());
            return List.of(); // Return empty list if error occurs
        }
    }

    // List Directory Roles
    public static List<DirectoryRole> getAllDirectoryRoles() {
        throw new UnsupportedOperationException("Feature incomplete. Contact Ben / implement it");
    }

    // List Directory Roles Assigned to User
    public static List<DirectoryRole> getDirectoryRolesByUser(User user) {
        return getDirectoryRolesByUser(user.getId());
    }

    // List Directory Roles Assigned to User by ID
    public static List<DirectoryRole> getDirectoryRolesByUser(String user_id) {
        throw new UnsupportedOperationException("Feature incomplete. Contact Ben / implement it");
    }

    /**
     * Get App Role Assignments to User by User ID
     * 
     * @param user_id {@code String}
     * @return {@code List<AppRoleAssignment}
     */
    public static List<AppRoleAssignment> getUserAppRoles(String user_id) {
        try {
            return getGraphClient()
                    .users()
                    .byUserId(user_id)
                    .appRoleAssignments()
                    .get()
                    .getValue();
        } catch (ApiException e) {
            System.err.println("Error fetching roles: " + e.getMessage());
            return List.of(); // Return empty list on error
        }
    }

    /**
     * Get App Role Assignments to User
     * 
     * @param user_id {@code User}
     * @return {@code List<AppRoleAssignment}
     */
    public static List<AppRoleAssignment> getUserAppRoles(User user) {
        try {
            return getGraphClient()
                    .users()
                    .byUserId(user.getId())
                    .appRoleAssignments()
                    .get()
                    .getValue();
        } catch (ApiException e) {
            System.err.println("Error fetching roles: " + e.getMessage());
            return List.of(); // Return empty list on error
        }
    }

    /**
     * Get App Role Assignments assigned to Group by ID
     * 
     * @param groupId {@code String}
     * @return {@code List<AppRoleAssignment>}
     */
    public static List<AppRoleAssignment> getGroupAppRoles(String groupId) {
        try {
            return getGraphClient()
                    .groups()
                    .byGroupId(groupId)
                    .appRoleAssignments()
                    .get()
                    .getValue();
        } catch (ApiException e) {
            System.err.println("Error fetching group app roles: " + e.getMessage());
            return List.of(); // Return empty list on error
        }
    }

    /**
     * Get App Role Assignments assigned to Group
     * 
     * @param group {@code Group}
     * @return {@code List<AppRoleAssignment>}
     */
    public static List<AppRoleAssignment> getGroupAppRoles(Group group) {
        return getGroupAppRoles(group.getId());
    }

    /**
     * Get App Role Assignments by App ID
     * 
     * @param appId {@code String}
     * @return {@code List<AppRoleAssignment>}
     */
    public static List<AppRoleAssignment> getAppRoleAssignmentsByApp(String appId) {
        try {
            // Fetch app role assignments for the given service principal (app)
            return getGraphClient()
                    .servicePrincipals() // Access the service principal (app)
                    .byServicePrincipalId(appId) // Provide the application ID
                    .appRoleAssignedTo() // Get app role assignments
                    .get() // Execute the request
                    .getValue(); // Extract the result list
        } catch (ApiException e) {
            System.err.println("Error fetching app role assignments: " + e.getMessage());
            return List.of(); // Return an empty list if an error occurs
        }
    }

    /**
     * Get App Role Assignments by App
     * 
     * @param app {@code ServicePrincipal}
     * @return {@code List<AppRoleAssignment}
     */
    public static List<AppRoleAssignment> getAppRoleAssignmentsByApp(ServicePrincipal app) {
        return getAppRoleAssignmentsByApp(app.getId());
    }

    /**
     * Get Users by App ID
     * 
     * @param appId {@code String} Enterprise Application ID
     * @return {@code List<User>}
     */
    public static List<User> getUsersByApp(String appId) {
        try {
            // Get app role assignments for the given service principal (app)
            List<AppRoleAssignment> assignments = getAppRoleAssignmentsByApp(appId);

            // Get user IDs
            List<UUID> userIds = assignments.stream().map(AppRoleAssignment::getPrincipalId)
                    .collect(Collectors.toList());

            List<User> users = new ArrayList<>();

            for (UUID id : userIds) {
                var user = getUserById(id.toString());

                if (user.isPresent()) {
                    users.add(user.get());
                }

            }

            return users;

        } catch (ApiException e) {
            System.err.println("Error fetching users assigned to app: " + e.getMessage());
            return List.of(); // Return empty list if an error occurs
        }
    }

    /**
     * Get Users by App
     * 
     * @param app {@code SerivcePrincipal} Enterprise Application
     * @return {@code List<User>}
     */
    public static List<User> getUsersByApp(ServicePrincipal app) {
        return getUsersByApp(app.getAppId());
    }

    /**
     * Get Groups by App ID
     * <p>
     * This is NOT currently tested due to entra plan limitations
     * </p
     * 
     * @param appId {@code String} Enterprise Application ID
     * @return {@code List<Group>}
     */
    public static List<Group> getGroupsByApp(String appId) {
        try {
            // Get app role assignments for the given service principal (app)
            List<AppRoleAssignment> assignments = getAppRoleAssignmentsByApp(appId);

            // Get group IDs (filtering out users)
            List<UUID> groupIds = assignments.stream()
                    .filter(assignment -> "Group".equalsIgnoreCase(assignment.getPrincipalType())) // Ensure only groups
                    .map(AppRoleAssignment::getPrincipalId)
                    .collect(Collectors.toList());

            List<Group> groups = new ArrayList<>();

            for (UUID id : groupIds) {
                Optional<Group> group = getGroupById(id.toString()); // Fetch group by ID

                group.ifPresent(groups::add); // Add to list if present
            }

            return groups;

        } catch (ApiException e) {
            System.err.println("Error fetching groups assigned to app: " + e.getMessage());
            return List.of(); // Return empty list if an error occurs
        }
    }

    /**
     * Get Groups by App
     * <p>
     * This is NOT currently tested due to entra plan limitations
     * </p
     * 
     * @param app {@code ServicePrincipal} Enterprise Application
     * @return {@code List<Group>}
     */
    public static List<Group> getGroupsByApp(ServicePrincipal app) {
        return getGroupsByApp(app.getAppId());
    }

    /**
     * Get App Roles Defined by an Application
     *
     * @param appId {@code String} Application ID (NOT the Service Principal ID)
     * @return {@code List<AppRole>}
     */
    public static List<AppRole> getAppRolesByApplication(String appId) {
        try {
            // Get the application object (App Roles are stored in the Application)
            Application application = getGraphClient().applications().byApplicationId(appId).get();

            return application.getAppRoles();
        } catch (ApiException e) {
            System.err.println("Error fetching app roles: " + e.getMessage());
            return List.of(); // Return empty list if an error occurs
        }
    }

    /**
     * Get App Roles Defined by an Application
     *
     * @param app {@code Application} Application (NOT the Service Principal)
     * @return {@code List<AppRole>}
     */
    public static List<AppRole> getAppRolesByApplication(Application app) {
        return getAppRolesByApplication(app.getAppId());
    }
}
