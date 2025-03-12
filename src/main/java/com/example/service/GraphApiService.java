package com.example.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.graph.models.AppRole;
import com.microsoft.graph.models.AppRoleAssignment;
import com.microsoft.graph.models.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * service class for graph api.
 */
@Service
public class GraphApiService {

    private static final String GRAPH_URL = "https://graph.microsoft.com/v1.0";

    Logger logger = LoggerFactory.getLogger(this.getClass());

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
            String jsonResponse = callGraphApi(accessToken, url);
            ObjectMapper objectMapper = new ObjectMapper();

            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);


            return objectMapper.readValue(jsonResponse, User.class);
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
    public LocalDateTime getLastSignInTime(String accessToken) {
        String url = GRAPH_URL + "/me?$select=signInActivity";

        String jsonResponse = callGraphApi(accessToken, url);
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode signInActivity = root.path("signInActivity");

            if (!signInActivity.isMissingNode()) {
                String lastSignInString = signInActivity.path("lastSignInDateTime").asText(null);
                if (lastSignInString != null) {
                    return LocalDateTime.parse(lastSignInString, DateTimeFormatter.ISO_DATE_TIME);
                }
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
