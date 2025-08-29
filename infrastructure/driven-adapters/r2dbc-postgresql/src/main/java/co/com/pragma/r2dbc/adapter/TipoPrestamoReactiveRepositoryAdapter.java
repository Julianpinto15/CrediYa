package co.com.pragma.r2dbc.adapter;

import co.com.pragma.model.solicitud.TipoPrestamo;
import co.com.pragma.model.solicitud.gateways.TipoPrestamoRepository;
import co.com.pragma.r2dbc.data.TipoPrestamoData;
import co.com.pragma.r2dbc.helper.ReactiveAdapterOperations;
import co.com.pragma.r2dbc.repository.TipoPrestamoReactiveRepository;
import lombok.extern.slf4j.Slf4j;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.function.Function;

@Slf4j
@Repository
public class TipoPrestamoReactiveRepositoryAdapter
        extends ReactiveAdapterOperations<TipoPrestamo, TipoPrestamoData, UUID, TipoPrestamoReactiveRepository>
        implements TipoPrestamoRepository {

    private final TipoPrestamoReactiveRepository repository;
    private final DatabaseClient databaseClient;

    // Método estático para el mapeo Data -> Domain
    private static final Function<TipoPrestamoData, TipoPrestamo> TO_DOMAIN_MAPPER = data -> {
        log.debug("Mapeando TipoPrestamoData: id={}, nombre={}, validacionAutomatica={}",
                data.getId(), data.getNombre(), data.getValidacionAutomatica());

        return TipoPrestamo.builder()
                .id(data.getId())
                .nombre(data.getNombre())
                .montoMinimo(data.getMontoMinimo())
                .montoMaximo(data.getMontoMaximo())
                .tasaInteres(data.getTasaInteres())
                .validacionAutomatica(Boolean.TRUE.equals(data.getValidacionAutomatica()))
                .build();
    };

    public TipoPrestamoReactiveRepositoryAdapter(TipoPrestamoReactiveRepository repository, ObjectMapper mapper, DatabaseClient databaseClient) {
        super(repository, mapper, TO_DOMAIN_MAPPER);
        this.repository = repository;
        this.databaseClient = databaseClient;
        log.info("TipoPrestamoReactiveRepositoryAdapter inicializado correctamente");
    }

    public Mono<TipoPrestamoData> debugFindByNombre(String nombre) {
        return databaseClient.sql("SELECT * FROM tipos_prestamo WHERE nombre = $1")
                .bind(0, nombre)
                .map((row, metadata) -> {
                    Object rawValue = row.get("validacion_automatica");
                    System.out.println("Raw validacion_automatica: " + rawValue +
                            " | Type: " + (rawValue != null ? rawValue.getClass().getName() : "null"));

                    return new TipoPrestamoData(
                            row.get("id", UUID.class),
                            row.get("nombre", String.class),
                            row.get("montoMinimo", BigDecimal.class),
                            row.get("montoMaximo", BigDecimal.class),
                            row.get("tasaInteres", BigDecimal.class),
                            row.get("validacionAutomatica", Boolean.class) // 👈 aquí veremos si castea bien
                    );
                })
                .first();
    }

    @Override
    public Mono<TipoPrestamo> findByNombre(String nombre) {
        log.debug("Buscando tipo de préstamo por nombre: {}", nombre);
        return repository.findByNombre(nombre)
                .doOnNext(data -> log.debug("TipoPrestamoData encontrado: {}", data))
                .map(TO_DOMAIN_MAPPER)  // Usar directamente la función estática
                .doOnNext(tipo -> log.debug("Tipo de préstamo mapeado: {}", tipo.getNombre()))
                .doOnError(error -> log.error("Error buscando tipo de préstamo por nombre '{}': {}", nombre, error.getMessage()));
    }

    @Override
    public Mono<TipoPrestamo> findById(UUID id) {
        log.debug("Buscando tipo de préstamo por ID: {}", id);
        return repository.findById(id)
                .doOnNext(data -> log.debug("TipoPrestamoData encontrado: {}", data))
                .map(TO_DOMAIN_MAPPER)  // Usar directamente la función estática
                .doOnNext(tipo -> log.debug("Tipo de préstamo mapeado: {}", tipo.getNombre()))
                .doOnError(error -> log.error("Error buscando tipo de préstamo por ID {}: {}", id, error.getMessage()));
    }

    // Override del método toData para usar mapeo manual en lugar del ObjectMapper
    @Override
    protected TipoPrestamoData toData(TipoPrestamo tipoPrestamo) {
        log.debug("Mapeando TipoPrestamo a TipoPrestamoData: {}", tipoPrestamo.getNombre());

        TipoPrestamoData data = new TipoPrestamoData();
        data.setId(tipoPrestamo.getId());
        data.setNombre(tipoPrestamo.getNombre());
        data.setMontoMinimo(tipoPrestamo.getMontoMinimo());
        data.setMontoMaximo(tipoPrestamo.getMontoMaximo());
        data.setTasaInteres(tipoPrestamo.getTasaInteres());
        data.setValidacionAutomatica(tipoPrestamo.getValidacionAutomatica());

        log.debug("TipoPrestamoData mapeado: {}", data);
        return data;
    }

    // Override del método toEntity para usar mapeo manual
    @Override
    protected TipoPrestamo toEntity(TipoPrestamoData data) {
        return data != null ? TO_DOMAIN_MAPPER.apply(data) : null;
    }
}