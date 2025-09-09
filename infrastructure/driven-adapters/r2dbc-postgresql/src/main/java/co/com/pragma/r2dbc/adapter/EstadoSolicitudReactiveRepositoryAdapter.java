package co.com.pragma.r2dbc.adapter;

import co.com.pragma.model.solicitud.EstadoSolicitud;
import co.com.pragma.model.solicitud.gateways.EstadoSolicitudRepository;
import co.com.pragma.r2dbc.data.EstadoSolicitudData;
import co.com.pragma.r2dbc.helper.ReactiveAdapterOperations;
import co.com.pragma.r2dbc.repository.EstadoSolicitudReactiveRepository;
import lombok.extern.slf4j.Slf4j;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Slf4j
@Repository
public class EstadoSolicitudReactiveRepositoryAdapter extends ReactiveAdapterOperations<EstadoSolicitud, EstadoSolicitudData, UUID, EstadoSolicitudReactiveRepository>
        implements EstadoSolicitudRepository {

    private final EstadoSolicitudReactiveRepository repository;
    private final ObjectMapper mapper;

    public EstadoSolicitudReactiveRepositoryAdapter(EstadoSolicitudReactiveRepository repository, ObjectMapper mapper) {
        super(repository, mapper, d -> mapper.mapBuilder(d, EstadoSolicitud.EstadoSolicitudBuilder.class).build());
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Mono<EstadoSolicitud> findByNombre(String nombre) {
        log.debug("Buscando estado por nombre: {}", nombre);
        return repository.findByNombre(nombre)
                .map(data -> mapper.mapBuilder(data, EstadoSolicitud.EstadoSolicitudBuilder.class).build())
                .doOnSuccess(estado -> log.debug("Estado encontrado: {}", estado.getNombre()))
                .doOnError(error -> log.error("Error buscando estado por nombre {}: {}", nombre, error.getMessage()));
    }

    @Override
    public Flux<EstadoSolicitud> findByNombres(List<String> nombres) {
        log.debug("Buscando estados por nombres: {}", nombres);

        if (nombres == null || nombres.isEmpty()) {
            log.debug("Lista de nombres vacía, retornando Flux vacío");
            return Flux.empty();
        }

        return repository.findByNombreIn(nombres)
                .map(data -> mapper.mapBuilder(data, EstadoSolicitud.EstadoSolicitudBuilder.class).build())
                .doOnNext(estado -> log.debug("Estado encontrado: {} con ID: {}", estado.getNombre(), estado.getId()))
                .doOnComplete(() -> log.debug("Completada búsqueda de estados por nombres: {}", nombres))
                .doOnError(error -> log.error("Error buscando estados por nombres {}: {}", nombres, error.getMessage()));
    }
}