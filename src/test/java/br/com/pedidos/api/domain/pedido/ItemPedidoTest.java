package br.com.pedidos.api.domain.pedido;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ItemPedidoTest {

    @Test
    void aceitaQuantidadePositiva() {
        ItemPedido item = new ItemPedido("CAFE-500", 2, new BigDecimal("18.90"));

        assertThat(item.quantidade()).isEqualTo(2);
    }

    @Test
    void rejeitaQuantidadeZero() {
        assertThatThrownBy(() -> new ItemPedido("CAFE-500", 0, new BigDecimal("18.90")))
                .isInstanceOf(ItemInvalidoException.class);
    }

    @Test
    void rejeitaQuantidadeNegativa() {
        assertThatThrownBy(() -> new ItemPedido("CAFE-500", -1, new BigDecimal("18.90")))
                .isInstanceOf(ItemInvalidoException.class);
    }

    @Test
    void aceitaPrecoPositivo() {
        ItemPedido item = new ItemPedido("CAFE-500", 2, new BigDecimal("18.90"));

        assertThat(item.precoUnitario()).isEqualTo(new BigDecimal("18.90"));
    }

    @Test
    void rejeitaPrecoZero() {
        assertThatThrownBy(() -> new ItemPedido("CAFE-500", 2, BigDecimal.ZERO))
                .isInstanceOf(ItemInvalidoException.class);
    }

    @Test
    void rejeitaPrecoNegativo() {
        assertThatThrownBy(() -> new ItemPedido("CAFE-500", 2, new BigDecimal("-1.00")))
                .isInstanceOf(ItemInvalidoException.class);
    }

    @Test
    void totalDoItemEDerivadoDeQuantidadeEPreco() {
        ItemPedido item = new ItemPedido("CAFE-500", 2, new BigDecimal("18.90"));

        assertThat(item.total()).isEqualByComparingTo(new BigDecimal("37.80"));
    }

    @Test
    void totalDoItemEBigDecimal() {
        ItemPedido item = new ItemPedido("CAFE-500", 2, new BigDecimal("18.90"));

        assertThat(item.total()).isInstanceOf(BigDecimal.class);
    }

    @Test
    void cenarioDeReferenciaDaAula() {
        ItemPedido item = new ItemPedido("CAFE-500", 2, new BigDecimal("18.90"));

        assertThat(item.total()).isEqualByComparingTo(new BigDecimal("37.80"));
    }
}
