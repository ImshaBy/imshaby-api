package by.imsha.rest;

import by.imsha.domain.Ping;
import io.sentry.Sentry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.RequestContextUtils;

import java.util.Locale;

/**
 * @author Alena Misan
 */


@RestController
@RequestMapping(value = "/")
public class PingController {

    @GetMapping
    public ResponseEntity<Ping> ping(HttpServletRequest request, HttpServletResponse response) {
        Locale locale = RequestContextUtils.getLocale(request);
        return ResponseEntity.ok(
                new Ping("locale: " + locale)
        );
    }

    @GetMapping("/sentry-test")
    public ResponseEntity<Ping> sentryTest() {
        try {
            throw new Exception("This is a test exception for Sentry integration verification.");
        } catch (Exception e) {
            Sentry.captureException(e);
            return ResponseEntity.ok(
                    new Ping("Sentry test exception sent! Check your Sentry dashboard.")
            );
        }
    }
}
