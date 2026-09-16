package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Um retrato completo da barbearia pra tomada de decisão: visão geral, comparações com o
 * período anterior, financeiro (faturamento por dia/forma de pagamento/lucro estimado),
 * ranking de profissionais e serviços do mês, e clientes (novos/recorrentes/a reativar).
 * Nenhum número aqui é inventado — tudo vem de consultas reais ao banco.
 */
@Data
@AllArgsConstructor
public class DashboardResponse {
    private VisaoGeral visaoGeral;
    private Comparacoes comparacoes;
    private Financeiro financeiro;
    private List<ProfissionalDesempenho> profissionais;
    private List<ServicoDesempenho> servicos;
    private ClientesResumo clientes;

    @Data
    @AllArgsConstructor
    public static class VisaoGeral {
        private BigDecimal faturamentoHoje;
        private BigDecimal faturamentoSemana;
        private BigDecimal faturamentoMes;
        private long atendimentosHoje;
        private long atendimentosSemana;
        private long atendimentosMes;
        private long clientesCadastrados;
        private BigDecimal ticketMedioMes;
        private long novosClientesMes;
        private long clientesRecorrentesMes;
    }

    /** variacaoXPercentual é null quando não há período anterior pra comparar (base zero). */
    @Data
    @AllArgsConstructor
    public static class Comparacoes {
        private BigDecimal faturamentoHoje;
        private BigDecimal faturamentoOntem;
        private Double variacaoHojeOntemPercentual;
        private BigDecimal faturamentoSemanaAtual;
        private BigDecimal faturamentoSemanaAnterior;
        private Double variacaoSemanaPercentual;
        private BigDecimal faturamentoMesAtual;
        private BigDecimal faturamentoMesAnterior;
        private Double variacaoMesPercentual;
    }

    @Data
    @AllArgsConstructor
    public static class Financeiro {
        private List<PontoFaturamentoDia> faturamentoPorDia;
        private Map<String, BigDecimal> faturamentoPorFormaPagamento;
        private BigDecimal despesasMes;
        private BigDecimal comissoesMes;
        /** Estimado: faturamento - despesas - comissões do mês. Não é um fechamento contábil. */
        private BigDecimal lucroLiquidoMes;
    }

    @Data
    @AllArgsConstructor
    public static class PontoFaturamentoDia {
        private String data;
        private BigDecimal valor;
    }

    @Data
    @AllArgsConstructor
    public static class ProfissionalDesempenho {
        private String nome;
        private long atendimentos;
        private BigDecimal faturamento;
        private BigDecimal comissao;
        private BigDecimal ticketMedio;
        private long clientesAtendidos;
    }

    @Data
    @AllArgsConstructor
    public static class ServicoDesempenho {
        private String nome;
        private long quantidade;
        private BigDecimal faturamento;
        private BigDecimal ticketMedio;
    }

    @Data
    @AllArgsConstructor
    public static class ClientesResumo {
        private long novos;
        private long recorrentes;
        private long inativos30Dias;
        private List<ClienteInativo> listaInativos;
    }

    @Data
    @AllArgsConstructor
    public static class ClienteInativo {
        private String nome;
        private String telefone;
        /** Null quando o cliente nunca teve nenhum agendamento. */
        private String ultimoAgendamento;
    }
}
