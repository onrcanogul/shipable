package dev.onrcanogul.appbackend.core.api.port;

import dev.onrcanogul.appbackend.core.api.model.UserId;
import java.util.Map;

/**
 * How a module joins in on account deletion and data export.
 *
 * <p>Every module that stores anything about a user implements this; {@code privacy}
 * collects the implementations and calls them. That inversion is what keeps deletion
 * correct as the app grows: a new module that stores user data becomes discoverable to
 * the deletion flow by implementing this interface, instead of by someone remembering to
 * edit a checklist.
 *
 * <p>Defined in {@code core} rather than in {@code privacy} so contributors do not have to
 * depend on the module that consumes them.
 */
public interface UserDataContributor {

    /** Stable name for this data set, used as the key in the export document. */
    String dataSetName();

    /**
     * Everything this module holds about the user, in a form that serialises to JSON.
     *
     * <p>Returns an empty map when there is nothing.
     */
    Map<String, Object> exportFor(UserId userId);

    /**
     * Erases or anonymises everything this module holds about the user.
     *
     * <p>Must be idempotent — deletion is retried — and must not fail because there was
     * nothing to delete.
     */
    void eraseFor(UserId userId);
}
