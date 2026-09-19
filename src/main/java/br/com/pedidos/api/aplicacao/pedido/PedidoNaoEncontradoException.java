package br.com.pedidos.api.aplicacao.pedido;

public class PedidoNaoEncontradoException extends RuntimeException {

    public PedidoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
