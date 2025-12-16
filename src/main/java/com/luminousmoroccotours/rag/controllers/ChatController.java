package com.luminousmoroccotours.rag.controllers;

import com.luminousmoroccotours.rag.services.RagService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final RagService ragService;

    @PostMapping
    public Map<String, String> chat(@RequestBody Map<String, String> request) {
        String query = request.get("query");
        String answer = ragService.answer(query);
        return Map.of("answer", answer);
    }
}
