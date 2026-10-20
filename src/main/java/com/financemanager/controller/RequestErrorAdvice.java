package com.financemanager.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import java.util.Map;

@ControllerAdvice
public class RequestErrorAdvice {
    @ExceptionHandler(IllegalArgumentException.class)
    public Object invalidRequest(IllegalArgumentException exception, HttpServletRequest request) {
        if (request.getRequestURI().startsWith("/api/")) {
            return ResponseEntity.badRequest().body(Map.of("message", "That information could not be found or was not accepted. Refresh and try again."));
        }
        ModelAndView page = new ModelAndView("error/400");
        page.setStatus(HttpStatus.BAD_REQUEST);
        page.addObject("errorTitle", "We couldn’t find that information.");
        page.addObject("errorDetail", "It may have changed or is no longer available. Go back and refresh, then try again.");
        return page;
    }
}
