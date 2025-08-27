package co.com.pragma.r2dbc.adapter;

import co.com.pragma.model.solicitud.TipoPrestamo;
import co.com.pragma.model.solicitud.gateways.TipoPrestamoRepository;
import co.com.pragma.r2dbc.data.TipoPrestamoData;
import co.com.pragma.r2dbc.helper.ReactiveAdapterOperations;
import co.com.pragma.r2dbc.repository.TipoPrestamoReactiveRepository;
import lombok.extern.slf4j.Slf4j;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Repository
public class TipoPrestamoReactiveRepositoryAdapter
        extends ReactiveAdapterOperations<TipoPrestamo, TipoPrestamoData, UUID, TipoPrestamoReactiveRepository>
        implements TipoPrestamoRepository {

    private final TipoPrestamoReactiveRepository repository;
    private final ObjectMapper mapper;

    public TipoPrestamoReactiveRepositoryAdapter(TipoPrestamoReactiveRepository repository, ObjectMapper mapper) {
        super(repository, mapper, d -> mapper.mapBuilder(d, TipoPrestamo.TipoPrestamoBuilder.class).build());
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Mono<TipoPrestamo> findByNombre(String nombre) {
        log.debug("Buscando tipo de préstamo por nombre: {}", nombre);
        return repository.findByNombre(nombre)
                .map(data -> mapper.mapBuilder(data, TipoPrestamo.TipoPrestamoBuilder.class).build())
                .doOnSuccess(tipo -> log.debug("Tipo de préstamo encontrado: {}", tipo.getNombre()))
                .doOnError(error -> log.error("Error buscando tipo de préstamo por nombre {}: {}", nombre, error.getMessage()));
    }
}