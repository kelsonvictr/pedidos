package br.com.pedidos.api.aplicacao.pedido;

import br.com.pedidos.api.domain.pedido.ItemPedido;
import br.com.pedidos.api.domain.pedido.Pedido;

import java.util.List;

public class CriarPedidoService implements CriarPedido {

    private final Pedidos pedidos;

    public CriarPedidoService(Pedidos pedidos) {
        this.pedidos = pedidos;
    }

    @Override
    public Pedido criar(String cliente, List<ItemPedido> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new PedidoSemItensException("Pedido precisa de ao menos um item");
        }

        Pedido pedido = Pedido.novo();
        for (ItemPedido item : itens) {
            pedido = pedido.adicionarItem(item);
        }

        return pedidos.salvar(pedido);
    }
}
