package dev.onrcanogul.appbackend.privacy.internal.web;

import dev.onrcanogul.appbackend.identity.api.context.CurrentUserHolder;
import dev.onrcanogul.appbackend.privacy.api.dto.DeletionStatusResponse;
import dev.onrcanogul.appbackend.privacy.api.model.DataExport;
import dev.onrcanogul.appbackend.privacy.api.port.AccountDeletionService;
import dev.onrcanogul.appbackend.privacy.api.port.DataExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Account deletion and data export.
 *
 * <p>Every endpoint requires a <b>registered</b> account, not just any session. Letting an
 * anonymous device session delete an account or download an export would mean anyone
 * holding the phone can do both.
 */
@RestController
@RequestMapping("/api/v1/account")
@Tag(name = "Account privacy", description = "Account deletion and data export")
public class AccountPrivacyController {

    private final AccountDeletionService deletionService;
    private final DataExportService exportService;

    public AccountPrivacyController(AccountDeletionService deletionService, DataExportService exportService) {
        this.deletionService = deletionService;
        this.exportService = exportService;
    }

    @PostMapping("/deletion")
    @Operation(summary = "Request account deletion",
            description = "Schedules erasure after a grace period during which it can be cancelled. "
                    + "Apple and Google both require this to exist inside the app.")
    public DeletionStatusResponse requestDeletion() {
        return DeletionStatusResponse.from(
                deletionService.requestDeletion(CurrentUserHolder.requireRegistered().userId()));
    }

    @DeleteMapping("/deletion")
    @Operation(summary = "Cancel a pending deletion",
            description = "Only works during the grace period.")
    public DeletionStatusResponse cancelDeletion() {
        return DeletionStatusResponse.from(
                deletionService.cancelDeletion(CurrentUserHolder.requireRegistered().userId()));
    }

    @GetMapping("/deletion")
    @Operation(summary = "Status of a deletion request")
    public ResponseEntity<DeletionStatusResponse> deletionStatus() {
        return deletionService.statusOf(CurrentUserHolder.requireRegistered().userId())
                .map(DeletionStatusResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/export")
    @Operation(summary = "Everything we hold about this user",
            description = "Assembled from every module that stores user data.")
    public DataExport export() {
        return exportService.exportFor(CurrentUserHolder.requireRegistered().userId());
    }
}
