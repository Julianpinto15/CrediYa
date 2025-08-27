package co.com.pragma.api.mapper;

import co.com.pragma.api.dto.SolicitudCreateRequest;
import co.com.pragma.api.dto.SolicitudResponse;
import co.com.pragma.model.solicitud.Solicitud;

import java.util.UUID;

public class SolicitudMapper {

    public static Solicitud toDomain(SolicitudCreateRequest request) {
        return Solicitud.builder()
                .id(UUID.randomUUID()) // o lo genera la DB
                .documentoIdentidad(request.getDocumentoIdentidad())
                .monto(request.getMonto())
                .plazo(request.getPlazo())
                .tipoPrestamo(
                        co.com.pragma.model.solicitud.TipoPrestamo.builder()
                                .nombre(request.getTipoPrestamoNombre())
                                .build()
                )
                .build();
    }

    public static SolicitudResponse toResponse(Solicitud solicitud) {
        SolicitudResponse response = new SolicitudResponse();
        response.setId(solicitud.getId());
        response.setDocumentoIdentidad(solicitud.getDocumentoIdentidad());
        response.setMonto(solicitud.getMonto());
        response.setPlazo(solicitud.getPlazo());
        response.setFechaCreacion(solicitud.getFechaCreacion());

        SolicitudResponse.TipoPrestamoResponse tipo = new SolicitudResponse.TipoPrestamoResponse();
        tipo.setId(solicitud.getTipoPrestamo().getId());
        tipo.setNombre(solicitud.getTipoPrestamo().getNombre());
        response.setTipoPrestamo(tipo);

        SolicitudResponse.EstadoSolicitudResponse estado = new SolicitudResponse.EstadoSolicitudResponse();
        estado.setId(solicitud.getEstado().getId());
        estado.setNombre(solicitud.getEstado().getNombre());
        response.setEstado(estado);

        return response;
    }
}
