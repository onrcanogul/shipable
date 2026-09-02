package dev.onrcanogul.appbackend.admin.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Version gating for one client platform.
 *
 * @param minimumSupportedVersion clients below this get 426 and must update. Raising it
 *                                locks out everyone on an older build, so it is validated
 *                                as a real version rather than trusted as text
 */
public record UpdatePlatformConfigRequest(
        @NotBlank @Size(max = 32) String minimumSupportedVersion,
        @NotBlank @Size(max = 32) String latestVersion,
        @Size(max = 512) String updateUrl) {
}
