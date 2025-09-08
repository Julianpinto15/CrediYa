package co.com.pragma.model.solicitud.gateways;

import co.com.pragma.model.solicitud.Solicitud;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface SolicitudRepository {
    Mono<Solicitud> save(Solicitud solicitud);
    Mono<Boolean> existsByDocumentoIdentidad(String documentoIdentidad);
    Mono<Boolean> isValidLoanType(String tipoPrestamo);

    /**
     * Busca solicitudes por estados con paginación
     * @param estados Lista de nombres de estados a filtrar
     * @param page Número de página (empezando en 0)
     * @param size Tamaño de página
     * @return Flux de solicitudes paginadas
     */
    Flux<Solicitud> findByEstadosWithPagination(List<String> estados, int page, int size);

    /**
     * Cuenta el total de solicitudes por estados específicos
     * @param estados Lista de nombres de estados a contar
     * @return Mono con el total de solicitudes
     */
    Mono<Long> countByEstados(List<String> estados);
}
