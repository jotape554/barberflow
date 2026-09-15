package com.example.demo.service;

import com.example.demo.enums.StatusAgendamento;
import com.example.demo.model.Agendamento;
import com.example.demo.model.Profissional;
import com.example.demo.model.Servico;
import com.example.demo.repository.AgendamentoRepository;
import com.example.demo.repository.ProfissionalRepository;
import com.example.demo.repository.ServicoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * O cliente final nunca pode ver, e muito menos conseguir marcar, um horário que já está
 * ocupado — é a regra mais crítica do agendamento público. Estes testes cobrem exatamente
 * essa garantia, sem precisar de banco de dados nem de contexto Spring.
 */
@ExtendWith(MockitoExtension.class)
class DisponibilidadeServiceTest {

    @Mock private ProfissionalRepository profissionalRepository;
    @Mock private ServicoRepository servicoRepository;
    @Mock private AgendamentoRepository agendamentoRepository;

    @InjectMocks
    private DisponibilidadeService disponibilidadeService;

    private static final Long BARBEARIA_ID = 1L;
    private static final Long PROFISSIONAL_ID = 10L;
    private static final Long SERVICO_ID = 20L;
    private static final LocalDate QUARTA_FEIRA = LocalDate.of(2026, 9, 16); // quarta-feira

    private Profissional profissionalPadrao() {
        return Profissional.builder()
                .id(PROFISSIONAL_ID)
                .nome("Carlos")
                .ativo(true)
                .diasTrabalho("SEG,TER,QUA,QUI,SEX")
                .horarioInicio("09:00")
                .horarioFim("12:00")
                .build();
    }

    private Servico servicoPadrao(int duracaoMinutos) {
        return Servico.builder().id(SERVICO_ID).nome("Corte").ativo(true).duracaoMinutos(duracaoMinutos).build();
    }

    @Test
    void deveListarTodosOsSlotsQuandoNaoHaAgendamentoNenhum() {
        when(profissionalRepository.findByIdAndBarbeariaId(PROFISSIONAL_ID, BARBEARIA_ID))
                .thenReturn(Optional.of(profissionalPadrao()));
        when(servicoRepository.findByIdAndBarbeariaId(SERVICO_ID, BARBEARIA_ID))
                .thenReturn(Optional.of(servicoPadrao(30)));
        when(agendamentoRepository.findAllByProfissionalIdAndDataAndBarbeariaId(PROFISSIONAL_ID, QUARTA_FEIRA, BARBEARIA_ID))
                .thenReturn(List.of());

        List<LocalTime> disponiveis = disponibilidadeService.horariosDisponiveis(BARBEARIA_ID, PROFISSIONAL_ID, SERVICO_ID, QUARTA_FEIRA);

        // expediente 09:00-12:00, serviço de 30min => 09:00, 09:30, 10:00, 10:30, 11:00, 11:30
        assertThat(disponiveis).containsExactly(
                LocalTime.of(9, 0), LocalTime.of(9, 30), LocalTime.of(10, 0),
                LocalTime.of(10, 30), LocalTime.of(11, 0), LocalTime.of(11, 30)
        );
    }

    @Test
    void naoDeveOferecerUmHorarioQueJaEstaOcupado() {
        when(profissionalRepository.findByIdAndBarbeariaId(PROFISSIONAL_ID, BARBEARIA_ID))
                .thenReturn(Optional.of(profissionalPadrao()));
        when(servicoRepository.findByIdAndBarbeariaId(SERVICO_ID, BARBEARIA_ID))
                .thenReturn(Optional.of(servicoPadrao(30)));

        Agendamento existente = Agendamento.builder()
                .id(99L)
                .data(QUARTA_FEIRA)
                .horaInicio(LocalTime.of(10, 0))
                .horaFim(LocalTime.of(10, 30))
                .status(StatusAgendamento.AGENDADO)
                .build();
        when(agendamentoRepository.findAllByProfissionalIdAndDataAndBarbeariaId(PROFISSIONAL_ID, QUARTA_FEIRA, BARBEARIA_ID))
                .thenReturn(List.of(existente));

        List<LocalTime> disponiveis = disponibilidadeService.horariosDisponiveis(BARBEARIA_ID, PROFISSIONAL_ID, SERVICO_ID, QUARTA_FEIRA);

        assertThat(disponiveis).doesNotContain(LocalTime.of(10, 0));
        assertThat(disponiveis).containsExactly(
                LocalTime.of(9, 0), LocalTime.of(9, 30), LocalTime.of(10, 30), LocalTime.of(11, 0), LocalTime.of(11, 30)
        );
    }

    @Test
    void agendamentoCanceladoNaoBloqueiaOHorario() {
        when(profissionalRepository.findByIdAndBarbeariaId(PROFISSIONAL_ID, BARBEARIA_ID))
                .thenReturn(Optional.of(profissionalPadrao()));
        when(servicoRepository.findByIdAndBarbeariaId(SERVICO_ID, BARBEARIA_ID))
                .thenReturn(Optional.of(servicoPadrao(30)));

        Agendamento cancelado = Agendamento.builder()
                .id(99L)
                .data(QUARTA_FEIRA)
                .horaInicio(LocalTime.of(10, 0))
                .horaFim(LocalTime.of(10, 30))
                .status(StatusAgendamento.CANCELADO)
                .build();
        when(agendamentoRepository.findAllByProfissionalIdAndDataAndBarbeariaId(PROFISSIONAL_ID, QUARTA_FEIRA, BARBEARIA_ID))
                .thenReturn(List.of(cancelado));

        List<LocalTime> disponiveis = disponibilidadeService.horariosDisponiveis(BARBEARIA_ID, PROFISSIONAL_ID, SERVICO_ID, QUARTA_FEIRA);

        assertThat(disponiveis).contains(LocalTime.of(10, 0));
    }

    @Test
    void naoDeveOferecerHorarioEmDiaQueProfissionalNaoTrabalha() {
        Profissional soTrabalhaSegunda = Profissional.builder()
                .id(PROFISSIONAL_ID).nome("Carlos").ativo(true)
                .diasTrabalho("SEG").horarioInicio("09:00").horarioFim("12:00")
                .build();
        when(profissionalRepository.findByIdAndBarbeariaId(PROFISSIONAL_ID, BARBEARIA_ID))
                .thenReturn(Optional.of(soTrabalhaSegunda));
        when(servicoRepository.findByIdAndBarbeariaId(SERVICO_ID, BARBEARIA_ID))
                .thenReturn(Optional.of(servicoPadrao(30)));

        List<LocalTime> disponiveis = disponibilidadeService.horariosDisponiveis(BARBEARIA_ID, PROFISSIONAL_ID, SERVICO_ID, QUARTA_FEIRA);

        assertThat(disponiveis).isEmpty();
    }

    @Test
    void naoDeveOferecerHorarioParaProfissionalInativo() {
        Profissional inativo = Profissional.builder()
                .id(PROFISSIONAL_ID).nome("Carlos").ativo(false)
                .diasTrabalho("QUA").horarioInicio("09:00").horarioFim("12:00")
                .build();
        when(profissionalRepository.findByIdAndBarbeariaId(PROFISSIONAL_ID, BARBEARIA_ID))
                .thenReturn(Optional.of(inativo));
        when(servicoRepository.findByIdAndBarbeariaId(SERVICO_ID, BARBEARIA_ID))
                .thenReturn(Optional.of(servicoPadrao(30)));

        List<LocalTime> disponiveis = disponibilidadeService.horariosDisponiveis(BARBEARIA_ID, PROFISSIONAL_ID, SERVICO_ID, QUARTA_FEIRA);

        assertThat(disponiveis).isEmpty();
    }

    @Test
    void naoDeveOferecerHorarioQuandoProfissionalNaoTemExpedienteCadastrado() {
        Profissional semExpediente = Profissional.builder()
                .id(PROFISSIONAL_ID).nome("Carlos").ativo(true)
                .diasTrabalho("QUA").horarioInicio(null).horarioFim(null)
                .build();
        when(profissionalRepository.findByIdAndBarbeariaId(PROFISSIONAL_ID, BARBEARIA_ID))
                .thenReturn(Optional.of(semExpediente));
        when(servicoRepository.findByIdAndBarbeariaId(SERVICO_ID, BARBEARIA_ID))
                .thenReturn(Optional.of(servicoPadrao(30)));

        List<LocalTime> disponiveis = disponibilidadeService.horariosDisponiveis(BARBEARIA_ID, PROFISSIONAL_ID, SERVICO_ID, QUARTA_FEIRA);

        assertThat(disponiveis).isEmpty();
    }
}
