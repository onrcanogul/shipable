package dev.onrcanogul.appbackend.notifications.api.model;

/**
 * A transactional e-mail.
 *
 * <p>Transactional only. Marketing e-mail needs consent tracking and an unsubscribe link,
 * which this module deliberately does not model — send those from a tool built for it.
 */
public record EmailMessage(String to, String subject, String body, boolean html) {

    public static EmailMessage text(String to, String subject, String body) {
        return new EmailMessage(to, subject, body, false);
    }
}
