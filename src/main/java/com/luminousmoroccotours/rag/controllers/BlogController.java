package com.luminousmoroccotours.rag.controllers;

import com.luminousmoroccotours.rag.services.BlogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/blog")
@RequiredArgsConstructor
public class BlogController {

    private final BlogService blogService;

    @PostMapping("/generate")
    public Map<String, String> generate(@RequestBody Map<String, String> request) {
        String topic = request.get("topic");
        String blogContent = blogService.generateBlog(topic);
        return Map.of("content", blogContent);
    }
}
