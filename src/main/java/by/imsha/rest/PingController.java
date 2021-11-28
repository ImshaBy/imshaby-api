package by.imsha.rest;

import by.imsha.domain.Ping;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.RequestContextUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Locale;

/**
 * @author Alena Misan
 */


@RestController
@RequestMapping(value = "/ping1")
public class PingController extends AbstractRestHandler {

    public static Ping pingObject = new Ping("ping");

    @RequestMapping(value = "",
            method = RequestMethod.GET,
            produces = {MediaType.APPLICATION_JSON_VALUE, "application/xml"})
    @ResponseStatus(HttpStatus.OK)
    public @ResponseBody Ping ping(HttpServletRequest request, HttpServletResponse response) {
        log.info("ping-o execution...");

        Locale locale = RequestContextUtils.getLocale(request);
        return new Ping("locale: " + locale);
    }



}
