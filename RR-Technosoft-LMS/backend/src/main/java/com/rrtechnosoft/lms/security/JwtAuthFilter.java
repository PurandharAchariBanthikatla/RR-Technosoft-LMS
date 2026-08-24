package com.rrtechnosoft.lms.security;

import com.rrtechnosoft.lms.entity.User;
import com.rrtechnosoft.lms.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);

        if (jwtService.isValid(token)) {
            Claims claims = jwtService.parseClaims(token);
            UUID userId = UUID.fromString(claims.getSubject());

            Optional<User> userOpt = userRepository.findById(userId);
            // Re-check live account state on every request: a still-valid (unexpired) access
            // token issued before an admin suspended/locked this account must stop working
            // immediately, not linger for up to its 15-minute lifetime.
            boolean userUsable = userOpt.isPresent()
                    && userOpt.get().getStatus() == com.rrtechnosoft.lms.entity.enums.AccountStatus.ACTIVE
                    && !userOpt.get().isLocked();

            if (userUsable && SecurityContextHolder.getContext().getAuthentication() == null) {
                User user = userOpt.get();
                UserPrincipal principal = new UserPrincipal(user);

                var authToken = new UsernamePasswordAuthenticationToken(
                        principal, null, principal.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}
