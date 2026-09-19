package br.com.pedidos.api.domain.pedido;

public class ItemInvalidoException extends RuntimeException {

    public ItemInvalidoException(String mensagem) {
        super(mensagem);
    }
}
