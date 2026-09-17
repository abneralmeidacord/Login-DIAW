package com.example.Login_DIAW.controller;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.Login_DIAW.exception.SendEmailException;
import com.example.Login_DIAW.service.PasswordRecoveryService;
import com.example.Login_DIAW.service.SendEmailService;

@Controller
public class LoginDIAWController {

    private static final Logger log = LoggerFactory.getLogger(LoginDIAWController.class);
    private final PasswordRecoveryService recoveryService;
    private final SendEmailService emailService;
    private final String baseUrl;
    private final InMemoryUserDetailsManager userDetailsManager;
    private final PasswordEncoder passwordEncoder;

    public LoginDIAWController(
            InMemoryUserDetailsManager userDetailsManager,
            PasswordEncoder passwordEncoder,
            PasswordRecoveryService recoveryService,
            SendEmailService emailService,
            @Value("${app.base-url:http://localhost:8080}") String baseUrl) {
        this.userDetailsManager = userDetailsManager;
        this.passwordEncoder = passwordEncoder;
        this.recoveryService = recoveryService;
        this.emailService = emailService;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
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
                        .build());

        redirectAttributes.addFlashAttribute(
                "registerSuccess",
                "Cadastro realizado com sucesso! Agora você pode fazer login.");
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

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        if (userDetailsManager.userExists(normalizedEmail)) {
            String token = recoveryService.generateToken(normalizedEmail);
            String link = baseUrl + "/resetpassword?token=" + token;

            try {
                emailService.sendEmail(
                        normalizedEmail,
                        "IntegraMed - Redefinição de senha",
                        "Para definir uma nova senha, acesse:\n" + link
                                + "\n\nEste link é válido por 15 minutos."
                                + "\nSe você não fez esta solicitação, ignore este e-mail.");
            } catch (SendEmailException e) {
                recoveryService.invalidateToken(token);
                log.error("Falha no envio do e-mail de recuperação.", e);
                redirectAttributes.addFlashAttribute(
                        "recoverError",
                        "Não foi possível enviar o e-mail. Tente novamente mais tarde.");
                return "redirect:/recoverpassword";
            }
        }

        // A resposta normal é igual para e-mails cadastrados e não cadastrados.
        redirectAttributes.addFlashAttribute(
                "recoverSuccess",
                "Se o email estiver cadastrado, você receberá as instruções de recuperação.");
        return "redirect:/login";
    }

    @GetMapping("/resetpassword")
    public String resetPassword(
            @RequestParam(name = "token", defaultValue = "") String token,
            Model model) {
        return showResetForm(token, model);
    }

    private String showResetForm(String token, Model model) {
        String email = recoveryService.getEmailFromToken(token);
        boolean valid = email != null && userDetailsManager.userExists(email);

        model.addAttribute("token", token);
        model.addAttribute("tokenValido", valid);
        if (!valid) {
            model.addAttribute("resetError", "Link inválido ou expirado. Solicite outro.");
        }
        return "resetpassword";
    }

    @PostMapping("/resetpassword")
    public String handleResetPassword(
            @RequestParam(name = "token", defaultValue = "") String token,
            @RequestParam("senha") String senha,
            @RequestParam("confirmacaoSenha") String confirmacaoSenha,
            Model model,
            RedirectAttributes redirectAttributes) {

        String email = recoveryService.getEmailFromToken(token);
        if (email == null || !userDetailsManager.userExists(email)) {
            return showResetForm(token, model);
        }

        if (senha.isBlank() || senha.length() < 8
                || senha.getBytes(StandardCharsets.UTF_8).length > 72) {
            model.addAttribute(
                    "resetError",
                    "Use pelo menos 8 caracteres e no máximo 72 bytes na senha.");
            return showResetForm(token, model);
        }

        if (!senha.equals(confirmacaoSenha)) {
            model.addAttribute("resetError", "As senhas não coincidem.");
            return showResetForm(token, model);
        }

        // Preserva o usuário, suas permissões e o estado da conta.
        var currentUser = userDetailsManager.loadUserByUsername(email);
        var updatedUser = User.withUserDetails(currentUser)
                .password(passwordEncoder.encode(senha))
                .build();

        // Retira o token atomicamente: dois envios não podem reutilizá-lo.
        if (recoveryService.consumeToken(token) == null) {
            return showResetForm(token, model);
        }

        userDetailsManager.updateUser(updatedUser);

        redirectAttributes.addFlashAttribute(
                "recoverSuccess", "Senha redefinida! Entre com sua nova senha.");
        return "redirect:/login";
    }
}
