package com.example.controller;

import com.example.service.CreateUserNotificationService;
import com.example.service.EmailService;
import com.example.service.UserService;
import com.example.utils.RandomPasswordGenerator;
import com.microsoft.graph.models.Invitation;
import com.microsoft.graph.models.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * User Controller
 */
@Controller
@RequiredArgsConstructor
public class UserController {

    private final CreateUserNotificationService createUserNotificationService;

    /**
     * Add new user via Microsoft Graph A
     */
    @PostMapping("/register")
    //@PreAuthorize("hasAuthority('SCOPE_User.ReadWrite.All') and hasAuthority('SCOPE_Directory.ReadWrite.All')")
    public User addUserToGraph(@RequestParam("username") String username,
                               @RequestParam("email") String email,
                               @RequestParam("application") String application,
                               @RequestParam("role") String role,
                               @RequestParam("office") String office) throws Exception {
        String password = RandomPasswordGenerator.generateRandomPassword(8);
        User user = UserService.createUser(username, email, password, application, role, office);
        createUserNotificationService.notifyCreateUser(username, email, password, user.getId());
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
    //@PreAuthorize("hasAuthority('SCOPE_User.ReadWrite.All') and hasAuthority('SCOPE_Directory.ReadWrite.All') and hasAuthority('SCOPE_User.Invite.All')")
    public Invitation invite(@RequestParam("email") String email, String application, String role, String office, Model model) throws Exception {
        Invitation result = UserService.inviteUser(email, application, role, office);
        model.addAttribute("redeemUrl", result.getInviteRedeemUrl());
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
