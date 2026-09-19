package br.com.pedidos.api.config;

import br.com.pedidos.api.aplicacao.pedido.AdicionarItem;
import br.com.pedidos.api.aplicacao.pedido.AdicionarItemService;
import br.com.pedidos.api.aplicacao.pedido.CriarPedido;
import br.com.pedidos.api.aplicacao.pedido.CriarPedidoService;
import br.com.pedidos.api.aplicacao.pedido.Pedidos;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CasosDeUsoConfig {

    @Bean
    public CriarPedido criarPedido(Pedidos pedidos) {
        return new CriarPedidoService(pedidos);
    }

    @Bean
    public AdicionarItem adicionarItem(Pedidos pedidos) {
        return new AdicionarItemService(pedidos);
    }
}
