package com.braincards.controller;

import com.braincards.model.Parent;
import com.braincards.repository.ParentRepository;
import com.braincards.service.ParentUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final ParentRepository parentRepository;

    public HomeController(ParentRepository parentRepository) {
        this.parentRepository = parentRepository;
    }

    @GetMapping("/")
    public String index() {
        return "redirect:/home";
    }

    @GetMapping("/home")
    public String home(@AuthenticationPrincipal ParentUserDetails principal, Model model) {
        Parent parent = parentRepository.findById(principal.getParentId()).orElseThrow();
        model.addAttribute("parent", parent);
        model.addAttribute("child", parent.getChild());
        return "home";
    }
}
