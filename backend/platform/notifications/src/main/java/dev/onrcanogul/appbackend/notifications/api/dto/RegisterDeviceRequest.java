package dev.onrcanogul.appbackend.notifications.api.dto;

import dev.onrcanogul.appbackend.notifications.api.model.PushPlatform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterDeviceRequest(
        @NotBlank @Size(max = 255) String deviceId,
        @NotBlank @Size(max = 512) String token,
        @NotNull PushPlatform platform,
        @Size(max = 16) String locale) {
}
