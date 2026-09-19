package br.com.pedidos.api.aplicacao.pedido;

public class PedidoSemItensException extends RuntimeException {

    public PedidoSemItensException(String mensagem) {
        super(mensagem);
    }
}
