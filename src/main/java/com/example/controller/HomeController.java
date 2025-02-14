package com.example.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Javadoc comment.
 */
@Controller
public class HomeController {

  @GetMapping("/")
  public String showIndex() {
    return "index";
  }

}
