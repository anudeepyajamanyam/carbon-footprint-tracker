package com.carbon.tracker.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Handles clean browser URLs for public landing, login, registration, and dashboard views.
 * Uses forwards so browser URLs remain clean (e.g. /login instead of /login.html).
 */
@Controller
public class ViewController {

    @GetMapping("/login")
    public String login() {
        return "forward:/login.html";
    }

    @GetMapping("/register")
    public String register() {
        return "forward:/register.html";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "forward:/dashboard.html";
    }
}
