package com.example.controller;

import com.example.service.GraphApiService;
import com.example.service.UserService;
import com.microsoft.graph.models.Invitation;
import com.microsoft.graph.models.User;
import com.microsoft.graph.models.UserCollectionResponse;
import org.springframework.stereotype.Controller;
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
     * Add new user via Microsoft Graph API.
     */
    @PostMapping("/register")
    public Invitation addUserToGraph(@RequestParam("email") String email) throws Exception {
        Invitation result = GraphApiService.inviteUser(email);
        return result;
    }

    /**
     * Retrieves a list of users from Microsoft Graph API.
     */
    @GetMapping("/register")
    public String register() throws Exception {
        return "register";
    }
}
