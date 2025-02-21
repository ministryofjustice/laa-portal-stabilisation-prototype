package com.example.controller;

import com.example.model.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Javadoc comment.
 */
@Controller
public class LoginController {
  @GetMapping("/")
  public String login(Model model) {
    model.addAttribute("user", new User());
    return "index";
  }

  /**
   * Javadoc comment.
   */
  @PostMapping("/login")
  public String handleLogin(@ModelAttribute User user, Model model) {
    String validUsername = "user";
    String validPassword = "password";
    System.out.println(user.getUsername());
    System.out.println(user.getPassword());

    if (validUsername.equals(user.getUsername()) && validPassword.equals(user.getPassword())) {
      model.addAttribute("message", "Welcome " + user.getUsername() + "!");
      return "home";
    } else {
      model.addAttribute("error", "Invalid username or password.");
      return "index";
    }
  }

  @GetMapping("/home")
  public String home(Model model, Authentication authentication) {
    if(authentication != null) {
      System.out.println("Authentication object is NOT null");
      OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
      OAuth2User principal = oauthToken.getPrincipal();
      System.out.println(principal);

      String name = principal.getAttribute("name");
      String email = principal.getAttribute("email");

      model.addAttribute("name", name);
      model.addAttribute("email", email);
    } else {
      System.out.println("Authentication object is null");
    }

    return "home";
  }

  @GetMapping("/migrate")
  public String migrate() {
    return "migrate";
  }

}
