package com.example.Login_DIAW.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.Login_DIAW.dto.EmailRequestDTO;
import com.example.Login_DIAW.service.SendEmailService;

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
