package br.com.pedidos.api.domain.pedido;

import java.math.BigDecimal;

public record ItemPedido(String codigoProduto, int quantidade, BigDecimal precoUnitario) {

    public ItemPedido {
        if (quantidade <= 0) {
            throw new ItemInvalidoException("Quantidade deve ser positiva");
        }
        if (precoUnitario == null || precoUnitario.signum() <= 0) {
            throw new ItemInvalidoException("Preço unitário deve ser positivo");
        }
    }

    public BigDecimal total() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }
}
