package com.example.demo.service;

import com.example.demo.config.StripeConfig;
import com.example.demo.dto.AssinaturaResponse;
import com.example.demo.dto.PlanoDisponivelResponse;
import com.example.demo.enums.PlanoSaas;
import com.example.demo.enums.Recurso;
import com.example.demo.enums.StatusAssinaturaSaas;
import com.example.demo.exception.RecursoNaoEncontradoException;
import com.example.demo.exception.RegraDeNegocioException;
import com.example.demo.model.Barbearia;
import com.example.demo.repository.BarbeariaRepository;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

/**
 * Centraliza a regra de acesso ao painel e a integração com a Stripe. O acesso é liberado
 * quando status == ATIVA, ou quando ainda está dentro do período de teste gratuito.
 * A confirmação de pagamento chega de forma assíncrona pelo webhook da Stripe
 * (ver {@link com.example.demo.controller.StripeWebhookController}), nunca diretamente
 * do navegador do usuário — evita que alguém "libere o próprio acesso" manipulando o front.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AssinaturaService {

    public static final int DIAS_TRIAL = 14;

    private final BarbeariaRepository barbeariaRepository;
    private final StripeConfig stripeConfig;

    public List<PlanoDisponivelResponse> listarPlanos() {
        return Arrays.stream(PlanoSaas.values())
                .map(p -> new PlanoDisponivelResponse(p, p.getPrecoMensal(), p.getDescricao()))
                .toList();
    }

    public AssinaturaResponse status(Long barbeariaId) {
        return paraResponse(buscar(barbeariaId));
    }

    /** Cria (ou reaproveita) o cliente na Stripe e devolve a URL de checkout para o plano escolhido. */
    @Transactional
    public String criarSessaoCheckout(Long barbeariaId, PlanoSaas plano) {
        Barbearia barbearia = buscar(barbeariaId);
        String priceId = stripeConfig.priceIdPara(plano);
        if (priceId == null || priceId.isBlank()) {
            throw new RegraDeNegocioException("Pagamentos ainda não configurados para este plano.");
        }

        try {
            String customerId = garantirCustomerId(barbearia);

            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    .setCustomer(customerId)
                    .setClientReferenceId(String.valueOf(barbearia.getId()))
                    .putMetadata("barbeariaId", String.valueOf(barbearia.getId()))
                    .putMetadata("plano", plano.name())
                    .setSuccessUrl(stripeConfig.getFrontendUrl() + "/painel/assinatura?checkout=sucesso")
                    .setCancelUrl(stripeConfig.getFrontendUrl() + "/painel/assinatura?checkout=cancelado")
                    .addLineItem(SessionCreateParams.LineItem.builder()
                            .setPrice(priceId)
                            .setQuantity(1L)
                            .build())
                    .build();

            Session session = Session.create(params);
            return session.getUrl();
        } catch (StripeException e) {
            log.error("Falha ao criar sessão de checkout na Stripe", e);
            throw new RegraDeNegocioException("Não foi possível iniciar o pagamento agora. Tente novamente em instantes.");
        }
    }

    /** URL do portal da Stripe onde a barbearia gerencia forma de pagamento e pode cancelar. */
    public String criarSessaoPortal(Long barbeariaId) {
        Barbearia barbearia = buscar(barbeariaId);
        if (barbearia.getStripeCustomerId() == null) {
            throw new RegraDeNegocioException("Você ainda não tem uma assinatura para gerenciar.");
        }
        try {
            com.stripe.param.billingportal.SessionCreateParams params =
                    com.stripe.param.billingportal.SessionCreateParams.builder()
                            .setCustomer(barbearia.getStripeCustomerId())
                            .setReturnUrl(stripeConfig.getFrontendUrl() + "/painel/assinatura")
                            .build();
            return com.stripe.model.billingportal.Session.create(params).getUrl();
        } catch (StripeException e) {
            log.error("Falha ao criar sessão do portal de cobrança na Stripe", e);
            throw new RegraDeNegocioException("Não foi possível abrir o portal de cobrança agora.");
        }
    }

    /** Chamado pelo webhook quando checkout.session.completed chega — a assinatura foi paga. */
    @Transactional
    public void confirmarCheckout(String stripeCustomerId, String stripeSubscriptionId, Long barbeariaId, PlanoSaas plano) {
        Barbearia barbearia = buscar(barbeariaId);
        barbearia.setStripeCustomerId(stripeCustomerId);
        barbearia.setStripeSubscriptionId(stripeSubscriptionId);
        if (plano != null) {
            barbearia.setPlanoSaas(plano);
        }
        barbearia.setStatusAssinaturaSaas(StatusAssinaturaSaas.ATIVA);
        barbeariaRepository.save(barbearia);
    }

    /** Chamado pelo webhook quando a assinatura é cancelada ou expira na Stripe. */
    @Transactional
    public void marcarCancelada(String stripeSubscriptionId) {
        barbeariaRepository.findByStripeSubscriptionId(stripeSubscriptionId).ifPresent(barbearia -> {
            barbearia.setStatusAssinaturaSaas(StatusAssinaturaSaas.CANCELADA);
            barbeariaRepository.save(barbearia);
        });
    }

    public Event validarEventoWebhook(String payload, String assinatura) throws SignatureVerificationException {
        return Webhook.constructEvent(payload, assinatura, stripeConfig.getWebhookSecret());
    }

    public boolean acessoLiberado(Barbearia barbearia) {
        if (barbearia.getStatusAssinaturaSaas() == StatusAssinaturaSaas.ATIVA) {
            return true;
        }
        if (barbearia.getStatusAssinaturaSaas() == StatusAssinaturaSaas.TRIAL) {
            LocalDate fimTrial = barbearia.getDataFimTrial();
            return fimTrial != null && !LocalDate.now().isAfter(fimTrial);
        }
        return false;
    }

    /**
     * Durante o trial, libera todos os recursos pra barbearia poder explorar o sistema por
     * inteiro antes de decidir o plano. Depois que assina de verdade (ATIVA), passa a valer
     * o recorte real do plano contratado. Sem assinatura válida, o AssinaturaGateFilter já
     * bloqueia o painel inteiro antes disso ser sequer avaliado.
     */
    public boolean recursoLiberado(Barbearia barbearia, Recurso recurso) {
        if (barbearia.getStatusAssinaturaSaas() == StatusAssinaturaSaas.TRIAL) {
            return acessoLiberado(barbearia);
        }
        if (barbearia.getStatusAssinaturaSaas() != StatusAssinaturaSaas.ATIVA) {
            return false;
        }
        return barbearia.getPlanoSaas().atendeNivelMinimo(recurso.getPlanoMinimo());
    }

    private String garantirCustomerId(Barbearia barbearia) throws StripeException {
        if (barbearia.getStripeCustomerId() != null) {
            return barbearia.getStripeCustomerId();
        }
        CustomerCreateParams params = CustomerCreateParams.builder()
                .setName(barbearia.getNome())
                .putMetadata("barbeariaId", String.valueOf(barbearia.getId()))
                .build();
        Customer customer = Customer.create(params);
        barbearia.setStripeCustomerId(customer.getId());
        barbeariaRepository.save(barbearia);
        return customer.getId();
    }

    private AssinaturaResponse paraResponse(Barbearia barbearia) {
        Long diasRestantes = null;
        if (barbearia.getStatusAssinaturaSaas() == StatusAssinaturaSaas.TRIAL && barbearia.getDataFimTrial() != null) {
            diasRestantes = Math.max(0, ChronoUnit.DAYS.between(LocalDate.now(), barbearia.getDataFimTrial()));
        }

        PlanoSaas plano = barbearia.getPlanoSaas();
        return new AssinaturaResponse(
                plano,
                plano.getPrecoMensal(),
                plano.getDescricao(),
                barbearia.getStatusAssinaturaSaas(),
                barbearia.getDataFimTrial(),
                diasRestantes,
                acessoLiberado(barbearia)
        );
    }

    private Barbearia buscar(Long id) {
        return barbeariaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Barbearia não encontrada"));
    }
}
