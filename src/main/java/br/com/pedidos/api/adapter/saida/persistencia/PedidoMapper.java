package br.com.pedidos.api.adapter.saida.persistencia;

import br.com.pedidos.api.domain.pedido.ItemPedido;
import br.com.pedidos.api.domain.pedido.Pedido;
import br.com.pedidos.api.domain.pedido.StatusPedido;

import java.util.ArrayList;
import java.util.List;

class PedidoMapper {

    private PedidoMapper() {
    }

    static PedidoJpaEntity paraEntidade(Pedido pedido) {
        PedidoJpaEntity entidade = new PedidoJpaEntity(pedido.id(), pedido.status().name());
        for (ItemPedido item : pedido.itens()) {
            entidade.adicionarItem(new ItemJpaEntity(item.codigoProduto(), item.quantidade(), item.precoUnitario()));
        }
        return entidade;
    }

    static Pedido paraDominio(PedidoJpaEntity entidade) {
        List<ItemPedido> itens = new ArrayList<>();
        for (ItemJpaEntity item : entidade.getItens()) {
            itens.add(new ItemPedido(item.getSku(), item.getQuantidade(), item.getPrecoUnitario()));
        }
        return new Pedido(entidade.getId(), StatusPedido.valueOf(entidade.getStatus()), itens);
    }
}
