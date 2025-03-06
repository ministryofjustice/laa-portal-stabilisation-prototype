package com.example.controller;

import com.example.model.User;
import com.example.model.UserSessionData;
import com.example.service.LoginService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller for handling login-related requests.
 */
@Controller
public class LoginController {

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);
    private final LoginService loginService;

    public LoginController(LoginService loginService) {
        this.loginService = loginService;
    }

    @GetMapping("/")
    public String login(Model model) {
        model.addAttribute("user", new User());
        return "index";
    }

    /**
     * @param email input by user
     * @return home view if successful, else login view
     */
    @PostMapping("/login")
    public RedirectView handleLogin(@RequestParam("email") String email, RedirectAttributes redirectAttributes) {
        if (email == null || email.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                "An incorrect Username or Password was specified");
            return new RedirectView("/");
        }
        try {
            String azureLoginUrl = loginService.buildAzureLoginUrl(email);
            return new RedirectView(azureLoginUrl);
        } catch (Exception e) {
            logger.error("Error logging in: {}", e.getMessage());
            return new RedirectView("/");
        }
    }

    /**
     * Handles GET requests to the "/home" endpoint.
     *
     * @param model          the model to be populated with user session data
     * @param authentication the authentication object containing user credentials
     * @param session        the current HTTP session
     * @param authClient     the OAuth2 authorized client for Azure
     * @return the view for home
     */
    @GetMapping("/home")
    public String home(Model model, Authentication authentication, HttpSession session, @RegisteredOAuth2AuthorizedClient("azure") OAuth2AuthorizedClient authClient) {
        try {
            UserSessionData userSessionData = loginService.processUserSession(authentication, authClient, session);

            if (userSessionData != null) {
                model.addAttribute("name", userSessionData.getName());
                model.addAttribute("appRoleAssignments", userSessionData.getAppRoleAssignments());
                model.addAttribute("appRole", userSessionData.getUserAppRoles());
                model.addAttribute("user", userSessionData.getUser());
                model.addAttribute("lastLogin", userSessionData.getLastLogin());
            } else {
                logger.info("No access token found");
            }
        } catch (Exception e) {
            logger.error("Error getting user list: {}", e.getMessage());
        }
        return "home";
    }

    @GetMapping("/migrate")
    public String migrate() {
        return "migrate";
    }
}
