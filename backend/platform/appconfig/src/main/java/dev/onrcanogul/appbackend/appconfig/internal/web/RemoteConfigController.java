package dev.onrcanogul.appbackend.appconfig.internal.web;

import dev.onrcanogul.appbackend.appconfig.api.dto.RemoteConfigResponse;
import dev.onrcanogul.appbackend.appconfig.api.model.AppVersion;
import dev.onrcanogul.appbackend.appconfig.api.port.RemoteConfigService;
import dev.onrcanogul.appbackend.core.api.context.ClientPlatform;
import dev.onrcanogul.appbackend.core.api.context.RequestContext;
import dev.onrcanogul.appbackend.core.api.context.RequestContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The first call a client makes, before sign-in.
 *
 * <p>Public on purpose: a client that must update, or that has hit a maintenance window,
 * needs to find that out without being able to authenticate. The platform and version come
 * from the request headers, so the client sends no parameters.
 */
@RestController
@RequestMapping("/api/v1/config")
@Tag(name = "App config", description = "Version gating, maintenance mode and feature flags")
public class RemoteConfigController {

    private final RemoteConfigService remoteConfigService;

    public RemoteConfigController(RemoteConfigService remoteConfigService) {
        this.remoteConfigService = remoteConfigService;
    }

    @GetMapping
    @Operation(summary = "Configuration for the calling client",
            description = "Reads X-Client-Platform and X-App-Version. Call this at launch, before sign-in.")
    public RemoteConfigResponse config() {
        ClientPlatform platform = RequestContextHolder.current()
                .map(RequestContext::platform)
                .orElse(ClientPlatform.UNKNOWN);
        AppVersion version = RequestContextHolder.current()
                .map(RequestContext::appVersion)
                .map(AppVersion::parseOrNull)
                .orElse(null);

        return RemoteConfigResponse.from(
                remoteConfigService.configFor(platform),
                !remoteConfigService.isSupported(platform, version));
    }
}
