package br.com.pedidos.api.domain.pedido;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PedidoTest {

    private static final ItemPedido CAFE_500 = new ItemPedido("CAFE-500", 2, new BigDecimal("18.90"));

    @Test
    void pedidoNovoComecaAbertoVazioComId() {
        Pedido pedido = Pedido.novo();

        assertThat(pedido.status()).isEqualTo(StatusPedido.ABERTO);
        assertThat(pedido.itens()).isEmpty();
        assertThat(pedido.id()).isNotNull();
    }

    @Test
    void adicionarItemDevolveNovoPedidoSemAlterarOriginal() {
        Pedido original = Pedido.novo();

        Pedido comItem = original.adicionarItem(CAFE_500);

        assertThat(comItem).isNotSameAs(original);
        assertThat(original.itens()).isEmpty();
        assertThat(comItem.itens()).containsExactly(CAFE_500);
    }

    @Test
    void abertoAceitaPagar() {
        Pedido pedido = Pedido.novo().pagar();

        assertThat(pedido.status()).isEqualTo(StatusPedido.PAGO);
    }

    @Test
    void abertoAceitaCancelar() {
        Pedido pedido = Pedido.novo().cancelar();

        assertThat(pedido.status()).isEqualTo(StatusPedido.CANCELADO);
    }

    @Test
    void pagoNaoAceitaNovoItem() {
        Pedido pago = Pedido.novo().pagar();

        assertThatThrownBy(() -> pago.adicionarItem(CAFE_500))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void canceladoNaoAceitaNovoItem() {
        Pedido cancelado = Pedido.novo().cancelar();

        assertThatThrownBy(() -> cancelado.adicionarItem(CAFE_500))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void pagoNaoAceitaPagarDeNovo() {
        Pedido pago = Pedido.novo().pagar();

        assertThatThrownBy(pago::pagar)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void pagoNaoAceitaCancelar() {
        Pedido pago = Pedido.novo().pagar();

        assertThatThrownBy(pago::cancelar)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void canceladoNaoAceitaCancelarDeNovo() {
        Pedido cancelado = Pedido.novo().cancelar();

        assertThatThrownBy(cancelado::cancelar)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void canceladoNaoAceitaPagar() {
        Pedido cancelado = Pedido.novo().cancelar();

        assertThatThrownBy(cancelado::pagar)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void listaDeItensNaoPodeSerModificadaPorFora() {
        Pedido pedido = Pedido.novo().adicionarItem(CAFE_500);

        assertThatThrownBy(() -> pedido.itens().add(CAFE_500))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(pedido.itens()).containsExactly(CAFE_500);
    }

    @Test
    void cenarioDeReferenciaDaAula() {
        Pedido pedidoDoClienteC1 = Pedido.novo().adicionarItem(CAFE_500);

        assertThat(pedidoDoClienteC1.itens()).hasSize(1);
        assertThat(pedidoDoClienteC1.itens().get(0).total()).isEqualByComparingTo(new BigDecimal("37.80"));
    }
}
