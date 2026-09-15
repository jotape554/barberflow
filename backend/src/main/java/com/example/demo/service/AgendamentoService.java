package com.example.demo.service;

import com.example.demo.enums.FormaPagamento;
import com.example.demo.enums.StatusAgendamento;
import com.example.demo.enums.StatusReceita;
import com.example.demo.exception.ConflitoHorarioException;
import com.example.demo.exception.RecursoNaoEncontradoException;
import com.example.demo.exception.RegraDeNegocioException;
import com.example.demo.model.*;
import com.example.demo.repository.*;
import com.example.demo.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AgendamentoService {

    private final AgendamentoRepository agendamentoRepository;
    private final BarbeariaRepository barbeariaRepository;
    private final ClienteRepository clienteRepository;
    private final ProfissionalRepository profissionalRepository;
    private final ServicoRepository servicoRepository;
    private final ReceitaRepository receitaRepository;
    private final ComissaoRepository comissaoRepository;

    public List<Agendamento> listar() {
        Long barbeariaId = SecurityUtils.barbeariaAtualId();
        if (SecurityUtils.isProfissional()) {
            return agendamentoRepository.findAllByBarbeariaIdAndProfissionalId(barbeariaId, SecurityUtils.profissionalAtualId());
        }
        return agendamentoRepository.findAllByBarbeariaId(barbeariaId);
    }

    public List<Agendamento> listarPorData(LocalDate data) {
        Long barbeariaId = SecurityUtils.barbeariaAtualId();
        if (SecurityUtils.isProfissional()) {
            return agendamentoRepository.findAllByBarbeariaIdAndProfissionalIdAndData(
                    barbeariaId, SecurityUtils.profissionalAtualId(), data);
        }
        return agendamentoRepository.findAllByBarbeariaIdAndData(barbeariaId, data);
    }

    /** Nunca deixa um profissional enxergar (ou agir sobre) o agendamento de outro profissional. */
    public Agendamento buscar(Long id) {
        Agendamento agendamento = agendamentoRepository.findByIdAndBarbeariaId(id, SecurityUtils.barbeariaAtualId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento não encontrado"));

        if (SecurityUtils.isProfissional() && !agendamento.getProfissional().getId().equals(SecurityUtils.profissionalAtualId())) {
            throw new RecursoNaoEncontradoException("Agendamento não encontrado");
        }
        return agendamento;
    }

    /** Usado pelas rotas autenticadas do painel (barbearia vem do JWT). Profissional só cria pra si mesmo. */
    @Transactional
    public Agendamento criar(Long clienteId, Long profissionalId, Long servicoId,
                              LocalDate data, LocalTime horaInicio, String observacao) {
        Long profissionalFinal = SecurityUtils.isProfissional() ? SecurityUtils.profissionalAtualId() : profissionalId;
        return criarComBarbeariaId(SecurityUtils.barbeariaAtualId(), clienteId, profissionalFinal, servicoId,
                data, horaInicio, observacao);
    }

    /** Usado pelo fluxo público (barbearia vem do slug, não existe JWT). */
    @Transactional
    public Agendamento criarComBarbeariaId(Long barbeariaId, Long clienteId, Long profissionalId, Long servicoId,
                                            LocalDate data, LocalTime horaInicio, String observacao) {

        Barbearia barbearia = barbeariaRepository.findById(barbeariaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Barbearia não encontrada"));

        Cliente cliente = clienteRepository.findByIdAndBarbeariaId(clienteId, barbeariaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado"));

        Profissional profissional = profissionalRepository.findByIdAndBarbeariaId(profissionalId, barbeariaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Profissional não encontrado"));

        Servico servico = servicoRepository.findByIdAndBarbeariaId(servicoId, barbeariaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Serviço não encontrado"));

        if (!servico.isAtivo()) {
            throw new RegraDeNegocioException("Este serviço não está mais disponível.");
        }

        LocalTime horaFim = horaInicio.plusMinutes(servico.getDuracaoMinutos());

        verificarConflito(profissional.getId(), data, horaInicio, horaFim, barbeariaId, null);

        Agendamento agendamento = Agendamento.builder()
                .barbearia(barbearia)
                .cliente(cliente)
                .profissional(profissional)
                .servico(servico)
                .data(data)
                .horaInicio(horaInicio)
                .horaFim(horaFim)
                .status(StatusAgendamento.AGENDADO)
                .observacao(observacao)
                .build();

        return agendamentoRepository.save(agendamento);
    }

    /** Para qualquer status QUE NÃO SEJA conclusão (essa tem fluxo próprio, ver concluir()). */
    @Transactional
    public Agendamento atualizarStatus(Long id, StatusAgendamento novoStatus) {
        if (novoStatus == StatusAgendamento.CONCLUIDO) {
            throw new RegraDeNegocioException("Para concluir, use o endpoint de conclusão informando a forma de pagamento.");
        }
        Agendamento agendamento = buscar(id);
        agendamento.setStatus(novoStatus);
        return agendamentoRepository.save(agendamento);
    }

    /**
     * Conclui o agendamento e gera automaticamente a Receita e a Comissão do profissional.
     * Idempotente: se já existir receita/comissão para este agendamento, não duplica.
     */
    @Transactional
    public Agendamento concluir(Long id, FormaPagamento formaPagamento) {
        Agendamento agendamento = buscar(id);

        if (agendamento.getStatus() == StatusAgendamento.CANCELADO) {
            throw new RegraDeNegocioException("Não é possível concluir um agendamento cancelado.");
        }

        agendamento.setStatus(StatusAgendamento.CONCLUIDO);
        agendamentoRepository.save(agendamento);

        if (!receitaRepository.existsByAgendamentoId(agendamento.getId())) {
            Receita receita = Receita.builder()
                    .barbearia(agendamento.getBarbearia())
                    .agendamento(agendamento)
                    .cliente(agendamento.getCliente())
                    .profissional(agendamento.getProfissional())
                    .servico(agendamento.getServico())
                    .valor(agendamento.getServico().getPreco())
                    .formaPagamento(formaPagamento)
                    .status(StatusReceita.RECEBIDO)
                    .data(agendamento.getData())
                    .build();
            receita = receitaRepository.save(receita);

            if (!comissaoRepository.existsByAgendamentoId(agendamento.getId())) {
                BigDecimal percentual = agendamento.getProfissional().getPercentualComissao();
                BigDecimal valorComissao = receita.getValor()
                        .multiply(percentual)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

                Comissao comissao = Comissao.builder()
                        .barbearia(agendamento.getBarbearia())
                        .profissional(agendamento.getProfissional())
                        .agendamento(agendamento)
                        .receita(receita)
                        .percentual(percentual)
                        .valor(valorComissao)
                        .paga(false)
                        .data(agendamento.getData())
                        .build();
                comissaoRepository.save(comissao);
            }
        }

        return agendamento;
    }

    /** Regra crítica: nunca permitir dois agendamentos que se sobrepõem para o mesmo profissional. */
    public void verificarConflito(Long profissionalId, LocalDate data, LocalTime inicio, LocalTime fim,
                                   Long barbeariaId, Long ignorarAgendamentoId) {

        List<Agendamento> doMesmoDia = agendamentoRepository
                .findAllByProfissionalIdAndDataAndBarbeariaId(profissionalId, data, barbeariaId);

        for (Agendamento existente : doMesmoDia) {
            if (existente.getId().equals(ignorarAgendamentoId)) continue;
            if (existente.getStatus() == StatusAgendamento.CANCELADO) continue;

            boolean sobrepoe = inicio.isBefore(existente.getHoraFim()) && fim.isAfter(existente.getHoraInicio());
            if (sobrepoe) {
                throw new ConflitoHorarioException(
                        "Este profissional já possui um agendamento entre " +
                        existente.getHoraInicio() + " e " + existente.getHoraFim() + " nesta data.");
            }
        }
    }
}
