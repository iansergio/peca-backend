package com.peca.backend.auth;

import com.peca.backend.usuario.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * Codigo OTP de 6 digitos enviado por email para autenticar o usuario.
 */
@Entity
@Table(name = "codigos_verificacao", indexes = @Index(name = "idx_codigos_verificacao_email", columnList = "email"))
public class CodigoVerificacao {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	// Nulo enquanto o email nao tem conta: o usuario so e criado quando o codigo e validado.
	@ManyToOne(fetch = FetchType.LAZY, optional = true)
	@JoinColumn(name = "usuario_id")
	@OnDelete(action = OnDeleteAction.CASCADE)
	private Usuario usuario;

	// Nome usado no cadastro quando o email ainda nao tem conta.
	@Column(length = 120)
	private String nome;

	@Column(nullable = false, length = 6)
	private String codigo;

	@Column(nullable = false, length = 160)
	private String email;

	@Column(name = "expira_em", nullable = false)
	private LocalDateTime expiraEm;

	@Column(nullable = false)
	private boolean utilizado;

	// Tentativas erradas; ao atingir o limite o codigo e invalidado (protecao contra forca bruta).
	@Column(nullable = false)
	private int tentativas;

	// Base do intervalo minimo e do limite de solicitacoes por hora.
	@Column(name = "criado_em", nullable = false)
	private LocalDateTime criadoEm;

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public Usuario getUsuario() {
		return usuario;
	}

	public void setUsuario(Usuario usuario) {
		this.usuario = usuario;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public String getCodigo() {
		return codigo;
	}

	public void setCodigo(String codigo) {
		this.codigo = codigo;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public LocalDateTime getExpiraEm() {
		return expiraEm;
	}

	public void setExpiraEm(LocalDateTime expiraEm) {
		this.expiraEm = expiraEm;
	}

	public boolean isUtilizado() {
		return utilizado;
	}

	public void setUtilizado(boolean utilizado) {
		this.utilizado = utilizado;
	}

	public int getTentativas() {
		return tentativas;
	}

	public void setTentativas(int tentativas) {
		this.tentativas = tentativas;
	}

	public LocalDateTime getCriadoEm() {
		return criadoEm;
	}

	public void setCriadoEm(LocalDateTime criadoEm) {
		this.criadoEm = criadoEm;
	}

}
