package com.example.controller;

import com.example.model.ServicePrincipalModel;
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

import java.util.*;
import java.util.stream.Collectors;

/**
 * User Controller
 */
@Controller
@RequiredArgsConstructor
public class UserController {

    private final CreateUserNotificationService createUserNotificationService;
    private final UserService userService;

    @GetMapping("/users/add/step1")
    //@PreAuthorize("hasAuthority('SCOPE_User.ReadWrite.All') and hasAuthority('SCOPE_Directory.ReadWrite.All')")
    public String addUserOne(Model model,
                             HttpSession session) throws Exception {
        User user = (User) session.getAttribute("user");
        if (Objects.isNull(user)) {
            user = new User();
        }
        model.addAttribute("user", user);
        return "add-user-detail";
    }

    @PostMapping("/users/add/step1")
    //@PreAuthorize("hasAuthority('SCOPE_User.ReadWrite.All') and hasAuthority('SCOPE_Directory.ReadWrite.All')")
    public String addUserOne(@RequestParam("username") String username,
                             @RequestParam("email") String email,
                             @RequestParam("office") String office,
                             HttpSession session) throws Exception {
        User user = (User) session.getAttribute("user");
        if (Objects.isNull(user)) {
            user = new User();
        }
        user.setDisplayName(username);
        user.setMail(email);
        user.setOfficeLocation(office);
        session.setAttribute("user", user);
        return "redirect:/users/add/step2";
    }

    @GetMapping("/users/add/step2")
    //@PreAuthorize("hasAuthority('SCOPE_User.ReadWrite.All') and hasAuthority('SCOPE_Directory.ReadWrite.All')")
    public String addUserTwo(Model model, HttpSession session) throws Exception {
        List<ServicePrincipalModel> apps = userService.getServicePrincipals().stream()
                .map(x->new ServicePrincipalModel(x, false)).collect(Collectors.toList());
        List<String> selectedApps = (List<String>) session.getAttribute("apps");
        for (ServicePrincipalModel app : apps) {
            if (Objects.nonNull(selectedApps) && selectedApps.contains(app.getServicePrincipal().getAppId())) {
                app.setSelected(true);
            }
        }
        model.addAttribute("apps", apps);
        return "add-user-apps";
    }

    @PostMapping("/users/add/step2")
    //@PreAuthorize("hasAuthority('SCOPE_User.ReadWrite.All') and hasAuthority('SCOPE_Directory.ReadWrite.All')")
    public String addUserTwo(@RequestParam("apps") List<String> apps,
                             HttpSession session) throws Exception {
        session.setAttribute("apps", apps);
        return "redirect:/users/add/step3";
    }

    @GetMapping("/users/add/step3")
    //@PreAuthorize("hasAuthority('SCOPE_User.ReadWrite.All') and hasAuthority('SCOPE_Directory.ReadWrite.All')")
    public String addUserThree(Model model, HttpSession session) throws Exception {
        List<String> selectedApps = (List<String>) session.getAttribute("apps");
        if (Objects.isNull(selectedApps)) {
            selectedApps = new ArrayList<>();
        }
        List<UserRole> roles = userService.getAllAvailableRolesForApps(selectedApps);
        List<String> selecteRoles = (List<String>) session.getAttribute("roles");
        for (UserRole role : roles) {
            if (Objects.nonNull(selecteRoles) && selecteRoles.contains(role.getAppRoleId())) {
                role.setSelected(true);
            }
        }
        model.addAttribute("roles", roles);
        return "add-user-roles";
    }

    @PostMapping("/users/add/step3")
    //@PreAuthorize("hasAuthority('SCOPE_User.ReadWrite.All') and hasAuthority('SCOPE_Directory.ReadWrite.All')")
    public String addUserThree(@RequestParam("selectedRoles") List<String> roles,
                             HttpSession session) throws Exception {
        session.setAttribute("roles", roles);
        return "redirect:/users/add/cya";
    }

    @GetMapping("/users/add/cya")
    //@PreAuthorize("hasAuthority('SCOPE_User.ReadWrite.All') and hasAuthority('SCOPE_Directory.ReadWrite.All')")
    public String addUserCya(Model model, HttpSession session) throws Exception {
        List<String> selectedApps = (List<String>) session.getAttribute("apps");
        if (Objects.isNull(selectedApps)) {
            selectedApps = new ArrayList<>();
        }
        if (!selectedApps.isEmpty()) {
            List<UserRole> roles = userService.getAllAvailableRolesForApps(selectedApps);
            List<String> selectedRoles = (List<String>) session.getAttribute("roles");
            Map<String, List<UserRole>> cyaRoles = new HashMap<>();
            for (UserRole role : roles) {
                if (selectedRoles.contains(role.getAppRoleId())) {
                    List<UserRole> appRoles = cyaRoles.getOrDefault(role.getAppId(), new ArrayList<>());
                    appRoles.add(role);
                    cyaRoles.put(role.getAppId(), appRoles);
                }
            }
            model.addAttribute("roles", cyaRoles);
        }
        User user = (User) session.getAttribute("user");
        if (Objects.isNull(user)) {
            user = new User();
        }
        model.addAttribute("user", user);
        return "add-user-cya";
    }

    @PostMapping("/users/add/cya")
    //@PreAuthorize("hasAuthority('SCOPE_User.ReadWrite.All') and hasAuthority('SCOPE_Directory.ReadWrite.All')")
    public String addUserCya(HttpSession session) throws Exception {
        String password = RandomPasswordGenerator.generateRandomPassword(8);
        User user = (User) session.getAttribute("user");
        List<String> selectedRoles = (List<String>) session.getAttribute("roles");
        user = userService.createUser(user, password, selectedRoles);
        createUserNotificationService.notifyCreateUser(user.getDisplayName(), user.getMail(), password, user.getId());
        session.removeAttribute("roles");
        return "redirect:/users/add/created";
    }

    @GetMapping("/users/add/created")
    //@PreAuthorize("hasAuthority('SCOPE_User.ReadWrite.All') and hasAuthority('SCOPE_Directory.ReadWrite.All')")
    public String addUsercreated(Model model, HttpSession session) throws Exception {
        User user = (User) session.getAttribute("user");
        model.addAttribute("user", user);
        return "add-user-created";
    }

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
