package com.example.Login_DIAW.controller;

import com.example.Login_DIAW.dto.EmailRequestDTO;
import com.example.Login_DIAW.service.SendEmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/email")
public class SendEmailController {

    @Autowired
    private SendEmailService sendEmailService;

    @PostMapping("/send")
    public String sendEmail(@RequestBody EmailRequestDTO emailRequest) {
        sendEmailService.sendEmail(emailRequest.getTo(), emailRequest.getSubject(), emailRequest.getBody());
        return "Email enviado com sucesso!";
    }
}