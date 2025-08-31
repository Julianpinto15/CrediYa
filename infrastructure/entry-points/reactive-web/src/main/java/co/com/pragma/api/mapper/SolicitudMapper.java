package co.com.pragma.api.mapper;

import co.com.pragma.api.dto.SolicitudCreateRequest;
import co.com.pragma.api.dto.SolicitudResponse;
import co.com.pragma.model.solicitud.Solicitud;

import co.com.pragma.model.solicitud.exceptions.InvalidLoanTypeException;
import co.com.pragma.model.solicitud.gateways.TipoPrestamoRepository;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class SolicitudMapper {

    private final TipoPrestamoRepository tipoPrestamoRepository;

    public SolicitudMapper(TipoPrestamoRepository tipoPrestamoRepository) {
        this.tipoPrestamoRepository = tipoPrestamoRepository;
    }

    public Mono<Solicitud> toDomain(SolicitudCreateRequest request) {
        return tipoPrestamoRepository.findByNombre(request.getTipoPrestamoNombre())
                .switchIfEmpty(Mono.error(new InvalidLoanTypeException("Tipo de préstamo inválido: " + request.getTipoPrestamoNombre())))
                .map(tipo -> Solicitud.builder()
                        .id(null)
                        .documentoIdentidad(request.getDocumentoIdentidad())
                        .monto(request.getMonto())
                        .plazo(request.getPlazo())
                        .tipoPrestamo(tipo)
                        .build());
    }

    public SolicitudResponse toResponse(Solicitud solicitud) {
        SolicitudResponse response = new SolicitudResponse();
        response.setId(solicitud.getId());
        response.setDocumentoIdentidad(solicitud.getDocumentoIdentidad());
        response.setMonto(solicitud.getMonto());
        response.setPlazo(solicitud.getPlazo());
        response.setFechaCreacion(solicitud.getFechaCreacion());

        if (solicitud.getTipoPrestamo() != null) {
            SolicitudResponse.TipoPrestamoResponse tp = new SolicitudResponse.TipoPrestamoResponse();
            tp.setId(solicitud.getTipoPrestamo().getId());
            tp.setNombre(solicitud.getTipoPrestamo().getNombre());
            response.setTipoPrestamo(tp);
        }

        if (solicitud.getEstado() != null) {
            SolicitudResponse.EstadoSolicitudResponse est = new SolicitudResponse.EstadoSolicitudResponse();
            est.setId(solicitud.getEstado().getId());
            est.setNombre(solicitud.getEstado().getNombre());
            est.setDescripcion(solicitud.getEstado().getDescripcion());
            response.setEstado(est);
        }

        return response;
    }
}