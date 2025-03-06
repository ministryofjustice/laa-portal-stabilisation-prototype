package com.example.service;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.graph.models.*;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;


@Service
public class GraphApiService {

    private static final String AZURE_CLIENT_ID = System.getenv("AZURE_CLIENT_ID");
    private static final String AZURE_TENANT_ID = System.getenv("AZURE_TENANT_ID");
    private static final String AZURE_CLIENT_SECRET = System.getenv("AZURE_CLIENT_SECRET");
    private static final String GRAPH_URL = "https://graph.microsoft.com/v1.0";
    private static GraphServiceClient graphClient;

    Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * create User at Entra
     *
     * @return {@code User}
     */
    public static User createUser(String username, String password) {
        GraphServiceClient graphClient = getGraphClient();

        User user = new User();
        user.setAccountEnabled(true);
        //Another object with the same value for property userPrincipalName already exists.
        user.setDisplayName(username);
        //less than 65 characters and do not contain spaces
        user.setMailNickname("someone");
        user.setUserPrincipalName(username+"@mojodevlexternal.onmicrosoft.com");
        PasswordProfile passwordProfile = new PasswordProfile();
        passwordProfile.setForceChangePasswordNextSignIn(true);
        //The specified password does not comply with password complexity requirements. Please provide a different password.
        passwordProfile.setPassword(password);
        user.setPasswordProfile(passwordProfile);
        return graphClient.users().post(user);
    }

    /**
     * Returns all Users from Entra
     *
     * @return {@code List<User>}
     */
    public static UserCollectionResponse getUsers() {
        GraphServiceClient graphClient = getGraphClient();

        return graphClient.users().get();
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
     * Get App Role Assignments of User
     *
     * @param accessToken The OAuth2 access token required to authenticate the request.
     * @return {@code List<AppRoleAssignment}
     */
    public List<AppRoleAssignment> getAppRoleAssignments(String accessToken) {
        String url = GRAPH_URL + "/me/appRoleAssignments";

        String jsonResponse = callGraphAPI(accessToken, url);

        List<AppRoleAssignment> appRoleAssignments = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode values = root.path("value");

            for (JsonNode node : values) {
                AppRoleAssignment appRoleAssignment = new AppRoleAssignment();
                appRoleAssignment.setId(node.path("resourceId").asText());
                appRoleAssignment.setResourceDisplayName(node.path("resourceDisplayName").asText());

                appRoleAssignments.add(appRoleAssignment);
            }
        } catch (Exception e) {
            logger.error("Unexpected error processing app role assignments", e);
        }

        return appRoleAssignments;
    }

    public String getUserProfile(String accessToken) {
        String url = GRAPH_URL + "/v1.0/me";

        return callGraphAPI(accessToken, url);
    }

    /**
     * Get groups and roles assigned to a User
     *
     * @param accessToken The OAuth2 access token required to authenticate the request.
     * @return A list of {@link AppRole} objects
     */
    public List<AppRole> getUserAssignedApps(String accessToken) {
        String url = "https://graph.microsoft.com/v1.0/me/memberOf";

        String jsonResponse = callGraphAPI(accessToken, url);

        List<AppRole> appRoles = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode values = root.path("value");

            for (JsonNode node : values) {
                AppRole appRole = new AppRole();
                appRole.setDisplayName(node.path("displayName").asText());
                appRole.setDescription(node.path("description").asText());

                appRoles.add(appRole);
            }
        } catch (Exception e) {
            logger.error("Unexpected error processing app role assignments", e);
        }

        return appRoles;
    }

    private String callGraphAPI(String accessToken, String url) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.set("Accept", "application/json");

        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

        return response.getBody();
    }
}
