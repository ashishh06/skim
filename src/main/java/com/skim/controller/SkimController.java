package com.skim.controller;

import com.skim.dto.SkimRequest;
import com.skim.service.SkimService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/skim")
@CrossOrigin(origins = "*")
@AllArgsConstructor
public class SkimController {

    private final SkimService skimService;

    @PostMapping("/process")
    public ResponseEntity<String> processContent(@RequestBody SkimRequest request){
        String result = skimService.processContent(request);
        return ResponseEntity.ok(result);
    }
}
