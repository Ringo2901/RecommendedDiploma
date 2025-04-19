package by.bsuir.aleksandrov.recommendeddiploma.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.util.Map;

@Controller
public class CastomErrorController implements ErrorController {

    private final ErrorAttributes errorAttributes;

    public CastomErrorController(ErrorAttributes errorAttributes) {
        super();
        this.errorAttributes = errorAttributes;
    }

    @GetMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        // Оборачиваем HttpServletRequest
        WebRequest webRequest = new ServletWebRequest(request);

        Map<String, Object> errors = errorAttributes.getErrorAttributes(
                webRequest, ErrorAttributeOptions.of(ErrorAttributeOptions.Include.MESSAGE));

        model.addAttribute("status", errors.get("status"));
        model.addAttribute("error", errors.get("error"));
        model.addAttribute("message", errors.get("message"));

        return "errors/error-page";
    }
}
