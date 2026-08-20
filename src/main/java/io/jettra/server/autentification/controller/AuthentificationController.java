package io.jettra.server.autentification.controller;

import io.jettra.core.inject.annotation.Inject;
import io.jettra.jwt.JettraJWT;
import io.jettra.jwt.request.LoginResponse;
import io.jettra.rest.core.Response;
import io.jettra.rest.annotations.GET;
import io.jettra.rest.annotations.Path;
import io.jettra.rest.annotations.PermitAll;
import io.jettra.rest.annotations.Produces;
import io.jettra.rest.annotations.QueryParam;
import io.jettra.server.autentification.entity.JCredential;
import io.jettra.server.autentification.entity.JRole;
import io.jettra.server.autentification.entity.JUser;
import io.jettra.server.autentification.repository.JCredentialRepository;
import io.jettra.server.autentification.repository.JUserRepository;
import io.jettra.server.config.JettraConfigProperty;
import io.jettra.server.discoverer.Discovered;
import io.jettra.server.openapi.annotations.OpenApi;
import io.jettra.server.openapi.annotations.Operation;
import io.jettra.server.openapi.annotations.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Path("/autentification/auth")
@OpenApi(title = "Authentication API", version = "v1.0", description = "API for Authentication")
@Discovered
public class AuthentificationController {

    @JettraConfigProperty(name = "server.JWT_SECRET")
    private String JWT_SECRET;

    @JettraConfigProperty(name = "server.JWT_EXPIRATION")
    private Integer JWT_EXPIRATION;

    @Inject
    private JCredentialRepository jCredentialRepository;
        
    @Inject
    private JUserRepository jUserRepository;

    @GET
    @Produces("application/json")
    @PermitAll
    @Operation(summary = "Login and get JWT", description = "Authenticates a user and returns a Bearer token")
    public Response login(
            @Parameter(name = "username", description = "Username for authentication", required = true) @QueryParam("username") String username,
            @Parameter(name = "password", description = "Password for authentication", required = true) @QueryParam("password") String password) {

        if (username == null || password == null || username.trim().isEmpty() || password.trim().isEmpty()) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("{\"error\":\"Invalid credentials\"}").build();
        }

        if (jCredentialRepository == null) {
            jCredentialRepository = new io.jettra.server.autentification.repository.JCredentialRepositoryImpl();
        }
        if (jUserRepository == null) {
            jUserRepository = new io.jettra.server.autentification.repository.JUserRepositoryImpl();
        }

        Optional<JCredential> optCred = jCredentialRepository.findByUsernamePassword(username, password);

        if (optCred.isEmpty()) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("{\"error\":\"Invalid credentials\"}").build();
        }

        JUser user = optCred.get().jUser();
        if (user == null && optCred.get().id() != null) {
            Optional<JUser> optUser = jUserRepository.findById(optCred.get().id());
            if (optUser.isPresent()) {
                user = optUser.get();
            }
        }

        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("{\"error\":\"User not found in UserRepository\"}").build();
        }

        List<String> roleNames = new ArrayList<>();
        if (user.jRoles() != null) {
            for (JRole r : user.jRoles()) {
                roleNames.add(r.name());
            }
        }

        String secret = (JWT_SECRET != null && !JWT_SECRET.isBlank()) ? JWT_SECRET : "default_secret_key_jettra_rest_2026";
        long expiration = (JWT_EXPIRATION != null && JWT_EXPIRATION > 0) ? JWT_EXPIRATION : 3600000L;

        JettraJWT jwt = new JettraJWT(secret, expiration);
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", roleNames);
        String token = "Bearer " + jwt.generateToken(claims, username);

        return Response.ok(new LoginResponse(token)).build();
    }
}
