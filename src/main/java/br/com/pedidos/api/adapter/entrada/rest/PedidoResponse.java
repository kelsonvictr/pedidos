package br.com.pedidos.api.adapter.entrada.rest;

import br.com.pedidos.api.domain.pedido.Pedido;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PedidoResponse(
        UUID id,
        String clienteId,
        List<ItemResponse> itens,
        String status,
        BigDecimal total
) {

    static PedidoResponse de(Pedido pedido, String clienteId) {
        List<ItemResponse> itens = pedido.itens().stream()
                .map(ItemResponse::de)
                .toList();

        BigDecimal total = itens.stream()
                .map(ItemResponse::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new PedidoResponse(pedido.id(), clienteId, itens, pedido.status().name(), total);
    }
}
