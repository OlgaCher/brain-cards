package com.braincards.controller;

import com.braincards.dto.RegistrationForm;
import com.braincards.repository.ParentRepository;
import com.braincards.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class RegistrationController {

    private final AuthService authService;
    private final ParentRepository parentRepository;

    public RegistrationController(AuthService authService, ParentRepository parentRepository) {
        this.authService = authService;
        this.parentRepository = parentRepository;
    }

    @GetMapping("/register")
    public String showForm(Model model) {
        model.addAttribute("registrationForm", new RegistrationForm());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registrationForm") RegistrationForm form, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "register";
        }
        if (parentRepository.findByEmail(form.getEmail()).isPresent()) {
            bindingResult.rejectValue("email", "email.duplicate", "This email is already registered");
            return "register";
        }
        authService.register(form);
        return "redirect:/login?registered";
    }
}
