package dev.onrcanogul.appbackend.admin.internal.web;

import dev.onrcanogul.appbackend.admin.api.dto.UpdateSettingRequest;
import dev.onrcanogul.appbackend.appconfig.api.model.SettingView;
import dev.onrcanogul.appbackend.appconfig.api.port.SettingsAdminService;
import dev.onrcanogul.appbackend.core.api.context.RequestContext;
import dev.onrcanogul.appbackend.core.api.context.RequestContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Reading and changing runtime settings.
 *
 * <p>The listing is self-describing — key, type, current value, boot default, whether it is
 * overridden, and what it does — so whoever is on call does not have to already know that
 * {@code core.rate-limit.window} takes {@code 1m} and not {@code 60}.
 *
 * <p>A change takes effect on the next request on this instance, and within one refresh
 * interval on the others. No redeploy, no restart.
 */
@RestController
@RequestMapping("/api/admin/v1/settings")
@Tag(name = "Admin: settings", description = "Runtime settings, changeable without a deploy")
public class AdminSettingsController {

    private final SettingsAdminService settings;

    public AdminSettingsController(SettingsAdminService settings) {
        this.settings = settings;
    }

    @GetMapping
    @Operation(summary = "Every known setting, with its current and default value")
    public List<SettingView> list() {
        return settings.list();
    }

    @GetMapping("/{key}")
    @Operation(summary = "One setting")
    public SettingView get(@PathVariable String key) {
        return settings.get(key);
    }

    @PutMapping("/{key}")
    @Operation(summary = "Override a setting",
            description = "Validated against the setting's declared type. An unknown key is a 404, "
                    + "so the table cannot fill with typos that silently do nothing.")
    public SettingView set(@PathVariable String key, @Valid @RequestBody UpdateSettingRequest request) {
        return settings.set(key, request.value(), auditActor(request.note()));
    }

    @DeleteMapping("/{key}")
    @Operation(summary = "Drop the override so the boot default applies again")
    public SettingView reset(@PathVariable String key) {
        return settings.reset(key, auditActor(null));
    }

    /**
     * Who to record against the change.
     *
     * <p>The admin API authenticates a key, not a person, so the best available identity is
     * the caller's address plus whatever note they left. Honest about its limits: if you
     * need to know <i>who</i>, give each operator their own key or move to admin accounts.
     */
    private String auditActor(String note) {
        String ip = RequestContextHolder.current()
                .map(RequestContext::clientIp)
                .orElse("unknown");
        String actor = "admin-key@" + ip;
        return note == null || note.isBlank() ? actor : actor + " (" + note.trim() + ")";
    }
}
