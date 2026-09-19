package br.com.pedidos.api.adapter.saida.persistencia;

import br.com.pedidos.api.aplicacao.pedido.Pedidos;
import br.com.pedidos.api.domain.pedido.Pedido;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component
public class PedidosJpaAdapter implements Pedidos {

    private final PedidoJpaRepository repository;

    public PedidosJpaAdapter(PedidoJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public Pedido salvar(Pedido pedido) {
        PedidoJpaEntity entidade = repository.save(PedidoMapper.paraEntidade(pedido));
        return PedidoMapper.paraDominio(entidade);
    }

    @Override
    @Transactional
    public Optional<Pedido> buscarPorId(UUID id) {
        return repository.findById(id).map(PedidoMapper::paraDominio);
    }
}
