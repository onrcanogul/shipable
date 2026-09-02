package dev.onrcanogul.appbackend.notifications.internal.web;

import dev.onrcanogul.appbackend.identity.api.context.CurrentUserHolder;
import dev.onrcanogul.appbackend.notifications.api.dto.RegisterDeviceRequest;
import dev.onrcanogul.appbackend.notifications.api.port.DeviceTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Device registration for push. The user always comes from the bearer token. */
@RestController
@RequestMapping("/api/v1/devices")
@Tag(name = "Devices", description = "Push token registration")
public class DeviceController {

    private final DeviceTokenService deviceTokenService;

    public DeviceController(DeviceTokenService deviceTokenService) {
        this.deviceTokenService = deviceTokenService;
    }

    @PostMapping
    @Operation(summary = "Register or update this device's push token",
            description = "Upserts on device id. Call it on every launch: push tokens rotate.")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterDeviceRequest request) {
        deviceTokenService.register(
                CurrentUserHolder.require().userId(),
                request.deviceId(),
                request.token(),
                request.platform(),
                request.locale());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{deviceId}")
    @Operation(summary = "Stop sending push to this device",
            description = "Call it on sign-out, so the next person to use the phone does not get their pushes.")
    public ResponseEntity<Void> unregister(@PathVariable String deviceId) {
        deviceTokenService.unregister(CurrentUserHolder.require().userId(), deviceId);
        return ResponseEntity.noContent().build();
    }
}
