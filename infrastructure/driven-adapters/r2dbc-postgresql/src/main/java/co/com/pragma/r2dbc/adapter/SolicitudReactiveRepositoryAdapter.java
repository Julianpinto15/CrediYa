package co.com.pragma.r2dbc.adapter;

import co.com.pragma.model.solicitud.EstadoSolicitud;
import co.com.pragma.model.solicitud.Solicitud;
import co.com.pragma.model.solicitud.TipoPrestamo;
import co.com.pragma.model.solicitud.gateways.EstadoSolicitudRepository;
import co.com.pragma.model.solicitud.gateways.SolicitudRepository;
import co.com.pragma.model.solicitud.gateways.TipoPrestamoRepository;
import co.com.pragma.r2dbc.data.SolicitudData;
import co.com.pragma.r2dbc.helper.ReactiveAdapterOperations;
import co.com.pragma.r2dbc.repository.SolicitudReactiveRepository;
import lombok.extern.slf4j.Slf4j;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.data.domain.Sort;

import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.query.Criteria;
import java.util.List;

import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Repository
public class SolicitudReactiveRepositoryAdapter
        extends ReactiveAdapterOperations<Solicitud, SolicitudData, UUID, SolicitudReactiveRepository>
        implements SolicitudRepository {

    private final TipoPrestamoRepository tipoPrestamoRepository;
    private final EstadoSolicitudRepository estadoSolicitudRepository;
    private final SolicitudReactiveRepository repository;
    private final TransactionalOperator transactionalOperator;  // Inyéctalo en tu config
    private final R2dbcEntityTemplate r2dbcEntityTemplate;

    public SolicitudReactiveRepositoryAdapter(SolicitudReactiveRepository repository,
                                              TipoPrestamoRepository tipoPrestamoRepository,
                                              EstadoSolicitudRepository estadoSolicitudRepository,
                                              ObjectMapper mapper,
                                              R2dbcEntityTemplate r2dbcEntityTemplate, TransactionalOperator transactionalOperator) {
        super(repository, mapper, d -> Solicitud.builder()
                .id(d.getId())
                .documentoIdentidad(d.getDocumentoIdentidad())
                .monto(d.getMonto())
                .plazo(d.getPlazo())
                .fechaCreacion(d.getFechaCreacion())
                .build());
        this.tipoPrestamoRepository = tipoPrestamoRepository;
        this.estadoSolicitudRepository = estadoSolicitudRepository;
        this.repository = repository;
        this.r2dbcEntityTemplate = r2dbcEntityTemplate;
        this.transactionalOperator = transactionalOperator;
    }


    private Mono<Solicitud> buildSolicitudFromData(SolicitudData data) {
        log.debug("Construyendo Solicitud desde SolicitudData ID: {}", data.getId());

        Solicitud.SolicitudBuilder builder = Solicitud.builder()
                .id(data.getId())
                .documentoIdentidad(data.getDocumentoIdentidad())
                .monto(data.getMonto())
                .plazo(data.getPlazo())
                .fechaCreacion(data.getFechaCreacion());

        Mono<TipoPrestamo> tipoPrestamoMono = data.getTipoPrestamoId() != null
                ? tipoPrestamoRepository.findById(data.getTipoPrestamoId())
                : Mono.just(TipoPrestamo.builder().build());

        Mono<EstadoSolicitud> estadoMono = data.getEstadoSolicitudId() != null
                ? estadoSolicitudRepository.findById(data.getEstadoSolicitudId())
                : Mono.just(EstadoSolicitud.builder().build());

        return Mono.zip(tipoPrestamoMono, estadoMono)
                .map(tuple -> builder
                        .tipoPrestamo(tuple.getT1())
                        .estado(tuple.getT2())
                        .build());
    }

    @Override
    public Mono<Boolean> isValidLoanType(String tipoPrestamo) {
        log.debug("Verificando validez del tipo de préstamo: {}", tipoPrestamo);
        return tipoPrestamoRepository.findByNombre(tipoPrestamo)
                .hasElement()
                .doOnNext(isValid -> log.debug("Tipo de préstamo {} es válido: {}", tipoPrestamo, isValid))
                .doOnError(error -> log.error("Error verificando tipo de préstamo {}: {}", tipoPrestamo, error.getMessage()));
    }

    @Override
    public Mono<Solicitud> save(Solicitud solicitud) {
        log.debug("Guardando solicitud: documento={}, tipoPrestamo={}, estado={}",
                solicitud.getDocumentoIdentidad(),
                solicitud.getTipoPrestamo() != null ? solicitud.getTipoPrestamo().getNombre() : "null",
                solicitud.getEstado() != null ? solicitud.getEstado().getNombre() : "null");

        SolicitudData data = toData(solicitud);

        return transactionalOperator.transactional(
                        r2dbcEntityTemplate.insert(data) // Cambiar a insert
                                .flatMap(this::buildSolicitudFromData)
                ).doOnSuccess(saved -> log.info("Solicitud guardada exitosamente con ID: {}", saved.getId()))
                .doOnError(error -> log.error("Error guardando solicitud: {}", error.getMessage(), error));
    }

    @Override
    public Mono<Boolean> existsByDocumentoIdentidad(String documentoIdentidad) {
        log.debug("Verificando existencia de solicitud por documento: {}", documentoIdentidad);
        return repository.existsByDocumentoIdentidad(documentoIdentidad);
    }

    protected SolicitudData toData(Solicitud solicitud) {
        UUID tipoPrestamoId = solicitud.getTipoPrestamo() != null ? solicitud.getTipoPrestamo().getId() : null;
        UUID estadoSolicitudId = solicitud.getEstado() != null ? solicitud.getEstado().getId() : null;

        return SolicitudData.builder()
                .id(null) // Forzar ID nulo para nuevas solicitudes
                .documentoIdentidad(solicitud.getDocumentoIdentidad())
                .monto(solicitud.getMonto())
                .plazo(solicitud.getPlazo())
                .tipoPrestamoId(tipoPrestamoId)
                .estadoSolicitudId(estadoSolicitudId)
                .fechaCreacion(solicitud.getFechaCreacion())
                .build();
    }

    @Override
    public Flux<Solicitud> findByEstadosWithPagination(List<String> estadosNombres, int page, int size) {
        log.debug("Buscando solicitudes por estados {} - página: {}, tamaño: {}", estadosNombres, page, size);

        // CORRECCIÓN CRÍTICA: Usar findByNombres en lugar de intentar usar UUIDs como nombres
        return estadoSolicitudRepository.findByNombres(estadosNombres)
                .map(EstadoSolicitud::getId)
                .collectList()
                .flatMapMany(estadoIds -> {
                    if (estadoIds.isEmpty()) {
                        log.debug("No se encontraron estados válidos, retornando vacío");
                        return Flux.empty();
                    }

                    log.debug("IDs de estados encontrados: {}", estadoIds);

                    Query query = Query.query(Criteria.where("estado_solicitud_id").in(estadoIds))
                            .sort(Sort.by(Sort.Direction.DESC, "fecha_creacion"))
                            .offset((long) page * size)
                            .limit(size);

                    return r2dbcEntityTemplate.select(SolicitudData.class)
                            .matching(query)
                            .all()
                            .flatMap(this::buildSolicitudFromData)
                            .doOnNext(solicitud -> log.debug("Solicitud encontrada: ID={}, Estado={}",
                                    solicitud.getId(),
                                    solicitud.getEstado() != null ? solicitud.getEstado().getNombre() : "null"));
                })
                .doOnComplete(() -> log.debug("Consulta paginada completada"))
                .doOnError(error -> log.error("Error al buscar solicitudes: {}", error.getMessage(), error));
    }

    @Override
    public Mono<Long> countByEstados(List<String> estadosNombres) {
        log.debug("Contando solicitudes por estados: {}", estadosNombres);

        // CORRECCIÓN CRÍTICA: Usar findByNombres en lugar de intentar usar UUIDs como nombres
        return estadoSolicitudRepository.findByNombres(estadosNombres)
                .map(EstadoSolicitud::getId)
                .collectList()
                .flatMap(estadoIds -> {
                    if (estadoIds.isEmpty()) {
                        log.debug("No se encontraron estados válidos para contar");
                        return Mono.just(0L);
                    }

                    log.debug("IDs de estados para contar: {}", estadoIds);

                    Query query = Query.query(Criteria.where("estado_solicitud_id").in(estadoIds));

                    return r2dbcEntityTemplate.count(query, SolicitudData.class)
                            .doOnNext(count -> log.debug("Total de solicitudes encontradas: {}", count));
                })
                .doOnError(error -> log.error("Error al contar solicitudes: {}", error.getMessage(), error));
    }

}

