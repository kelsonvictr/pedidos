package br.com.pedidos.api.aplicacao.pedido;

import br.com.pedidos.api.domain.pedido.ItemPedido;
import br.com.pedidos.api.domain.pedido.Pedido;

import java.util.List;

public interface CriarPedido {

    Pedido criar(String cliente, List<ItemPedido> itens);
}
