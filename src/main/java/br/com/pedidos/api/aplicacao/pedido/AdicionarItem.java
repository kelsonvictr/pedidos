package br.com.pedidos.api.aplicacao.pedido;

import br.com.pedidos.api.domain.pedido.ItemPedido;
import br.com.pedidos.api.domain.pedido.Pedido;

import java.util.UUID;

public interface AdicionarItem {

    Pedido adicionar(UUID pedidoId, ItemPedido item);
}
