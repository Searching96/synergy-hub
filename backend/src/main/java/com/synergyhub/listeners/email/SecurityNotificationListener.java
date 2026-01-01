package com.synergyhub.listeners.email;

import com.synergyhub.events.auth.AccountLockedEvent;
import com.synergyhub.events.auth.PasswordChangedEvent;
import com.synergyhub.util.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class SecurityNotificationListener {

    private final EmailService emailService;

    @Async // Send emails asynchronously to not block the user response
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPasswordChanged(PasswordChangedEvent event) {
        emailService.sendPasswordChangedEmail(
            event.getUser().getEmail(), 
            event.getUser(), 
            event.getIpAddress()
        );
    }

    @Async
    @EventListener // Listen immediately (locking might happen outside a transaction commit)
    public void onAccountLocked(AccountLockedEvent event) {
        emailService.sendAccountLockedEmail(
            event.getUser().getEmail(), 
            event.getUser(), 
            event.getIpAddress()
        );
    }
}