package br.com.projetointegrador.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.cors.DefaultCorsProcessor;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiFiltro extends OncePerRequestFilter {
    private final CorsConfiguration cors = new CorsConfiguration();
    private final DefaultCorsProcessor processor = new DefaultCorsProcessor();

    public ApiFiltro(@Value("${stocka.frontend-origin}") String frontendOrigin) {
        cors.setAllowedOrigins(List.of(frontendOrigin));
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Content-Type", "X-STOCKA-Request"));
        cors.setAllowCredentials(true);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
        if (!processor.processRequest(cors, request, response) || CorsUtils.isPreFlightRequest(request)) {
            return;
        }
        // Cabecalho exigido nas escritas: formularios de outros sites nao conseguem envia-lo.
        // A origem autorizada e estrita; o navegador exige preflight para este cabecalho.
        if (!List.of("GET", "HEAD", "OPTIONS").contains(request.getMethod())
                && !"true".equals(request.getHeader("X-STOCKA-Request"))) {
            response.setStatus(403);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"mensagem\":\"Cabecalho X-STOCKA-Request obrigatorio.\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
