package by.imsha.rest;

import by.imsha.domain.Ping;
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
}
