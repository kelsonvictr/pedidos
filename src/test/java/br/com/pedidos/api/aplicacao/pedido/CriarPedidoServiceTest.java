package br.com.pedidos.api.aplicacao.pedido;

import br.com.pedidos.api.domain.pedido.ItemPedido;
import br.com.pedidos.api.domain.pedido.Pedido;
import br.com.pedidos.api.domain.pedido.StatusPedido;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CriarPedidoServiceTest {

    private static final ItemPedido CAFE_500 = new ItemPedido("CAFE-500", 2, new BigDecimal("18.90"));

    private final PedidosEmMemoria pedidosEmMemoria = new PedidosEmMemoria();
    private final CriarPedidoService service = new CriarPedidoService(pedidosEmMemoria);

    @Test
    void criaPedidoComItensValidos() {
        Pedido pedido = service.criar("c-1", List.of(CAFE_500));

        assertThat(pedido.status()).isEqualTo(StatusPedido.ABERTO);
        assertThat(pedido.itens()).containsExactly(CAFE_500);
    }

    @Test
    void pedidoCriadoESalvoAtravesDePedidos() {
        Pedido pedido = service.criar("c-1", List.of(CAFE_500));

        assertThat(pedidosEmMemoria.buscarPorId(pedido.id())).contains(pedido);
    }

    @Test
    void retornoEOPedidoSalvo() {
        Pedido pedido = service.criar("c-1", List.of(CAFE_500));

        assertThat(pedidosEmMemoria.buscarPorId(pedido.id())).contains(pedido);
    }

    @Test
    void listaDeItensVaziaERecusada() {
        assertThatThrownBy(() -> service.criar("c-1", List.of()))
                .isInstanceOf(PedidoSemItensException.class);

        assertThat(pedidosEmMemoria.estaVazio()).isTrue();
    }

    @Test
    void listaDeItensNulaERecusada() {
        assertThatThrownBy(() -> service.criar("c-1", null))
                .isInstanceOf(PedidoSemItensException.class);

        assertThat(pedidosEmMemoria.estaVazio()).isTrue();
    }

    @Test
    void itemInvalidoContinuaRecusadoPeloDominio() {
        assertThatThrownBy(() -> new ItemPedido("CAFE-500", 0, new BigDecimal("18.90")))
                .isInstanceOf(br.com.pedidos.api.domain.pedido.ItemInvalidoException.class);
    }

    @Test
    void cenarioDeReferenciaDaAula() {
        Pedido pedido = service.criar("c-1", List.of(CAFE_500));

        assertThat(pedido.itens()).hasSize(1);
        assertThat(pedido.itens().get(0).total()).isEqualByComparingTo(new BigDecimal("37.80"));
    }

    private static class PedidosEmMemoria implements Pedidos {

        private final Map<UUID, Pedido> armazenamento = new HashMap<>();

        @Override
        public Pedido salvar(Pedido pedido) {
            armazenamento.put(pedido.id(), pedido);
            return pedido;
        }

        @Override
        public Optional<Pedido> buscarPorId(UUID id) {
            return Optional.ofNullable(armazenamento.get(id));
        }

        boolean estaVazio() {
            return armazenamento.isEmpty();
        }
    }
}
