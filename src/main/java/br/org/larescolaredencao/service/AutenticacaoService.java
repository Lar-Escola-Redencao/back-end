package br.org.larescolaredencao.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import br.org.larescolaredencao.repository.MembroRepository;

@Service
public class AutenticacaoService implements UserDetailsService {

    @Autowired
    private MembroRepository repository;

    @Override
    public UserDetails loadUserByUsername(String identificador) throws UsernameNotFoundException {
        return repository.findByEmail(identificador)
                .or(() -> repository.findByCpf(identificador))
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado!"));
    }
}
