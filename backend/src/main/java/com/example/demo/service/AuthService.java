package com.example.demo.service;

import com.example.demo.dto.AuthResponse;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.RegistroRequest;
import com.example.demo.enums.Papel;
import com.example.demo.model.Barbearia;
import com.example.demo.model.Usuario;
import com.example.demo.repository.BarbeariaRepository;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.security.JwtService;
import com.example.demo.security.UsuarioPrincipal;
import com.example.demo.exception.RegraDeNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final long VALIDADE_TOKEN_RESET_MINUTOS = 60;

    private final BarbeariaRepository barbeariaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final EmailService emailService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Transactional
    public AuthResponse registrar(RegistroRequest req) {
        if (usuarioRepository.existsByEmail(req.getEmail())) {
            throw new RegraDeNegocioException("Já existe um usuário com este e-mail.");
        }

        String slug = gerarSlugUnico(req.getNomeBarbearia());

        Barbearia barbearia = Barbearia.builder()
                .nome(req.getNomeBarbearia())
                .slug(slug)
                .dataFimTrial(LocalDate.now().plusDays(AssinaturaService.DIAS_TRIAL))
                .build();
        barbearia = barbeariaRepository.save(barbearia);

        Usuario admin = Usuario.builder()
                .nome(req.getNomeAdmin())
                .email(req.getEmail())
                .senhaHash(passwordEncoder.encode(req.getSenha()))
                .papel(Papel.ADMINISTRADOR)
                .barbearia(barbearia)
                .build();
        usuarioRepository.save(admin);

        UsuarioPrincipal principal = new UsuarioPrincipal(admin);
        String token = jwtService.gerarToken(principal);

        return new AuthResponse(token, admin.getNome(), admin.getPapel().name(), barbearia.getId(), barbearia.getSlug(), null);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getEmail(), req.getSenha()));

        Usuario usuario = usuarioRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new RegraDeNegocioException("E-mail ou senha inválidos"));

        UsuarioPrincipal principal = new UsuarioPrincipal(usuario);
        String token = jwtService.gerarToken(principal);

        Long profissionalId = usuario.getProfissional() != null ? usuario.getProfissional().getId() : null;
        return new AuthResponse(token, usuario.getNome(), usuario.getPapel().name(),
                usuario.getBarbearia().getId(), usuario.getBarbearia().getSlug(), profissionalId);
    }

    /**
     * Sempre "funciona" do ponto de vista do cliente, exista ou não o e-mail — nunca revela
     * se um endereço tem conta cadastrada (evita que alguém descubra e-mails de clientes).
     */
    @Transactional
    public void esqueciSenha(String email) {
        usuarioRepository.findByEmail(email).ifPresent(usuario -> {
            String token = UUID.randomUUID().toString();
            usuario.setResetSenhaToken(token);
            usuario.setResetSenhaExpiraEm(Instant.now().plus(VALIDADE_TOKEN_RESET_MINUTOS, ChronoUnit.MINUTES));
            usuarioRepository.save(usuario);

            String link = frontendUrl + "/redefinir-senha?token=" + token;
            emailService.enviarRedefinicaoSenha(usuario.getEmail(), link);
        });
    }

    @Transactional
    public void redefinirSenha(String token, String novaSenha) {
        Usuario usuario = usuarioRepository.findByResetSenhaToken(token)
                .orElseThrow(() -> new RegraDeNegocioException("Link inválido ou expirado."));

        if (usuario.getResetSenhaExpiraEm() == null || Instant.now().isAfter(usuario.getResetSenhaExpiraEm())) {
            throw new RegraDeNegocioException("Link inválido ou expirado.");
        }

        usuario.setSenhaHash(passwordEncoder.encode(novaSenha));
        usuario.setResetSenhaToken(null);
        usuario.setResetSenhaExpiraEm(null);
        usuarioRepository.save(usuario);
    }

    private String gerarSlugUnico(String nome) {
        String base = Normalizer.normalize(nome, Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "")
                .toLowerCase()
                .trim()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");

        String slug = base;
        int contador = 1;
        while (barbeariaRepository.existsBySlug(slug)) {
            slug = base + "-" + contador++;
        }
        return slug;
    }
}
