package by.imsha.aop.timing;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class TimingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        TimingService.startTime();
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        filterChain.doFilter(request, responseWrapper);
        TimingService.stopTime();

        // Достаем содержимое ответа после его отправки
        byte[] content = responseWrapper.getContentAsByteArray();

        // Добавляем заголовок к оригинальному HttpServletResponse
        response.addHeader("Server-Timing", TimingService.getResultServerTiming());

        // Записываем измененное содержимое обратно в ответ
        response.getOutputStream().write(content);
        response.flushBuffer();
    }
}
