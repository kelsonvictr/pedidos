package br.com.pedidos.api.domain.pedido;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record Pedido(UUID id, StatusPedido status, List<ItemPedido> itens) {

    public Pedido {
        itens = List.copyOf(itens);
    }

    public static Pedido novo() {
        return new Pedido(UUID.randomUUID(), StatusPedido.ABERTO, List.of());
    }

    public Pedido adicionarItem(ItemPedido item) {
        if (status != StatusPedido.ABERTO) {
            throw new IllegalStateException("Pedido " + status + " não aceita novos itens");
        }
        List<ItemPedido> novosItens = new ArrayList<>(itens);
        novosItens.add(item);
        return new Pedido(id, status, novosItens);
    }

    public Pedido pagar() {
        if (status != StatusPedido.ABERTO) {
            throw new IllegalStateException("Pedido " + status + " não pode ser pago");
        }
        return new Pedido(id, StatusPedido.PAGO, itens);
    }

    public Pedido cancelar() {
        if (status != StatusPedido.ABERTO) {
            throw new IllegalStateException("Pedido " + status + " não pode ser cancelado");
        }
        return new Pedido(id, StatusPedido.CANCELADO, itens);
    }
}
