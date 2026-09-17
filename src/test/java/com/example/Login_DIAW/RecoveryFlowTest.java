package com.example.Login_DIAW.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import com.example.Login_DIAW.exception.SendEmailException;
import com.example.Login_DIAW.service.PasswordRecoveryService;
import com.example.Login_DIAW.service.SendEmailService;

class RecoveryFlowTest {
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final InMemoryUserDetailsManager users = new InMemoryUserDetailsManager(
            User.withUsername("teste@example.com").password(encoder.encode("senhaAntiga123")).roles("USER", "ADMIN")
                    .build());
    private final PasswordRecoveryService tokens = new PasswordRecoveryService();
    private final CapturingMail mail = new CapturingMail();
    private final LoginDIAWController controller = new LoginDIAWController(users, encoder, tokens, mail,
            "http://localhost:8080");

    static class CapturingMail extends SendEmailService {
        String to;
        String body;
        boolean fail;

        CapturingMail() {
            super(null, "sender@example.com");
        }

        @Override
        public void sendEmail(String to, String subject, String body) {
            this.to = to;
            this.body = body;
            if (fail)
                throw new SendEmailException("Simulated SMTP failure");
        }

        String token() {
            return body.split("token=")[1].split("\\s")[0];
        }
    }

    @Test
    void sendsLinkAndResetsPreservingRolesAndRejectsReplay() {
        assertEquals("redirect:/login",
                controller.handleRecoverPassword(" TESTE@example.com ", new RedirectAttributesModelMap()));
        assertEquals("teste@example.com", mail.to);
        String token = mail.token();
        assertEquals("teste@example.com", tokens.getEmailFromToken(token));
        var page = new ExtendedModelMap();
        assertEquals("resetpassword", controller.resetPassword(token, page));
        assertEquals(true, page.get("tokenValido"));
        assertEquals("redirect:/login", controller.handleResetPassword(token, "novaSenha123", "novaSenha123",
                new ExtendedModelMap(), new RedirectAttributesModelMap()));
        var updated = users.loadUserByUsername("teste@example.com");
        assertTrue(encoder.matches("novaSenha123", updated.getPassword()));
        assertFalse(encoder.matches("senhaAntiga123", updated.getPassword()));
        assertTrue(updated.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        assertNull(tokens.getEmailFromToken(token));
        var replay = new ExtendedModelMap();
        assertEquals("resetpassword", controller.handleResetPassword(token, "outraSenha123", "outraSenha123", replay,
                new RedirectAttributesModelMap()));
        assertEquals(false, replay.get("tokenValido"));
        assertTrue(encoder.matches("novaSenha123", users.loadUserByUsername("teste@example.com").getPassword()));
    }

    @Test
    void unknownEmailReturnsSameNormalMessageWithoutSending() {
        var known = new RedirectAttributesModelMap();
        controller.handleRecoverPassword("teste@example.com", known);
        mail.to = null;
        var unknown = new RedirectAttributesModelMap();
        assertEquals("redirect:/login", controller.handleRecoverPassword("missing@example.com", unknown));
        assertNull(mail.to);
        assertEquals(known.getFlashAttributes().get("recoverSuccess"),
                unknown.getFlashAttributes().get("recoverSuccess"));
    }

    @Test
    void mismatchDoesNotConsumeTokenOrChangePassword() {
        String token = tokens.generateToken("teste@example.com");
        var model = new ExtendedModelMap();
        assertEquals("resetpassword", controller.handleResetPassword(token, "novaSenha123", "diferente123", model,
                new RedirectAttributesModelMap()));
        assertEquals("As senhas não coincidem.", model.get("resetError"));
        assertEquals("teste@example.com", tokens.getEmailFromToken(token));
        assertTrue(encoder.matches("senhaAntiga123", users.loadUserByUsername("teste@example.com").getPassword()));
    }

    @Test
    void rejectsShortAndOversizedPassword() {
        String token = tokens.generateToken("teste@example.com");
        for (String password : new String[] { "abc", "á".repeat(40) }) {
            var model = new ExtendedModelMap();
            assertEquals("resetpassword",
                    controller.handleResetPassword(token, password, password, model, new RedirectAttributesModelMap()));
            assertNotNull(model.get("resetError"));
            assertEquals("teste@example.com", tokens.getEmailFromToken(token));
        }
    }

    @Test
    void missingAndInvalidTokenHideForm() {
        for (String token : new String[] { "", "invalid" }) {
            var model = new ExtendedModelMap();
            assertEquals("resetpassword", controller.resetPassword(token, model));
            assertEquals(false, model.get("tokenValido"));
        }
    }

    @Test
    void failedEmailInvalidatesGeneratedToken() {
        mail.fail = true;
        var redirect = new RedirectAttributesModelMap();
        assertEquals("redirect:/recoverpassword", controller.handleRecoverPassword("teste@example.com", redirect));
        assertNotNull(redirect.getFlashAttributes().get("recoverError"));
        assertNull(tokens.getEmailFromToken(mail.token()));
    }

    @Test
    void tokenCanBeConsumedOnlyOnce() throws Exception {
        String token = tokens.generateToken("teste@example.com");
        var executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            var tasks = java.util.List.<java.util.concurrent.Callable<String>>of(
                    () -> tokens.consumeToken(token), () -> tokens.consumeToken(token));
            var results = executor.invokeAll(tasks);
            int successes = 0;
            for (var result : results)
                if (result.get() != null)
                    successes++;
            assertEquals(1, successes);
        } finally {
            executor.shutdownNow();
        }
    }
}
