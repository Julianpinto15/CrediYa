package co.com.pragma.r2dbc.adapter;

import co.com.pragma.model.solicitud.EstadoSolicitud;
import co.com.pragma.model.solicitud.Solicitud;
import co.com.pragma.model.solicitud.TipoPrestamo;
import co.com.pragma.model.solicitud.gateways.SolicitudRepository;
import co.com.pragma.model.solicitud.gateways.TipoPrestamoRepository;
import co.com.pragma.model.solicitud.gateways.EstadoSolicitudRepository;
import co.com.pragma.r2dbc.data.SolicitudData;
import co.com.pragma.r2dbc.helper.ReactiveAdapterOperations;
import co.com.pragma.r2dbc.repository.SolicitudReactiveRepository;
import lombok.extern.slf4j.Slf4j;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Repository
public class SolicitudReactiveRepositoryAdapter
        extends ReactiveAdapterOperations<Solicitud, SolicitudData, UUID, SolicitudReactiveRepository>
        implements SolicitudRepository {

    private final TipoPrestamoRepository tipoPrestamoRepository;
    private final EstadoSolicitudRepository estadoSolicitudRepository;
    private final SolicitudReactiveRepository repository;
    private final ObjectMapper mapper;

    public SolicitudReactiveRepositoryAdapter(SolicitudReactiveRepository repository,
                                              ObjectMapper mapper,
                                              TipoPrestamoRepository tipoPrestamoRepository,
                                              EstadoSolicitudRepository estadoSolicitudRepository) {
        super(repository, mapper, d -> mapper.map(d, Solicitud.class));
        this.tipoPrestamoRepository = tipoPrestamoRepository;
        this.estadoSolicitudRepository = estadoSolicitudRepository;
        this.repository = repository;
        this.mapper = mapper;
    }

    // Método estático para construir Solicitud desde SolicitudData
    private static Mono<Solicitud> buildSolicitudFromData(SolicitudData data,
                                                          ObjectMapper mapper,
                                                          TipoPrestamoRepository tipoPrestamoRepo,
                                                          EstadoSolicitudRepository estadoRepo) {
        log.debug("Construyendo Solicitud desde SolicitudData ID: {}", data.getId());

        Solicitud.SolicitudBuilder builder = Solicitud.builder()
                .id(data.getId())
                .documentoIdentidad(data.getDocumentoIdentidad())
                .monto(data.getMonto())
                .plazo(data.getPlazo())
                .fechaCreacion(data.getFechaCreacion());

        // Cargar relaciones de manera reactiva
        Mono<TipoPrestamo> tipoPrestamoMono = data.getTipoPrestamoId() != null
                ? tipoPrestamoRepo.findById(data.getTipoPrestamoId())
                .doOnNext(tipo -> log.debug("TipoPrestamo encontrado: {}", tipo.getNombre()))
                .doOnError(error -> log.error("Error buscando TipoPrestamo por ID {}: {}", data.getTipoPrestamoId(), error.getMessage()))
                : Mono.empty();

        Mono<EstadoSolicitud> estadoMono = data.getEstadoSolicitudId() != null
                ? estadoRepo.findById(data.getEstadoSolicitudId())
                .doOnNext(estado -> log.debug("EstadoSolicitud encontrado: {}", estado.getNombre()))
                .doOnError(error -> log.error("Error buscando EstadoSolicitud por ID {}: {}", data.getEstadoSolicitudId(), error.getMessage()))
                : Mono.empty();

        // CORRECCIÓN: No usar defaultIfEmpty(null), usar switchIfEmpty con un Mono que contenga el valor
        return Mono.zip(
                        tipoPrestamoMono.switchIfEmpty(Mono.fromCallable(() -> (TipoPrestamo) null)),
                        estadoMono.switchIfEmpty(Mono.fromCallable(() -> (EstadoSolicitud) null))
                )
                .map(tuple -> {
                    TipoPrestamo tipoPrestamo = tuple.getT1();
                    EstadoSolicitud estado = tuple.getT2();

                    log.debug("Construyendo Solicitud con tipoPrestamo: {} y estado: {}",
                            tipoPrestamo != null ? tipoPrestamo.getNombre() : "null",
                            estado != null ? estado.getNombre() : "null");

                    return builder
                            .tipoPrestamo(tipoPrestamo)
                            .estado(estado)
                            .build();
                })
                .switchIfEmpty(Mono.fromCallable(() -> builder.build()));
    }

    @Override
    @Transactional
    public Mono<Solicitud> save(Solicitud solicitud) {
        log.debug("Guardando solicitud: documento={}, tipoPrestamo={}, estado={}",
                solicitud.getDocumentoIdentidad(),
                solicitud.getTipoPrestamo() != null ? solicitud.getTipoPrestamo().getNombre() : "null",
                solicitud.getEstado() != null ? solicitud.getEstado().getNombre() : "null");

        // Convertir Solicitud a SolicitudData
        SolicitudData solicitudData = toData(solicitud);

        log.debug("SolicitudData creada: tipoPrestamoId={}, estadoSolicitudId={}",
                solicitudData.getTipoPrestamoId(), solicitudData.getEstadoSolicitudId());

        return repository.save(solicitudData)
                .doOnNext(savedData -> log.debug("Datos guardados en BD: ID={}", savedData.getId()))
                .flatMap(savedData -> buildSolicitudFromData(savedData, mapper, tipoPrestamoRepository, estadoSolicitudRepository))
                .doOnSuccess(saved -> log.info("Solicitud guardada exitosamente con ID: {}", saved.getId()))
                .doOnError(error -> log.error("Error guardando solicitud: {}", error.getMessage()));
    }

    @Override
    public Mono<Boolean> existsByDocumentoIdentidad(String documentoIdentidad) {
        log.debug("Verificando existencia de solicitud por documento: {}", documentoIdentidad);
        return repository.existsByDocumentoIdentidad(documentoIdentidad)
                .doOnNext(exists -> log.debug("Solicitud con documento {} existe: {}", documentoIdentidad, exists))
                .doOnError(error -> log.error("Error verificando existencia por documento {}: {}", documentoIdentidad, error.getMessage()));
    }

    @Override
    public Mono<Boolean> isValidLoanType(String tipoPrestamo) {
        log.debug("Verificando validez del tipo de préstamo: {}", tipoPrestamo);
        return tipoPrestamoRepository.findByNombre(tipoPrestamo)
                .hasElement()
                .doOnNext(isValid -> log.debug("Tipo de préstamo {} es válido: {}", tipoPrestamo, isValid))
                .doOnError(error -> log.error("Error verificando tipo de préstamo {}: {}", tipoPrestamo, error.getMessage()));
    }

    // Método para convertir Solicitud a SolicitudData
    @Override
    protected SolicitudData toData(Solicitud solicitud) {
        UUID tipoPrestamoId = solicitud.getTipoPrestamo() != null ? solicitud.getTipoPrestamo().getId() : null;
        UUID estadoSolicitudId = solicitud.getEstado() != null ? solicitud.getEstado().getId() : null;

        log.debug("Convirtiendo Solicitud a SolicitudData: tipoPrestamoId={}, estadoSolicitudId={}",
                tipoPrestamoId, estadoSolicitudId);

        return SolicitudData.builder()
                .id(solicitud.getId())
                .documentoIdentidad(solicitud.getDocumentoIdentidad())
                .monto(solicitud.getMonto())
                .plazo(solicitud.getPlazo())
                .tipoPrestamoId(tipoPrestamoId)
                .estadoSolicitudId(estadoSolicitudId)
                .fechaCreacion(solicitud.getFechaCreacion())
                .build();
    }
}