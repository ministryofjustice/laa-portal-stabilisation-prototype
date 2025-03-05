package com.example.service;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.graph.models.AppRole;
import com.microsoft.graph.models.AppRoleAssignment;
import com.microsoft.graph.models.User;
import com.microsoft.graph.models.UserCollectionResponse;
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

/**
 * service class for graph api.
 */
@Service
public class GraphApiService {

    private static final String AZURE_CLIENT_ID = System.getenv("AZURE_CLIENT_ID");
    private static final String AZURE_TENANT_ID = System.getenv("AZURE_TENANT_ID");
    private static final String AZURE_CLIENT_SECRET = System.getenv("AZURE_CLIENT_SECRET");
    private static final String GRAPH_URL = "https://graph.microsoft.com/v1.0";
    private static GraphServiceClient graphClient;

    Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * Returns all Users from Entra.
     *
     * @return {@code List<User>}
     */
    public static UserCollectionResponse getUsers() {
        GraphServiceClient graphClient = getGraphClient();

        return graphClient.users().get();
    }

    /**
     * Get Authenticated Graph Client for API usage.
     *
     * @return Usable and authenticated Graph Client
     */
    private static GraphServiceClient getGraphClient() {
        if (graphClient == null) {

            // Create secret
            final ClientSecretCredential credential = new ClientSecretCredentialBuilder()
                    .clientId(AZURE_CLIENT_ID)
                    .tenantId(AZURE_TENANT_ID)
                    .clientSecret(AZURE_CLIENT_SECRET)
                    .build();

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

        String jsonResponse = callGraphApi(accessToken, url);

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

    public User getUserProfile(String accessToken) {
        String url = GRAPH_URL + "/me";
        try {
            String responseBody = callGraphApi(accessToken, url);
            ObjectMapper objectMapper = new ObjectMapper();

            return objectMapper.readValue(responseBody, User.class);
        } catch (Exception e) {
            logger.error("Unexpected error processing user profile", e);
        }
        return null;
    }

    /**
     * Retrieves the last sign-in time of the authenticated user.
     *
     * @param accessToken The OAuth2 access token required to authenticate the request.
     * @return Last sign-in timestamp as a {@link String}, or {@code null} if not available.
     */
    public String getLastSignInTime(String accessToken) {
        String url = GRAPH_URL + "/me?$select=signInActivity";

        String jsonResponse = callGraphApi(accessToken, url);
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode signInActivity = root.path("signInActivity");

            if (!signInActivity.isMissingNode()) {
                return signInActivity.path("lastSignInDateTime").asText(null);
            }
        } catch (Exception e) {
            logger.error("Unexpected error retrieving last sign-in time", e);
        }

        return null;
    }


    /**
     * Get groups and roles assigned to a User
     *
     * @param accessToken The OAuth2 access token required to authenticate the request.
     * @return A list of {@link AppRole} objects
     */
    public List<AppRole> getUserAssignedApps(String accessToken) {
        String url = "https://graph.microsoft.com/v1.0/me/memberOf";

        String jsonResponse = callGraphApi(accessToken, url);

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

    private String callGraphApi(String accessToken, String url) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.set("Accept", "application/json");

        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, String.class);

        return response.getBody();
    }
}
