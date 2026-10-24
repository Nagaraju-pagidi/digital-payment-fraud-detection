package com.fraudpay.payment.service;

import com.fraudpay.payment.web.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/** The bank's own email channel (OTPs, account opening). In the demo every email lands in Mailpit. */
@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final JavaMailSender mailSender;
    private final String from;
    private final String bankName;

    public MailService(JavaMailSender mailSender,
                       @Value("${fraudpay.mail-from}") String from,
                       @Value("${fraudpay.bank-name}") String bankName) {
        this.mailSender = mailSender;
        this.from = from;
        this.bankName = bankName;
    }

    /** Sends immediately; throws 503 if the mail server is down (an OTP that never arrives is useless). */
    public void send(String to, String subject, String body) {
        try {
            SimpleMailMessage m = new SimpleMailMessage();
            m.setFrom(from);
            m.setTo(to);
            m.setSubject(bankName + ": " + subject);
            m.setText(body + "\n\n-- \n" + bankName + " | This is an automated message, please do not reply.\n"
                    + "Never share your OTP, password or transaction PIN with anyone - not even bank staff.");
            mailSender.send(m);
            log.info("Email '{}' sent to {}", subject, to);
        } catch (Exception e) {
            log.warn("Email to {} failed: {}", to, e.getMessage());
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "We could not send the OTP email right now. Please try again in a minute.");
        }
    }

    /** Best-effort notification (account opened, password changed...). Never fails the request. */
    public void notify(String to, String subject, String body) {
        try {
            send(to, subject, body);
        } catch (Exception ignored) {
            // already logged
        }
    }
}
