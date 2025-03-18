package com.example.service;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.graph.models.*;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import org.springframework.stereotype.Service;

import java.util.LinkedList;

@Service
public class EmailService {

    private static final String AZURE_CLIENT_ID = System.getenv("AZURE_CLIENT_ID");
    private static final String AZURE_TENANT_ID = System.getenv("AZURE_TENANT_ID");
    private static final String AZURE_CLIENT_SECRET = System.getenv("AZURE_CLIENT_SECRET");
    private static GraphServiceClient graphClient;

    /**
     * send main via Graph api
     *
     */
    public static void sendMail(String email, String subject, String content) {
        com.microsoft.graph.users.item.sendmail.SendMailPostRequestBody sendMailPostRequestBody = new com.microsoft.graph.users.item.sendmail.SendMailPostRequestBody();
        Message message = new Message();
        message.setSubject(subject);
        ItemBody body = new ItemBody();
        body.setContentType(BodyType.Html);
        body.setContent(content);
        message.setBody(body);
        LinkedList<Recipient> toRecipients = new LinkedList<Recipient>();
        Recipient recipient = new Recipient();
        EmailAddress emailAddress = new EmailAddress();
        emailAddress.setAddress(email);
        recipient.setEmailAddress(emailAddress);
        toRecipients.add(recipient);
        message.setToRecipients(toRecipients);
        sendMailPostRequestBody.setMessage(message);
        sendMailPostRequestBody.setSaveToSentItems(false);
        GraphServiceClient graphClient = getGraphClient();
        graphClient.me().sendMail().post(sendMailPostRequestBody);
    }

    public static String getWelcomeMessage(String userName, String password) {
        String welcomeMessage = "<p>Welcome " + userName + "!</p>";
        welcomeMessage += "<p> Your password is " + password + "</p>";
        return welcomeMessage;
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
