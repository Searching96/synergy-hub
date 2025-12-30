package com.synergyhub.listeners.email;

import com.synergyhub.domain.entity.User;
import com.synergyhub.events.auth.RegistrationCompletedEvent;
import com.synergyhub.util.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class RegistrationEventListener {
    private final EmailService emailService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRegistrationCompleted(RegistrationCompletedEvent event) {
        User user = event.getUser();
        emailService.sendWelcomeEmail(user.getEmail(), user.getName(), user, event.getIpAddress());
    }
}