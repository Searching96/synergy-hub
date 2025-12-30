package com.synergyhub.listeners.email;

import com.synergyhub.events.email.EmailVerificationEvent;
import com.synergyhub.util.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class EmailVerificationEventListener {
    private final EmailService emailService;

    // ✅ Fix: Only send verification if the token generation committed
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEmailVerification(EmailVerificationEvent event) {
        emailService.sendEmailVerification(event.getUser().getEmail(), event.getToken(), event.getUser(), event.getIpAddress());
    }
}