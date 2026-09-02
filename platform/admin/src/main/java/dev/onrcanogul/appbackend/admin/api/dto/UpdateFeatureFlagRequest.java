package dev.onrcanogul.appbackend.admin.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * @param exposedToClient null leaves it as it is. Flags that decide server behaviour should
 *                        stay false - sending one to clients announces what you are about
 *                        to launch
 */
public record UpdateFeatureFlagRequest(
        @NotNull Boolean enabled,
        Boolean exposedToClient,
        @Size(max = 512) String description) {
}
