package br.com.pedidos.api.adapter.saida.persistencia;

import br.com.pedidos.api.domain.pedido.ItemPedido;
import br.com.pedidos.api.domain.pedido.Pedido;
import br.com.pedidos.api.domain.pedido.StatusPedido;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PedidosJpaAdapterIT {

    private static final ItemPedido CAFE_500 = new ItemPedido("CAFE-500", 2, new BigDecimal("18.90"));

    @Autowired
    private PedidosJpaAdapter adapter;

    @Test
    void salvaERecuperaPedidoDoClienteC1() {
        Pedido pedidoDoClienteC1 = Pedido.novo().adicionarItem(CAFE_500);

        // salvar() é @Transactional no adapter: transação própria, fechada ao retornar.
        Pedido salvo = adapter.salvar(pedidoDoClienteC1);
        UUID id = salvo.id();

        // buscarPorId() é @Transactional no adapter: nova transação, separada da anterior.
        Pedido recuperado = adapter.buscarPorId(id).orElseThrow();

        assertThat(recuperado.id()).isEqualTo(id);
        assertThat(recuperado.status()).isEqualTo(StatusPedido.ABERTO);
        assertThat(recuperado.itens()).hasSize(1);

        ItemPedido item = recuperado.itens().get(0);
        assertThat(item.codigoProduto()).isEqualTo("CAFE-500");
        assertThat(item.quantidade()).isEqualTo(2);
        assertThat(item.precoUnitario()).isEqualByComparingTo(new BigDecimal("18.90"));
        assertThat(item.total()).isEqualByComparingTo(new BigDecimal("37.80"));
    }
}
