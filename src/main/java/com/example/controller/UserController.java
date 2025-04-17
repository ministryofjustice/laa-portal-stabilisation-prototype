package com.example.controller;

import com.example.model.ServicePrincipalModel;
import com.example.service.CreateUserNotificationService;
import com.example.model.PaginatedUsers;
import com.example.dto.OfficeData;
import com.example.dto.PermissionsData;
import com.example.model.UserRole;
import com.example.service.UserService;
import com.example.utils.RandomPasswordGenerator;
import com.microsoft.graph.models.Invitation;
import com.microsoft.graph.models.User;
import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;
import com.example.dto.UserData;

import java.util.List;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
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

    @GetMapping("/user/create/details")
    public String createUser(HttpSession session, Model model) {
        UserData storedData = (UserData) session.getAttribute("userData");

        if (storedData == null) {
            storedData = new UserData();
        }
        model.addAttribute("userData", storedData);
        return "user/user-details";
    }

    @PostMapping("/user/create/details")
    public RedirectView postUser(HttpSession session, @ModelAttribute UserData userData) {
        session.setAttribute("userData", userData);
        return new RedirectView("/user/create/services");
    }

    @GetMapping("/user/create/services")
    //@PreAuthorize("hasAuthority('SCOPE_User.ReadWrite.All') and hasAuthority('SCOPE_Directory.ReadWrite.All')")
    public String addUserTwo(Model model, HttpSession session, @ModelAttribute UserData userData) throws Exception {
        List<ServicePrincipalModel> apps = userService.getServicePrincipals().stream()
                .map(x -> new ServicePrincipalModel(x, false)).collect(Collectors.toList());
        List<String> selectedApps = (List<String>) session.getAttribute("apps");
        for (ServicePrincipalModel app : apps) {
            if (Objects.nonNull(selectedApps) && selectedApps.contains(app.getServicePrincipal().getAppId())) {
                app.setSelected(true);
            }
        }
        model.addAttribute("apps", apps);
        UserData storedData = (UserData) session.getAttribute("userData");

        if (storedData == null) {
            storedData = new UserData();
        }
        model.addAttribute("userData", storedData);
        return "add-user-apps";
    }

    @PostMapping("/user/create/services")
    //@PreAuthorize("hasAuthority('SCOPE_User.ReadWrite.All') and hasAuthority('SCOPE_Directory.ReadWrite.All')")
    public String addUserTwo(@RequestParam("apps") List<String> apps,
                             HttpSession session) throws Exception {
        session.setAttribute("apps", apps);

        return "redirect:/user/create/roles";
    }

    @GetMapping("/user/create/roles")
    //@PreAuthorize("hasAuthority('SCOPE_User.ReadWrite.All') and hasAuthority('SCOPE_Directory.ReadWrite.All')")
    public String addUserThree(Model model, HttpSession session) throws Exception {
        List<String> selectedApps = (List<String>) session.getAttribute("apps");
        if (Objects.isNull(selectedApps)) {
            selectedApps = new ArrayList<>();
        }
        List<UserRole> roles = userService.getAllAvailableRolesForApps(selectedApps);
        List<String> selectedRoles = (List<String>) session.getAttribute("roles");
        for (UserRole role : roles) {
            if (Objects.nonNull(selectedRoles) && selectedRoles.contains(role.getAppRoleId())) {
                role.setSelected(true);
            }
        }
        model.addAttribute("roles", roles);
        return "add-user-roles";
    }

    @PostMapping("/user/create/roles")
    //@PreAuthorize("hasAuthority('SCOPE_User.ReadWrite.All') and hasAuthority('SCOPE_Directory.ReadWrite.All')")
    public String addUserThree(@RequestParam("selectedRoles") List<String> roles,
                             HttpSession session) throws Exception {
        session.setAttribute("roles", roles);
        return "redirect:/user/create/offices";
    }

    @GetMapping("/user/create/offices")
    public String offices(HttpSession session, Model model) {
        OfficeData officeData = (OfficeData) session.getAttribute("officeData");
        if (officeData == null) {
            officeData = new OfficeData();
        }
        model.addAttribute("officeData", officeData);
        return "user/offices";
    }

    @PostMapping("/user/create/offices")
    public String postOffices(HttpSession session, @RequestParam(value = "office", required = false) List<String> selectedOffices) {
        OfficeData officeData = new OfficeData();
        officeData.setSelectedOffices(selectedOffices);
        session.setAttribute("officeData", officeData);
        return "redirect:/user/create/check-answers";
    }

    @GetMapping("/user/create/check-answers")
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
            if (Objects.nonNull(selectedRoles)) {
                for (UserRole role : roles) {
                    if (selectedRoles.contains(role.getAppRoleId())) {
                        List<UserRole> appRoles = cyaRoles.getOrDefault(role.getAppId(), new ArrayList<>());
                        appRoles.add(role);
                        cyaRoles.put(role.getAppId(), appRoles);
                    }
                }
            }
            model.addAttribute("roles", cyaRoles);
        }

        UserData storedData = (UserData) session.getAttribute("userData");

        if (storedData == null) {
            storedData = new UserData();
        }
        model.addAttribute("user", storedData);

        return "add-user-cya";
    }

    @PostMapping("/users/add/cya")
    //@PreAuthorize("hasAuthority('SCOPE_User.ReadWrite.All') and hasAuthority('SCOPE_Directory.ReadWrite.All')")
    public String addUserCya(HttpSession session) throws Exception {
        String password = RandomPasswordGenerator.generateRandomPassword(8);
        User user = (User) session.getAttribute("userData");

        List<String> selectedRoles = (List<String>) session.getAttribute("roles");
        user = userService.createUser(user, password, selectedRoles);
        createUserNotificationService.notifyCreateUser(user.getDisplayName(), user.getMail(), password, user.getId());
        session.removeAttribute("roles");
        session.removeAttribute("apps");
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






    @GetMapping("/permissions")
    public String permissions(HttpSession session, Model model) {
        PermissionsData permissionData = (PermissionsData) session.getAttribute("permissionData");
        if (permissionData == null) {
            permissionData = new PermissionsData();
        }
        model.addAttribute("permissionData", permissionData);
        return "user/permissions";

    }

    @PostMapping("/permissions")
    public RedirectView postPermissions(HttpSession session, @RequestParam(value = "permission", required = false) List<String> selectedPermissions) {
        PermissionsData permissionData = new PermissionsData();
        permissionData.setSelectedPermissions(selectedPermissions);
        session.setAttribute("permissionData", permissionData);
        return new RedirectView("/check-answers");
    }




    @GetMapping("/check-answers")
    public String checkAnswers(HttpSession session, Model model) {
        OfficeData officeData = (OfficeData) session.getAttribute("officeData");
        PermissionsData permissionData = (PermissionsData) session.getAttribute("permissionData");
        UserData userData = (UserData) session.getAttribute("userData");
        model.addAttribute("userData", userData);
        model.addAttribute("officeData", officeData);
        model.addAttribute("permissionData", permissionData);
        return "user/check-answers";
    }

    @GetMapping("/confirmation")
    public String confirmation(HttpSession session, Model model) {
        UserData userData = (UserData) session.getAttribute("userData");
        model.addAttribute("userData", userData);
        return "user/confirmation";
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
