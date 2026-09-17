package com.example.Login_DIAW.service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class PasswordRecoveryService {

    private final Map<String, RecoveryToken> tokens = new ConcurrentHashMap<>();

    /**
     * Gera um token de recuperação para o e-mail informado.
     */
    public String generateToken(String email) {

        String token = UUID.randomUUID().toString();

        // Token válido por 15 minutos
        LocalDateTime expiration = LocalDateTime.now().plusMinutes(15);

        tokens.put(token, new RecoveryToken(email, expiration));

        return token;
    }

    /**
     * Retorna o e-mail associado ao token.
     * Retorna null caso o token seja inválido ou expirado.
     */
    public String getEmailFromToken(String token) {

        if (token == null || token.isBlank()) {
            return null;
        }
        RecoveryToken recoveryToken = tokens.get(token);

        if (recoveryToken == null) {
            return null;
        }

        // Verifica se o token expirou
        if (!LocalDateTime.now().isBefore(recoveryToken.expiration())) {
            tokens.remove(token);
            return null;
        }

        return recoveryToken.email();
    }

    /**
     * Valida e retira o token em uma única operação de remoção.
     * Um token retirado não pode ser utilizado novamente.
     */
    public String consumeToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        RecoveryToken recoveryToken = tokens.remove(token);
        if (recoveryToken == null
                || !LocalDateTime.now().isBefore(recoveryToken.expiration())) {
            return null;
        }
        return recoveryToken.email();
    }

    /**
     * Remove o token depois que ele for utilizado.
     */
    public void invalidateToken(String token) {
        tokens.remove(token);
    }

    /**
     * Guarda as informações do token.
     */
    private record RecoveryToken(
            String email,
            LocalDateTime expiration) {
    }
}
