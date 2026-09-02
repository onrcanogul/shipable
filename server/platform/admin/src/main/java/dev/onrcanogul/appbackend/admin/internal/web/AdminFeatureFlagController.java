package dev.onrcanogul.appbackend.admin.internal.web;

import dev.onrcanogul.appbackend.admin.api.dto.UpdateFeatureFlagRequest;
import dev.onrcanogul.appbackend.appconfig.api.model.FeatureFlagView;
import dev.onrcanogul.appbackend.appconfig.api.port.FeatureFlagAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Turning features on and off without shipping a build.
 *
 * <p>The reason this exists: the feature you most urgently need to disable is the one
 * costing you money or crashing, and waiting on App Store review is not an option.
 */
@RestController
@RequestMapping("/api/admin/v1/feature-flags")
@Tag(name = "Admin: feature flags", description = "Server-side switches")
public class AdminFeatureFlagController {

    private final FeatureFlagAdminService flags;

    public AdminFeatureFlagController(FeatureFlagAdminService flags) {
        this.flags = flags;
    }

    @GetMapping
    @Operation(summary = "Every flag")
    public List<FeatureFlagView> list() {
        return flags.list();
    }

    @PutMapping("/{key}")
    @Operation(summary = "Create or update a flag",
            description = "Upsert: a flag is usually first written at the moment you need it off. "
                    + "Leaving exposedToClient null keeps the current exposure, so flipping a "
                    + "server-only flag cannot accidentally publish it to clients.")
    public FeatureFlagView upsert(
            @PathVariable String key, @Valid @RequestBody UpdateFeatureFlagRequest request) {
        return flags.upsert(key, request.enabled(), request.exposedToClient(), request.description());
    }

    @DeleteMapping("/{key}")
    @Operation(summary = "Delete a flag",
            description = "Callers fall back to the default they pass to isEnabled.")
    public ResponseEntity<Void> delete(@PathVariable String key) {
        flags.delete(key);
        return ResponseEntity.noContent().build();
    }
}
