package dev.onrcanogul.appbackend.notifications.internal.service;

import dev.onrcanogul.appbackend.notifications.api.model.EmailMessage;
import dev.onrcanogul.appbackend.notifications.api.port.EmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logs instead of sending.
 *
 * <p>Swap in Postmark, Resend, SES or whatever you use by defining your own
 * {@link EmailSender} bean.
 *
 * <p>Note that the body is logged. Fine for a no-op in development; when you write the real
 * one, do not log message bodies - transactional e-mail contains reset links and personal
 * data, and logs are the least protected thing you own.
 */
public class NoopEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(NoopEmailSender.class);

    @Override
    public void send(EmailMessage message) {
        log.info("[no-op email] to={} subject='{}'", message.to(), message.subject());
    }
}
