package dev.onrcanogul.appbackend.quota.api.model;

import dev.onrcanogul.appbackend.core.api.error.AppException;
import dev.onrcanogul.appbackend.core.api.error.ErrorCodes;
import org.springframework.http.HttpStatus;

/** 429 when a caller proceeds past a denied {@link QuotaDecision}. */
public class QuotaExceededException extends AppException {

    public QuotaExceededException(QuotaDecision decision) {
        super(HttpStatus.TOO_MANY_REQUESTS, ErrorCodes.QUOTA_EXCEEDED,
                "Quota exceeded for " + decision.key() + ": " + decision.reason());
    }
}
