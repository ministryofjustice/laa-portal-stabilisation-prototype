package com.example.controller;

import com.example.service.EmailService;
import com.example.dto.OfficeData;
import com.example.dto.PermissionsData;
import com.example.service.UserService;
import com.example.utils.RandomPasswordGenerator;
import com.microsoft.graph.models.Invitation;
import com.microsoft.graph.models.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.view.RedirectView;
import jakarta.servlet.http.HttpSession;
import com.example.dto.UserData;

import java.util.List;

/**
 * User Controller
 */
@Controller
public class UserController {

    /**
     * Add new user via Microsoft Graph API.
     */
    @PostMapping("/register")
    public User addUserToGraph(@RequestParam("username") String username,
                               @RequestParam("email") String email) throws Exception {
        String password = RandomPasswordGenerator.generateRandomPassword(8);
        User user = UserService.createUser(username, email, password);
        String welcomeMsg = EmailService.getWelcomeMessage(username, password);
        EmailService.sendMail(email, "Welcome", welcomeMsg);
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
        Invitation result = UserService.inviteUser(email);
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

    @GetMapping("/create-user")
    public String createUser(HttpSession session, Model model) {
        UserData storedData = (UserData) session.getAttribute("userData");

        if (storedData == null) {
            storedData = new UserData();
        }
        model.addAttribute("userData", storedData);
        return "user/create-user";
    }

    @PostMapping("/create-user")
    public RedirectView postUser(HttpSession session, @ModelAttribute UserData userData) {
        session.setAttribute("userData", userData);

        return new RedirectView("/offices");
    }

    @GetMapping("/offices")
    public String offices(HttpSession session, Model model) {
        OfficeData officeData = (OfficeData) session.getAttribute("officeData");
        if (officeData != null && officeData.getSelectedOffices() != null) {
            System.out.println(officeData.getSelectedOffices());
        } else {
            System.out.println("no selected options in session");
        }
        if (officeData == null) {
            officeData = new OfficeData();
        }
        model.addAttribute("officeData", officeData);
        return "user/offices";
    }

    @PostMapping("/offices")
    public RedirectView postOffices(HttpSession session, @RequestParam(value = "office", required = false) List<String> selectedOffices) {
        OfficeData officeData = new OfficeData();
        officeData.setSelectedOffices(selectedOffices);
        session.setAttribute("officeData", officeData);
        return new RedirectView("/permissions");
    }


    @GetMapping("/permissions")
    public String permissions(HttpSession session, Model model) {
        PermissionsData permissionData = (PermissionsData) session.getAttribute("permissionData");
        if (permissionData != null && permissionData.getSelectedPermissions() != null) {
            System.out.println(permissionData.getSelectedPermissions());
        } else {
            System.out.println("no selected permissions in session");
        }
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
    public String confirmation(Model model) {
        return "user/confirmation";
    }

}
