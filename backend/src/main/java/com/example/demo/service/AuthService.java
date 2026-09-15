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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final BarbeariaRepository barbeariaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse registrar(RegistroRequest req) {
        if (usuarioRepository.existsByEmail(req.getEmail())) {
            throw new RegraDeNegocioException("Já existe um usuário com este e-mail.");
        }

        String slug = gerarSlugUnico(req.getNomeBarbearia());

        Barbearia barbearia = Barbearia.builder()
                .nome(req.getNomeBarbearia())
                .slug(slug)
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

        return new AuthResponse(token, admin.getNome(), admin.getPapel().name(), barbearia.getId(), barbearia.getSlug());
    }

    public AuthResponse login(LoginRequest req) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getEmail(), req.getSenha()));

        Usuario usuario = usuarioRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new RegraDeNegocioException("E-mail ou senha inválidos"));

        UsuarioPrincipal principal = new UsuarioPrincipal(usuario);
        String token = jwtService.gerarToken(principal);

        return new AuthResponse(token, usuario.getNome(), usuario.getPapel().name(),
                usuario.getBarbearia().getId(), usuario.getBarbearia().getSlug());
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
