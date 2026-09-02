package dev.onrcanogul.appbackend.identity.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @param deviceId a stable, client-generated device identifier. On iOS use
 *                 identifierForVendor; on Android a locally generated UUID kept in
 *                 storage. Never a hardware id - those are restricted and change across
 *                 OS versions
 */
public record AnonymousSignInRequest(
        @NotBlank @Size(max = 255) String deviceId) {
}
