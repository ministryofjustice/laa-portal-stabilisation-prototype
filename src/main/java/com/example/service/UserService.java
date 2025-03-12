package com.example.service;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.graph.models.Invitation;
import com.microsoft.graph.models.PasswordProfile;
import com.microsoft.graph.models.User;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * userService
 */
@Service
public class UserService {

    private static final String AZURE_CLIENT_ID = System.getenv("AZURE_CLIENT_ID");
    private static final String AZURE_TENANT_ID = System.getenv("AZURE_TENANT_ID");
    private static final String AZURE_CLIENT_SECRET = System.getenv("AZURE_CLIENT_SECRET");
    private static GraphServiceClient graphClient;

    Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * create User at Entra
     *
     * @return {@code User}
     */
    public static Invitation inviteUser(String email) {
        GraphServiceClient graphClient = getGraphClient();

        Invitation invitation = new Invitation();
        invitation.setInvitedUserEmailAddress(email);
        invitation.setInviteRedirectUrl("http://localhost:8080");
        invitation.setSendInvitationMessage(true);
        return  graphClient.invitations().post(invitation);
    }

    /**
     * create User at Entra
     *
     * @return {@code User}
     */
    public static User createUser(String username, String password) {
        GraphServiceClient graphClient = getGraphClient();

        User user = new User();
        user.setAccountEnabled(true);
        user.setDisplayName(username);
        user.setMailNickname("someone");
        user.setUserPrincipalName(username+"@mojodevlexternal.onmicrosoft.com");
        PasswordProfile passwordProfile = new PasswordProfile();
        passwordProfile.setForceChangePasswordNextSignIn(true);
        passwordProfile.setPassword(password);
        user.setPasswordProfile(passwordProfile);
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

}
