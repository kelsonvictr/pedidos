package br.com.pedidos.api.aplicacao.pedido;

import br.com.pedidos.api.domain.pedido.ItemInvalidoException;
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

class AdicionarItemServiceTest {

    private static final ItemPedido CAFE_500 = new ItemPedido("CAFE-500", 2, new BigDecimal("18.90"));
    private static final ItemPedido CAFE_500_UNIDADE = new ItemPedido("CAFE-500", 1, new BigDecimal("18.90"));

    private final PedidosEmMemoria pedidosEmMemoria = new PedidosEmMemoria();
    private final AdicionarItemService service = new AdicionarItemService(pedidosEmMemoria);

    @Test
    void adicionaItemAPedidoExistenteEAberto() {
        Pedido pedidoSalvo = pedidosEmMemoria.salvar(Pedido.novo().adicionarItem(CAFE_500));

        Pedido resultado = service.adicionar(pedidoSalvo.id(), CAFE_500_UNIDADE);

        assertThat(resultado.itens()).hasSize(2);
    }

    @Test
    void totalAposAdicionar() {
        Pedido pedidoSalvo = pedidosEmMemoria.salvar(Pedido.novo().adicionarItem(CAFE_500));

        Pedido resultado = service.adicionar(pedidoSalvo.id(), CAFE_500_UNIDADE);

        BigDecimal total = resultado.itens().stream()
                .map(ItemPedido::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(total).isEqualByComparingTo(new BigDecimal("56.70"));
    }

    @Test
    void mesmoUuidPreservado() {
        Pedido pedidoSalvo = pedidosEmMemoria.salvar(Pedido.novo().adicionarItem(CAFE_500));

        Pedido resultado = service.adicionar(pedidoSalvo.id(), CAFE_500_UNIDADE);

        assertThat(resultado.id()).isEqualTo(pedidoSalvo.id());
    }

    @Test
    void pedidoSalvoPeloPort() {
        Pedido pedidoSalvo = pedidosEmMemoria.salvar(Pedido.novo().adicionarItem(CAFE_500));

        service.adicionar(pedidoSalvo.id(), CAFE_500_UNIDADE);

        assertThat(pedidosEmMemoria.buscarPorId(pedidoSalvo.id()).orElseThrow().itens()).hasSize(2);
    }

    @Test
    void pedidoInexistenteLancaPedidoNaoEncontrado() {
        assertThatThrownBy(() -> service.adicionar(UUID.randomUUID(), CAFE_500_UNIDADE))
                .isInstanceOf(PedidoNaoEncontradoException.class);
    }

    @Test
    void pedidoPagoRecusaItem() {
        Pedido pago = pedidosEmMemoria.salvar(Pedido.novo().adicionarItem(CAFE_500).pagar());

        assertThatThrownBy(() -> service.adicionar(pago.id(), CAFE_500_UNIDADE))
                .isInstanceOf(IllegalStateException.class);

        assertThat(pedidosEmMemoria.buscarPorId(pago.id()).orElseThrow().itens()).hasSize(1);
    }

    @Test
    void pedidoCanceladoRecusaItem() {
        Pedido cancelado = pedidosEmMemoria.salvar(Pedido.novo().adicionarItem(CAFE_500).cancelar());

        assertThatThrownBy(() -> service.adicionar(cancelado.id(), CAFE_500_UNIDADE))
                .isInstanceOf(IllegalStateException.class);

        assertThat(pedidosEmMemoria.buscarPorId(cancelado.id()).orElseThrow().itens()).hasSize(1);
    }

    @Test
    void quantidadeInvalidaRecusadaPeloDominio() {
        assertThatThrownBy(() -> new ItemPedido("CAFE-500", 0, new BigDecimal("18.90")))
                .isInstanceOf(ItemInvalidoException.class);
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
    }
}
