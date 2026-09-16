package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PerguntaAssistenteRequest {

    @NotBlank(message = "Digite uma pergunta.")
    private String pergunta;
}
