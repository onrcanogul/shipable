package dev.onrcanogul.appbackend.admin.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @param value the new value, as text. Validated against the setting's declared type before
 *              it is stored, so a bad value is rejected here rather than misbehaving on
 *              every request afterwards
 * @param note  optional free text recorded with the change: why you turned it down, what
 *              you were responding to. Worth more than you expect a week later
 */
public record UpdateSettingRequest(
        @NotBlank @Size(max = 2048) String value,
        @Size(max = 190) String note) {
}
