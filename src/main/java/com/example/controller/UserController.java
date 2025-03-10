package com.example.controller;

import com.example.service.GraphApiService;
import com.example.service.UserService;
import com.microsoft.graph.models.Invitation;
import com.microsoft.graph.models.User;
import com.microsoft.graph.models.UserCollectionResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class UserController {

    private final UserService userService;
    private final GraphApiService graphApiService;

    public UserController(UserService userService, GraphApiService graphApiService) {
        this.userService = userService;
        this.graphApiService = graphApiService;
    }

    /**
     * Retrieves a list of users from Microsoft Graph API.
     */
    @GetMapping("/users")
    public UserCollectionResponse getUsersFromGraph() throws Exception {
        return GraphApiService.getUsers();
    }

    /**
     * Add new user via Microsoft Graph API.
     */
    @PostMapping("/register")
    public User addUserToGraph(@RequestParam("username") String username,
                               @RequestParam("password") String password) throws Exception {
        User user = GraphApiService.createUser(username, password);
        return user;
    }

    /**
     * Retrieves a list of users from Microsoft Graph API.
     */
    @GetMapping("/register")
    public String register() throws Exception {
        return "register";
    }

    /**
     * invite new user via Microsoft Graph API.
     */
    @PostMapping("/invite")
    public Invitation invite(@RequestParam("email") String email, Model model) throws Exception {
        Invitation result = GraphApiService.inviteUser(email);
        model.addAttribute("redeemUrl", result.getInviteRedeemUrl());
        //The invited user already exists in the directory as object ID: dc8b9eaa-68c0-4781-aa4f-be3a015c90f0, but the account is blocked from signing in. If the account is unblocked, they can use that account to sign in to shared apps and resources.
        return result;
    }

    /**
     * Retrieves a list of users from Microsoft Graph API.
     */
    @GetMapping("/invite")
    public String inviteUserToGraph() throws Exception {
        return "invite";
    }
}
