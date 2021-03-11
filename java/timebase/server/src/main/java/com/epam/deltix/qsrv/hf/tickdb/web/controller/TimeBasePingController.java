package com.epam.deltix.qsrv.hf.tickdb.web.controller;

import com.epam.deltix.qsrv.hf.tickdb.http.AbstractHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * @author Daniil Yarmalkevich
 * Date: 11/11/2019
 */
@Controller
@RequestMapping(value = "/")
public class TimeBasePingController {

    @RequestMapping(value = "/ping", method = RequestMethod.GET)
    public ResponseEntity<String> startPage() {
        try {
            if (AbstractHandler.TDB.isOpen()) {
                return ResponseEntity.ok().body("TimeBase is ready.");
            }
        } catch (Exception ignored) {
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("TimeBase is not ready.");
    }

}
