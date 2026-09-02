package dev.onrcanogul.appbackend.appconfig.api.model;

import dev.onrcanogul.appbackend.core.api.port.DurationParser;

/**
 * What a setting's value means, so the admin API can validate a write and a UI can render
 * the right control.
 *
 * <p>Validation happens on write rather than on read. A bad value stored now is a bad value
 * read on every request afterwards, and by then the person who typed it has moved on.
 */
public enum SettingType {

    STRING {
        @Override
        public void validate(String value) {
            // Any string is a valid string.
        }
    },

    INTEGER {
        @Override
        public void validate(String value) {
            Integer.parseInt(value.trim());
        }
    },

    BOOLEAN {
        @Override
        public void validate(String value) {
            String normalised = value.trim().toLowerCase(java.util.Locale.ROOT);
            if (!normalised.equals("true") && !normalised.equals("false")) {
                // Boolean.parseBoolean quietly turns anything unrecognised into false,
                // which would silently disable whatever this setting controls.
                throw new IllegalArgumentException("Expected true or false, got: " + value);
            }
        }
    },

    DURATION {
        @Override
        public void validate(String value) {
            DurationParser.parse(value);
        }
    };

    /** @throws RuntimeException when the value does not fit this type */
    public abstract void validate(String value);
}
