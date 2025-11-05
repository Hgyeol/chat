package com.chat.user;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public String register(RegistrationRequest registrationRequest, RedirectAttributes redirectAttributes) {
        try {
            userService.registerNewUser(registrationRequest.getUsername(), registrationRequest.getPassword());
            return "redirect:/login";
        } catch (DataIntegrityViolationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Username already exists. Please choose a different one.");
            return "redirect:/user/register";
        }
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

}
