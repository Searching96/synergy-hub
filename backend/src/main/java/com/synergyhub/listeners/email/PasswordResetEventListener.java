package com.synergyhub.listeners.email;

import com.synergyhub.events.auth.PasswordResetRequestedEvent;
import com.synergyhub.util.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class PasswordResetEventListener {
    private final EmailService emailService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPasswordResetRequested(PasswordResetRequestedEvent event) {
        emailService.sendPasswordResetEmail(event.getUser().getEmail(), event.getToken(), event.getUser(), event.getIpAddress());
    }
}