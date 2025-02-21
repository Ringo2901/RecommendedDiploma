package by.bsuir.aleksandrov.recommendeddiploma.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminController {

    @GetMapping("/admin")
    public String adminPanel(Model model) {
        model.addAttribute("message", "Добро пожаловать в админ-панель!");
        return "admin"; // ✅ Загружает templates/admin.html
    }
}
