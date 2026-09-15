package com.example.demo.service;

import com.example.demo.config.StripeConfig;
import com.example.demo.enums.StatusAssinaturaSaas;
import com.example.demo.model.Barbearia;
import com.example.demo.repository.BarbeariaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regra que decide se uma barbearia inadimplente é barrada do painel. Um bug aqui ou libera
 * acesso de graça para sempre, ou bloqueia quem está pagando — os dois são graves, então a
 * lógica pura (sem precisar de banco nem de rede) fica coberta aqui.
 */
@ExtendWith(MockitoExtension.class)
class AssinaturaServiceAcessoTest {

    @Mock private BarbeariaRepository barbeariaRepository;
    @Mock private StripeConfig stripeConfig;

    private AssinaturaService service() {
        return new AssinaturaService(barbeariaRepository, stripeConfig);
    }

    private Barbearia comStatus(StatusAssinaturaSaas status, LocalDate dataFimTrial) {
        return Barbearia.builder()
                .nome("Barbearia Teste")
                .slug("barbearia-teste")
                .statusAssinaturaSaas(status)
                .dataFimTrial(dataFimTrial)
                .build();
    }

    @Test
    void assinaturaAtivaSempreLiberaAcesso() {
        Barbearia barbearia = comStatus(StatusAssinaturaSaas.ATIVA, null);
        assertThat(service().acessoLiberado(barbearia)).isTrue();
    }

    @Test
    void trialDentroDoPrazoLiberaAcesso() {
        Barbearia barbearia = comStatus(StatusAssinaturaSaas.TRIAL, LocalDate.now().plusDays(1));
        assertThat(service().acessoLiberado(barbearia)).isTrue();
    }

    @Test
    void trialNoUltimoDiaAindaLiberaAcesso() {
        Barbearia barbearia = comStatus(StatusAssinaturaSaas.TRIAL, LocalDate.now());
        assertThat(service().acessoLiberado(barbearia)).isTrue();
    }

    @Test
    void trialExpiradoOntemBloqueiaAcesso() {
        Barbearia barbearia = comStatus(StatusAssinaturaSaas.TRIAL, LocalDate.now().minusDays(1));
        assertThat(service().acessoLiberado(barbearia)).isFalse();
    }

    @Test
    void trialSemDataDefinidaBloqueiaAcesso() {
        Barbearia barbearia = comStatus(StatusAssinaturaSaas.TRIAL, null);
        assertThat(service().acessoLiberado(barbearia)).isFalse();
    }

    @Test
    void statusInativaBloqueiaAcessoMesmoComTrialNoPrazo() {
        Barbearia barbearia = comStatus(StatusAssinaturaSaas.INATIVA, LocalDate.now().plusDays(5));
        assertThat(service().acessoLiberado(barbearia)).isFalse();
    }

    @Test
    void statusCanceladaBloqueiaAcesso() {
        Barbearia barbearia = comStatus(StatusAssinaturaSaas.CANCELADA, LocalDate.now().plusDays(5));
        assertThat(service().acessoLiberado(barbearia)).isFalse();
    }
}
