package dev.onrcanogul.appbackend.notifications.api.port;

import dev.onrcanogul.appbackend.notifications.api.model.EmailMessage;

/** Sends a transactional e-mail. Same rule as push: never fail the caller's work. */
public interface EmailSender {

    void send(EmailMessage message);
}
