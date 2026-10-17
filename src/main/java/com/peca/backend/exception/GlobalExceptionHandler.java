package com.peca.backend.exception;

import com.peca.backend.auth.CredenciaisInvalidasException;
import com.peca.backend.storage.ArmazenamentoException;
import com.peca.backend.storage.ImagemInvalidaException;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(RecursoNaoEncontradoException.class)
	public ProblemDetail handleNaoEncontrado(RecursoNaoEncontradoException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	@ExceptionHandler(ConflitoException.class)
	public ProblemDetail handleConflito(ConflitoException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
	}

	@ExceptionHandler(CredenciaisInvalidasException.class)
	public ProblemDetail handleCredenciais(CredenciaisInvalidasException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
	}

	@ExceptionHandler(AcessoNegadoException.class)
	public ProblemDetail handleAcessoNegado(AcessoNegadoException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ProblemDetail handleIntegridade(DataIntegrityViolationException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
				"Operacao viola uma restricao de integridade (ex: usuario com anuncios vinculados)");
	}

	@ExceptionHandler(ImagemInvalidaException.class)
	public ProblemDetail handleImagemInvalida(ImagemInvalidaException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
	}

	@ExceptionHandler(MissingServletRequestPartException.class)
	public ProblemDetail handleArquivoAusente(MissingServletRequestPartException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Campo obrigatorio ausente: " + ex.getRequestPartName());
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ProblemDetail handleArquivoGrande(MaxUploadSizeExceededException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONTENT_TOO_LARGE, "Arquivo excede o tamanho maximo permitido (5 MB)");
	}

	@ExceptionHandler(ArmazenamentoException.class)
	public ProblemDetail handleArmazenamento(ArmazenamentoException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, ex.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ProblemDetail handleValidacao(MethodArgumentNotValidException ex) {
		String detalhe = ex.getBindingResult().getFieldErrors().stream()
				.map(erro -> erro.getField() + ": " + erro.getDefaultMessage())
				.collect(Collectors.joining("; "));
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detalhe);
	}

}
