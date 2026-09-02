package dev.onrcanogul.appbackend.appconfig.api.port;

import dev.onrcanogul.appbackend.appconfig.api.model.AppVersion;
import dev.onrcanogul.appbackend.appconfig.api.model.RemoteConfig;
import dev.onrcanogul.appbackend.core.api.context.ClientPlatform;

/** What the client asks for, and what the version gate asks internally. */
public interface RemoteConfigService {

    RemoteConfig configFor(ClientPlatform platform);

    /**
     * Whether a client at this version may still call the API.
     *
     * @param version null when the client sent no version header. Treated as supported —
     *                a version gate that rejects unknown clients locks out every caller
     *                that predates the header, including your own tooling
     */
    boolean isSupported(ClientPlatform platform, AppVersion version);
}
