package br.com.pedidos.api.adapter.entrada.rest;

import br.com.pedidos.api.domain.pedido.ItemPedido;

import java.math.BigDecimal;

public record ItemResponse(String sku, int quantidade, BigDecimal precoUnitario, BigDecimal total) {

    static ItemResponse de(ItemPedido item) {
        return new ItemResponse(item.codigoProduto(), item.quantidade(), item.precoUnitario(), item.total());
    }
}
