package com.example.controller;

import com.example.model.PaginatedUsers;
import com.example.service.UserService;
import com.microsoft.graph.models.Invitation;
import com.microsoft.graph.models.User;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Stack;

/**
 * User Controller
 */
@Controller
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Add new user via Microsoft Graph API.
     */
    @PostMapping("/register")
    public User addUserToGraph(@RequestParam("username") String username,
                               @RequestParam("password") String password) {
        return UserService.createUser(username, password);
    }

    /**
     * Retrieves a list of users from Microsoft Graph API.
     */
    @GetMapping("/register")
    public String register() {
        return "register";
    }

    /**
     * invite new user via Microsoft Graph API.
     */
    @PostMapping("/invite")
    public Invitation invite(@RequestParam("email") String email, Model model) {
        Invitation result = UserService.inviteUser(email);
        model.addAttribute("redeemUrl", result.getInviteRedeemUrl());
        return result;
    }

    /**
     * Retrieves a list of users from Microsoft Graph API.
     */
    @GetMapping("/invite")
    public String inviteUserToGraph() {
        return "invite";
    }

    /**
     * Retrieves a list of users from Microsoft Graph API.
     */
    @GetMapping("/users")
    public String displayAllUsers(@RequestParam(defaultValue = "10") int size,
                                  @RequestParam(required = false) String nextPageLink,
                                  Model model, HttpSession session) {
        
        Stack<String> pageHistory = userService.getPageHistory(session);

        PaginatedUsers paginatedUsers = userService.getPaginatedUsersWithHistory(pageHistory, size, nextPageLink);

        model.addAttribute("users", paginatedUsers.getUsers());
        model.addAttribute("nextPageLink", paginatedUsers.getNextPageLink());
        model.addAttribute("previousPageLink", paginatedUsers.getPreviousPageLink());
        model.addAttribute("pageSize", size);
        model.addAttribute("pageHistory", pageHistory);

        return "users";
    }

    @GetMapping("/users/edit/{id}")
    public String editUser(@PathVariable String id, Model model) {
        User user = userService.getUserById(id);
        model.addAttribute("user", user);
        return "edit-user";
    }
}
