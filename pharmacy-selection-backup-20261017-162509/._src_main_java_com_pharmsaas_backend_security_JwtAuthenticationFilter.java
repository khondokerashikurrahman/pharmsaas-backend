
package com.pharmsaas.backend.security;

import com.pharmsaas.backend.modules.membership.entity.MembershipRole;
import com.pharmsaas.backend.modules.membership.entity.TenantMembership;
import com.pharmsaas.backend.modules.membership.repository.TenantMembershipRepository;
import com.pharmsaas.backend.modules.user.entity.SystemRole;
import com.pharmsaas.backend.modules.user.entity.User;
import com.pharmsaas.backend.modules.user.repository.UserRepository;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final TenantMembershipRepository membershipRepository;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserRepository userRepository,
            TenantMembershipRepository membershipRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        String method = request.getMethod();

        if (HttpMethod.OPTIONS.matches(method)) {
            return true;
        }

        return HttpMethod.POST.matches(method)
                && ("/api/auth/login".equals(path)
                || "/api/public/registration".equals(path));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7).trim();

        if (token.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Claims claims = jwtService.extractClaims(token);
            String subject = claims.getSubject();

            if (subject == null || subject.isBlank()) {
                rejectToken();
                filterChain.doFilter(request, response);
                return;
            }

            Long userId;

            try {
                userId = Long.valueOf(subject);
            } catch (NumberFormatException ex) {
                rejectToken();
                filterChain.doFilter(request, response);
                return;
            }

            Optional<User> optionalUser = userRepository.findById(userId);

            if (optionalUser.isEmpty()
                    || !optionalUser.get().isEnabled()) {
                rejectToken();
                filterChain.doFilter(request, response);
                return;
            }

            User user = optionalUser.get();

            // Global Super Admin does not need a tenant.
            if (user.getSystemRole() == SystemRole.SUPER_ADMIN) {
                if (jwtService.isSelectionToken(claims)
                        || claims.get("tenantId") != null) {
                    rejectToken();
                    filterChain.doFilter(request, response);
                    return;
                }

                setAuthentication(
                        request,
                        userId,
                        "ROLE_SUPER_ADMIN");
                filterChain.doFilter(request, response);
                return;
            }

            // Selection token can only access pharmacy-selection APIs.
            if (jwtService.isSelectionToken(claims)) {
                String path = request.getServletPath();
                String method = request.getMethod();

                boolean allowedEndpoint =
                        (HttpMethod.GET.matches(method)
                                && "/api/auth/my-pharmacies".equals(path))
                                || (HttpMethod.POST.matches(method)
                                && "/api/auth/select-tenant".equals(path));

                if (!allowedEndpoint || claims.get("tenantId") != null) {
                    rejectToken();
                    filterChain.doFilter(request, response);
                    return;
                }

                setAuthentication(
                        request,
                        userId,
                        "ROLE_TENANT_SELECTION");

                filterChain.doFilter(request, response);
                return;
            }

            // Every ordinary tenant token must identify its tenant.
            Long tenantId = jwtService.extractTenantId(token);

            if (tenantId == null || tenantId <= 0) {
                rejectToken();
                filterChain.doFilter(request, response);
                return;
            }

            Optional<TenantMembership> optionalMembership =
                    membershipRepository
                            .findByTenant_IdAndUser_IdAndActiveTrue(
                                    tenantId,
                                    userId);

            if (optionalMembership.isEmpty()) {
                rejectToken();
                filterChain.doFilter(request, response);
                return;
            }

            TenantMembership membership = optionalMembership.get();

            if (membership.getTenant() == null
                    || membership.getRole() == null) {
                rejectToken();
                filterChain.doFilter(request, response);
                return;
            }

            String status = membership.getTenant().getStatus();

            if (status == null
                    || !"Active".equalsIgnoreCase(status.trim())) {
                rejectToken();
                filterChain.doFilter(request, response);
                return;
            }

            String authority = mapRole(membership.getRole());

            if (authority == null) {
                rejectToken();
                filterChain.doFilter(request, response);
                return;
            }

            setAuthentication(request, userId, authority);

        } catch (JwtException | IllegalArgumentException ex) {
            rejectToken();
        }

        filterChain.doFilter(request, response);
    }

    private String mapRole(MembershipRole role) {
        return switch (role) {
            case TENANT_OWNER, PHARMACY_ADMIN -> "ROLE_TENANT_ADMIN";
            case MANAGER -> "ROLE_MANAGER";
            case STORE_KEEPER -> "ROLE_STORE_KEEPER";
            case PHARMACIST, STAFF, CASHIER -> "ROLE_STAFF";
            case SUPER_ADMIN -> null;
        };
    }

    private void setAuthentication(
            HttpServletRequest request,
            Long userId,
            String authority) {

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userId.toString(),
                        null,
                        List.of(new SimpleGrantedAuthority(authority)));

        authentication.setDetails(
                new WebAuthenticationDetailsSource()
                        .buildDetails(request));

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);
    }

    private void rejectToken() {
        SecurityContextHolder.clearContext();
    }
}
