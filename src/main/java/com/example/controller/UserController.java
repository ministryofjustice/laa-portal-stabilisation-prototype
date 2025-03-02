package com.example.controller;

import com.example.service.GraphApiService;
import com.example.service.UserService;
import com.microsoft.graph.models.UserCollectionResponse;
import org.springframework.web.bind.annotation.GetMapping;

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
}
