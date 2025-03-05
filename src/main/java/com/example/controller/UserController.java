package com.example.controller;

import com.example.service.GraphApiService;
import com.example.service.UserService;

/**
 * User Controller
 */
public class UserController {

    private final UserService userService;
    private final GraphApiService graphApiService;

    /**
     * UserController handles requests related to user operations.
     *
     * @param userService     the service used to manage user-related operations
     * @param graphApiService the service used to interact with the Graph API
     */
    public UserController(UserService userService, GraphApiService graphApiService) {
        this.userService = userService;
        this.graphApiService = graphApiService;
    }
}
