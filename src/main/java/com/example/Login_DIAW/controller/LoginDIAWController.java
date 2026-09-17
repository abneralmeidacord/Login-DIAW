package com.example.Login_DIAW.controller;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class LoginDIAWController {

    private final InMemoryUserDetailsManager userDetailsManager;
    private final PasswordEncoder passwordEncoder;

    public LoginDIAWController(
            InMemoryUserDetailsManager userDetailsManager,
            PasswordEncoder passwordEncoder) {
        this.userDetailsManager = userDetailsManager;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/home")
    public String home() {
        return "home";
    }

    @GetMapping("/admin")
    public String admin() {
        return "admin";
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    @PostMapping("/register")
    public String handleRegister(
            @RequestParam("nome") String nome,
            @RequestParam("email") String email,
            @RequestParam("senha") String senha,
            @RequestParam("confirmacaoSenha") String confirmacaoSenha,
            RedirectAttributes redirectAttributes) {

        if (nome.isBlank() || email.isBlank() || senha.isBlank() || confirmacaoSenha.isBlank()) {
            redirectAttributes.addFlashAttribute("registerError", "Preencha todos os campos obrigatórios.");
            return "redirect:/register";
        }

        String normalizedEmail = email.trim().toLowerCase();

        if (!normalizedEmail.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            redirectAttributes.addFlashAttribute("registerError", "Informe um email válido.");
            return "redirect:/register";
        }

        if (senha.length() < 8) {
            redirectAttributes.addFlashAttribute("registerError", "A senha deve possuir pelo menos 8 caracteres.");
            return "redirect:/register";
        }

        if (!senha.equals(confirmacaoSenha)) {
            redirectAttributes.addFlashAttribute("registerError", "As senhas informadas não coincidem.");
            return "redirect:/register";
        }

        if (userDetailsManager.userExists(normalizedEmail)) {
            redirectAttributes.addFlashAttribute("registerError", "Já existe uma conta cadastrada com este email.");
            return "redirect:/register";
        }

        userDetailsManager.createUser(
                User.withUsername(normalizedEmail)
                        .password(passwordEncoder.encode(senha))
                        .roles("USER")
                        .build()
        );

        redirectAttributes.addFlashAttribute(
                "registerSuccess",
                "Cadastro realizado com sucesso! Agora você pode fazer login."
        );
        return "redirect:/login";
    }

    @GetMapping("/recoverpassword")
    public String recoverpassword() {
        return "recoverpassword";
    }

    @PostMapping("/recoverpassword")
    public String handleRecoverPassword(
            @RequestParam("email") String email,
            RedirectAttributes redirectAttributes) {

        // Aqui você pode adicionar lógica para recuperar a senha.
        // userService.recoverPassword(email);

        redirectAttributes.addFlashAttribute(
                "recoverSuccess",
                "Se o email estiver cadastrado, você receberá as instruções de recuperação."
        );
        return "redirect:/login";
    }
}
    
