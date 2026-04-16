package com.consult.controller;

import com.consult.dto.LoginRequest;
import com.consult.dto.MeResponse;
import com.consult.exception.BadRequestException;
import com.consult.security.AppUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authManager;
    private final SecurityContextRepository contextRepo;

    public AuthController(AuthenticationManager authManager, SecurityContextRepository contextRepo) {
        this.authManager = authManager;
        this.contextRepo = contextRepo;
    }

    @PostMapping("/login")
    public MeResponse login(@Valid @RequestBody LoginRequest req,
                            HttpServletRequest request,
                            HttpServletResponse response) {
        try {
            UsernamePasswordAuthenticationToken token =
                UsernamePasswordAuthenticationToken.unauthenticated(req.email(), req.password());
            Authentication auth = authManager.authenticate(token);

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            SecurityContextHolder.setContext(context);
            contextRepo.saveContext(context, request, response);

            AppUserDetails principal = (AppUserDetails) auth.getPrincipal();
            return MeResponse.from(principal.getUser());
        } catch (AuthenticationException e) {
            throw new BadRequestException("Невалиден email или парола.");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<MeResponse> me(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof AppUserDetails principal)) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(MeResponse.from(principal.getUser()));
    }
}
