package co.com.pragma.r2dbc;

import co.com.pragma.model.solicitud.Solicitud;

import co.com.pragma.model.solicitud.gateways.SolicitudRepository;
import co.com.pragma.r2dbc.helper.ReactiveAdapterOperations;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public class SolicitudReactiveRepositoryAdapter
        extends ReactiveAdapterOperations<Solicitud, SolicitudData, UUID, SolicitudReactiveRepository>
        implements SolicitudRepository {

    public SolicitudReactiveRepositoryAdapter(SolicitudReactiveRepository repository, ObjectMapper mapper) {
        // igual que con User: si tu Solicitud tiene @Builder, usa mapBuilder
        super(repository, mapper, d -> mapper.mapBuilder(d, Solicitud.SolicitudBuilder.class).build());
        // si no tienes builder: super(repository, mapper, d -> mapper.map(d, Solicitud.class));
    }

    @Override
    public Mono<Boolean> existsByDocumentoIdentidad(String documentoIdentidad) {
        return repository.existsByDocumentoIdentidad(documentoIdentidad);
    }

    @Override
    public Mono<Boolean> isValidLoanType(String tipoPrestamo) {
        // 👇 este método lo usas en tu UseCase para validar préstamos
        return repository.existsByTipoPrestamo(tipoPrestamo);
    }

    @Override
    @Transactional
    public Mono<Solicitud> save(Solicitud solicitud) {
        return super.save(solicitud);
    }
}
