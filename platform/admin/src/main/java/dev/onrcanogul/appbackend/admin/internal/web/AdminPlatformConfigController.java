package dev.onrcanogul.appbackend.admin.internal.web;

import dev.onrcanogul.appbackend.admin.api.dto.UpdatePlatformConfigRequest;
import dev.onrcanogul.appbackend.appconfig.api.model.PlatformConfigView;
import dev.onrcanogul.appbackend.appconfig.api.port.PlatformConfigAdminService;
import dev.onrcanogul.appbackend.core.api.context.ClientPlatform;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Version gating per platform.
 *
 * <p><b>The most destructive thing in this API.</b> Raising the minimum version locks out
 * every user on an older build at once, with no gradual rollout and no undo beyond setting
 * it back. Both versions are parsed before anything is stored, and a minimum above the
 * latest is refused outright.
 */
@RestController
@RequestMapping("/api/admin/v1/platform-config")
@Tag(name = "Admin: version gating", description = "Minimum supported version and force update")
public class AdminPlatformConfigController {

    private final PlatformConfigAdminService platformConfig;

    public AdminPlatformConfigController(PlatformConfigAdminService platformConfig) {
        this.platformConfig = platformConfig;
    }

    @GetMapping
    @Operation(summary = "Version gating for every platform")
    public List<PlatformConfigView> list() {
        return platformConfig.list();
    }

    @PutMapping("/{platform}")
    @Operation(summary = "Set the minimum and latest version for one platform",
            description = "Clients below the minimum get 426 on their next call. "
                    + "Check what is actually live in the stores before raising it.")
    public PlatformConfigView update(
            @PathVariable ClientPlatform platform, @Valid @RequestBody UpdatePlatformConfigRequest request) {
        return platformConfig.update(
                platform, request.minimumSupportedVersion(), request.latestVersion(), request.updateUrl());
    }
}
