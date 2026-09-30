package com.agrinexus.security;

import com.agrinexus.entity.User;
import com.agrinexus.repository.UserRepository;
import com.agrinexus.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter
        extends OncePerRequestFilter {

    private final JwtService jwtService;

    private final UserRepository userRepository;


    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authorizationHeader =
                request.getHeader("Authorization");


        /*
         * ---------------------------------------------------------
         * No Authorization header
         * ---------------------------------------------------------
         *
         * Let SecurityConfig decide whether the endpoint is public
         * or protected.
         */
        if (
                authorizationHeader == null
                        || !authorizationHeader.startsWith(
                        "Bearer "
                )
        ) {

            filterChain.doFilter(
                    request,
                    response
            );

            return;
        }


        /*
         * ---------------------------------------------------------
         * Extract Bearer token
         * ---------------------------------------------------------
         */
        String token =
                authorizationHeader.substring(
                        7
                ).trim();


        /*
         * Empty Bearer token.
         */
        if (token.isBlank()) {

            sendUnauthorized(
                    response,
                    "Bearer token is required"
            );

            return;
        }


        try {

            /*
             * -----------------------------------------------------
             * Extract user ID from JWT
             * -----------------------------------------------------
             */
            String userId =
                    jwtService.extractUserId(
                            token
                    );


            /*
             * -----------------------------------------------------
             * Authenticate only when no authentication already
             * exists in the current request.
             * -----------------------------------------------------
             */
            if (
                    userId != null
                            && !userId.isBlank()
                            && SecurityContextHolder
                            .getContext()
                            .getAuthentication() == null
            ) {

                UUID parsedUserId;

                try {

                    parsedUserId =
                            UUID.fromString(
                                    userId
                            );

                } catch (IllegalArgumentException ex) {

                    sendUnauthorized(
                            response,
                            "Invalid authentication token"
                    );

                    return;
                }


                /*
                 * -------------------------------------------------
                 * Load user from database
                 * -------------------------------------------------
                 */
                User user =
                        userRepository
                                .findById(
                                        parsedUserId
                                )
                                .orElse(null);


                /*
                 * -------------------------------------------------
                 * User must exist and remain active.
                 * -------------------------------------------------
                 */
                if (user == null) {

                    sendUnauthorized(
                            response,
                            "User associated with token was not found"
                    );

                    return;
                }


                if (
                        user.getActive() == null
                                || !user.getActive()
                ) {

                    sendUnauthorized(
                            response,
                            "User account is inactive"
                    );

                    return;
                }


                /*
                 * -------------------------------------------------
                 * Validate signature + identity + expiration.
                 * -------------------------------------------------
                 */
                if (
                        !jwtService.isTokenValid(
                                token,
                                user
                        )
                ) {

                    sendUnauthorized(
                            response,
                            "Invalid or expired authentication token"
                    );

                    return;
                }


                /*
                 * -------------------------------------------------
                 * Build Spring Security authorities.
                 * -------------------------------------------------
                 */
                String role =
                        user.getRole() != null
                                ? user.getRole().name()
                                : "FARMER";


                List<SimpleGrantedAuthority>
                        authorities =
                        List.of(
                                new SimpleGrantedAuthority(
                                        "ROLE_" + role
                                )
                        );


                /*
                 * -------------------------------------------------
                 * Create authenticated principal.
                 * -------------------------------------------------
                 */
                UsernamePasswordAuthenticationToken
                        authentication =
                        new UsernamePasswordAuthenticationToken(
                                user,
                                null,
                                authorities
                        );


                authentication.setDetails(
                        new WebAuthenticationDetailsSource()
                                .buildDetails(
                                        request
                                )
                );


                SecurityContextHolder
                        .getContext()
                        .setAuthentication(
                                authentication
                        );
            }


            /*
             * Continue request after successful authentication.
             */
            filterChain.doFilter(
                    request,
                    response
            );

        } catch (Exception ex) {

            /*
             * -----------------------------------------------------
             * Any JWT parsing/signature/expiration error means the
             * request is unauthenticated.
             *
             * Return 401 immediately instead of allowing the request
             * to become an anonymous request and potentially produce
             * 403 Forbidden.
             * -----------------------------------------------------
             */
            SecurityContextHolder
                    .clearContext();


            sendUnauthorized(
                    response,
                    "Invalid or expired authentication token"
            );
        }
    }


    /*
     * =========================================================
     * Standard 401 response
     * =========================================================
     */
    private void sendUnauthorized(
            HttpServletResponse response,
            String message
    ) throws IOException {

        if (response.isCommitted()) {
            return;
        }


        response.setStatus(
                HttpStatus.UNAUTHORIZED.value()
        );


        response.setContentType(
                "application/json"
        );


        response.setCharacterEncoding(
                "UTF-8"
        );


        String json =
                "{"
                        + "\"status\":401,"
                        + "\"error\":\"Unauthorized\","
                        + "\"message\":\""
                        + escapeJson(message)
                        + "\""
                        + "}";


        response.getWriter()
                .write(
                        json
                );
    }


    /*
     * =========================================================
     * Minimal JSON escaping
     * =========================================================
     */
    private String escapeJson(
            String value
    ) {

        if (value == null) {

            return "";
        }


        return value
                .replace(
                        "\\",
                        "\\\\"
                )
                .replace(
                        "\"",
                        "\\\""
                );
    }
}