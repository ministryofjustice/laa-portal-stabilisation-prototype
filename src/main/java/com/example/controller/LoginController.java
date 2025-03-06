package com.example.controller;

import com.example.model.User;
import com.example.model.UserSessionData;
import com.example.service.LoginService;
import com.microsoft.graph.models.AppRole;
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

  @PostMapping("/login")
  public RedirectView handleLogin(@RequestParam("email") String email, RedirectAttributes redirectAttributes) {
    if (email == null || email.trim().isEmpty()) {
      redirectAttributes.addFlashAttribute("errorMessage", "An incorrect Username or Password was specified");
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

  @GetMapping("/home")
  public String home(Model model, Authentication authentication, HttpSession session,
                     @RegisteredOAuth2AuthorizedClient("azure") OAuth2AuthorizedClient authorizedClient) {
    try {
      UserSessionData userSessionData = loginService.processUserSession(authentication, authorizedClient, session);

      if (userSessionData != null) {
        model.addAttribute("name", userSessionData.getName());
        model.addAttribute("appRoleAssignments", userSessionData.getAppRoleAssignments());
        for (AppRole role : userSessionData.getUserAppRoles()) {
          logger.info("Display Name: {}", role.getDisplayName());
          logger.info("Description: {}", role.getDescription());
        }
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
