package com.skim.dto;

import lombok.Data;

@Data
public class SkimRequest {
    private String content;
    private String operation;
    private String tone; // only used when operation = "rewrite"
}
