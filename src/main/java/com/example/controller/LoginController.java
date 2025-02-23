package com.example.controller;

import com.example.model.User;
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

  @GetMapping("/login")
  public String login(Model model) {
    model.addAttribute("user", new User());
    return "login";
  }

  /**
   * Javadoc comment.
   */
  @PostMapping("/login")
  public String handleLogin(@ModelAttribute User user, Model model) {
    String validUsername = "user";
    String validPassword = "password";

    if (validUsername.equals(user.getUsername()) && validPassword.equals(user.getPassword())) {
      model.addAttribute("message", "Welcome " + user.getUsername() + "!");
      return "home";
    } else {
      model.addAttribute("error", "Invalid username or password.");
      return "login";
    }
  }

  @GetMapping("/home")
  public String home() {
    return "home";
  }

}
