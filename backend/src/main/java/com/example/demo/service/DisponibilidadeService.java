package com.example.demo.service;

import com.example.demo.enums.StatusAgendamento;
import com.example.demo.exception.RecursoNaoEncontradoException;
import com.example.demo.model.Agendamento;
import com.example.demo.model.Profissional;
import com.example.demo.model.Servico;
import com.example.demo.repository.AgendamentoRepository;
import com.example.demo.repository.ProfissionalRepository;
import com.example.demo.repository.ServicoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Calcula os horários REALMENTE disponíveis de um profissional para um serviço,
 * considerando: dia em que ele trabalha, horário de trabalho, duração do serviço
 * e agendamentos já existentes. Nunca retorna um horário indisponível.
 */
@Service
@RequiredArgsConstructor
public class DisponibilidadeService {

    private final ProfissionalRepository profissionalRepository;
    private final ServicoRepository servicoRepository;
    private final AgendamentoRepository agendamentoRepository;

    private static final Map<DayOfWeek, String> SIGLA_DIA = Map.of(
            DayOfWeek.MONDAY, "SEG",
            DayOfWeek.TUESDAY, "TER",
            DayOfWeek.WEDNESDAY, "QUA",
            DayOfWeek.THURSDAY, "QUI",
            DayOfWeek.FRIDAY, "SEX",
            DayOfWeek.SATURDAY, "SAB",
            DayOfWeek.SUNDAY, "DOM"
    );

    public List<LocalTime> horariosDisponiveis(Long barbeariaId, Long profissionalId, Long servicoId, LocalDate data) {

        Profissional profissional = profissionalRepository.findByIdAndBarbeariaId(profissionalId, barbeariaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Profissional não encontrado"));

        Servico servico = servicoRepository.findByIdAndBarbeariaId(servicoId, barbeariaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Serviço não encontrado"));

        if (!profissional.isAtivo() || !servico.isAtivo()) {
            return List.of();
        }

        String sigla = SIGLA_DIA.get(data.getDayOfWeek());
        if (profissional.getDiasTrabalho() == null || !profissional.getDiasTrabalho().contains(sigla)) {
            return List.of(); // profissional não trabalha nesse dia
        }

        if (profissional.getHorarioInicio() == null || profissional.getHorarioFim() == null) {
            return List.of();
        }

        LocalTime inicioExpediente = LocalTime.parse(profissional.getHorarioInicio());
        LocalTime fimExpediente = LocalTime.parse(profissional.getHorarioFim());
        int duracao = servico.getDuracaoMinutos();

        List<Agendamento> existentes = agendamentoRepository
                .findAllByProfissionalIdAndDataAndBarbeariaId(profissionalId, data, barbeariaId)
                .stream()
                .filter(a -> a.getStatus() != StatusAgendamento.CANCELADO)
                .toList();

        List<LocalTime> disponiveis = new ArrayList<>();
        LocalTime candidato = inicioExpediente;

        while (!candidato.plusMinutes(duracao).isAfter(fimExpediente)) {
            LocalTime candidatoFim = candidato.plusMinutes(duracao);
            LocalTime finalCandidato = candidato;

            boolean conflita = existentes.stream().anyMatch(a ->
                    finalCandidato.isBefore(a.getHoraFim()) && candidatoFim.isAfter(a.getHoraInicio()));

            if (!conflita) {
                disponiveis.add(candidato);
            }

            candidato = candidato.plusMinutes(duracao);
        }

        return disponiveis;
    }
}
