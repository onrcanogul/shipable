package dev.onrcanogul.appbackend.identity.internal.web;

import dev.onrcanogul.appbackend.core.api.model.Result;
import dev.onrcanogul.appbackend.identity.api.context.CurrentUser;
import dev.onrcanogul.appbackend.identity.api.context.CurrentUserHolder;
import dev.onrcanogul.appbackend.identity.api.dto.AnonymousSignInRequest;
import dev.onrcanogul.appbackend.identity.api.dto.AuthenticationResponse;
import dev.onrcanogul.appbackend.identity.api.dto.LinkAnonymousAccountRequest;
import dev.onrcanogul.appbackend.identity.api.dto.RefreshTokenRequest;
import dev.onrcanogul.appbackend.identity.api.dto.SocialSignInRequest;
import dev.onrcanogul.appbackend.identity.api.model.AuthenticationResult;
import dev.onrcanogul.appbackend.identity.api.port.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The auth endpoints.
 *
 * <p>Lives in {@code identity} rather than in {@code host} so the module is self-contained:
 * deleting the module takes its endpoints with it, and the host does not accumulate
 * knowledge of every feature.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Sign-in, session refresh and anonymous accounts")
public class AuthController {

    private final AuthenticationService authenticationService;

    public AuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/social")
    @Operation(summary = "Sign in with an Apple or Google identity token")
    public AuthenticationResponse signInWithProvider(@Valid @RequestBody SocialSignInRequest request) {
        return respond(authenticationService.signInWithProvider(request.provider(), request.identityToken()));
    }

    @PostMapping("/anonymous")
    @Operation(summary = "Start or resume an anonymous device session",
            description = "Lets someone use the app before signing in. Call /link later to keep their data.")
    public AuthenticationResponse signInAnonymously(@Valid @RequestBody AnonymousSignInRequest request) {
        return AuthenticationResponse.from(authenticationService.signInAnonymously(request.deviceId()));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Exchange a refresh token for a new session")
    public AuthenticationResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return respond(authenticationService.refresh(request.refreshToken()));
    }

    @PostMapping("/link")
    @Operation(summary = "Attach a provider account to the current anonymous session",
            description = "The anonymous account is taken from the bearer token, never from the request body.")
    public AuthenticationResponse linkAnonymousAccount(@Valid @RequestBody LinkAnonymousAccountRequest request) {
        CurrentUser current = CurrentUserHolder.require();
        return respond(authenticationService.linkAnonymousAccount(
                current.userId(), request.provider(), request.identityToken()));
    }

    @PostMapping("/signout")
    @Operation(summary = "Revoke one refresh token")
    public ResponseEntity<Void> signOut(@Valid @RequestBody RefreshTokenRequest request) {
        authenticationService.signOut(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/signout-everywhere")
    @Operation(summary = "Revoke every refresh token for the current user")
    public ResponseEntity<Void> signOutEverywhere() {
        authenticationService.signOutEverywhere(CurrentUserHolder.require().userId());
        return ResponseEntity.noContent().build();
    }

    /**
     * A rejected token is 401, not 400: the request was well formed, the credentials were
     * not accepted.
     */
    private AuthenticationResponse respond(Result<AuthenticationResult> result) {
        return AuthenticationResponse.from(result.orElseThrow(HttpStatus.UNAUTHORIZED));
    }
}
