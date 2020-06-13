package com.extraction.controllers;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;


@Controller
class MyErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError() {
        return "404.html"; // 该资源位于resources/static目录下
    }

    @Override
    public String getErrorPath() {
        return null;
    }
}
