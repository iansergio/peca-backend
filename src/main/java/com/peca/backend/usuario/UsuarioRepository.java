package com.peca.backend.usuario;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

	boolean existsByEmail(String email);

}
