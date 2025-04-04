package com.example.controller;

import com.example.service.CreateUserNotificationService;
import com.example.model.PaginatedUsers;
import com.example.model.UserRole;
import com.example.service.UserService;
import com.example.utils.RandomPasswordGenerator;
import com.microsoft.graph.models.Invitation;
import com.microsoft.graph.models.User;
import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

/**
 * User Controller
 */
@Controller
@RequiredArgsConstructor
public class UserController {

    private final CreateUserNotificationService createUserNotificationService;
    private final UserService userService;

    /**
     * Add new user via Microsoft Graph API.
     */
    @PostMapping("/register")
    //@PreAuthorize("hasAuthority('SCOPE_User.ReadWrite.All') and hasAuthority('SCOPE_Directory.ReadWrite.All')")
    public User addUserToGraph(@RequestParam("username") String username,
                               @RequestParam("email") String email,
                               @RequestParam("application") String application,
                               @RequestParam("role") String role,
                               @RequestParam("office") String office) throws Exception {
        String password = RandomPasswordGenerator.generateRandomPassword(8);
        User user = userService.createUser(username, email, password, application, role, office);
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

    /**
     * Retrieves available user roles for user
     */
    @GetMapping("/users/edit/{id}/roles")
    public String editUserRoles(@PathVariable String id, Model model) {
        User user = userService.getUserById(id);
        List<UserRole> userRoles = userService.getUserAppRolesByUserId(id);
        List<UserRole> availableRoles = userService.getAllAvailableRoles();

        Set<String> userAssignedRoleIds = userRoles.stream()
                .map(UserRole::getAppRoleId)
                .collect(Collectors.toSet());

        model.addAttribute("user", user);
        model.addAttribute("availableRoles", availableRoles);
        model.addAttribute("userAssignedRoles", userAssignedRoleIds);

        return "edit-user-roles";
    }


    /**
     * Update user roles via graph SDK
     */
    @PostMapping("/users/edit/{id}/roles")
    public String updateUserRoles(@PathVariable String id,
                                  @RequestParam(required = false) List<String> selectedRoles) {
        userService.updateUserRoles(id, selectedRoles);
        return "redirect:/users";
    }

}
