package co.com.pragma.api.mapper;

import co.com.pragma.api.dto.SolicitudCreateRequest;
import co.com.pragma.api.dto.SolicitudListadoResponse;
import co.com.pragma.api.dto.SolicitudResponse;
import co.com.pragma.model.solicitud.Solicitud;

import co.com.pragma.model.solicitud.exceptions.InvalidLoanTypeException;
import co.com.pragma.model.solicitud.exceptions.SolicitudValidationException;
import co.com.pragma.model.solicitud.gateways.TipoPrestamoRepository;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class SolicitudMapper {

    private final TipoPrestamoRepository tipoPrestamoRepository;

    public SolicitudMapper(TipoPrestamoRepository tipoPrestamoRepository) {
        this.tipoPrestamoRepository = tipoPrestamoRepository;
    }

    public Mono<Solicitud> toDomain(SolicitudCreateRequest request) {
        // Validaciones básicas primero
        if (request == null) {
            return Mono.error(new SolicitudValidationException("La solicitud no puede ser nula"));
        }

        if (request.getDocumentoIdentidad() == null || request.getDocumentoIdentidad().trim().isEmpty()) {
            return Mono.error(new SolicitudValidationException("El documento de identidad es obligatorio"));
        }

        if (request.getMonto() == null || request.getMonto().compareTo(BigDecimal.ZERO) <= 0) {
            return Mono.error(new SolicitudValidationException("El monto debe ser mayor a cero"));
        }

        if (request.getPlazo() == null || request.getPlazo() <= 0) {
            return Mono.error(new SolicitudValidationException("El plazo debe ser mayor a cero"));
        }

        if (request.getTipoPrestamoNombre() == null || request.getTipoPrestamoNombre().trim().isEmpty()) {
            return Mono.error(new SolicitudValidationException("El tipo de préstamo es obligatorio"));
        }

        return tipoPrestamoRepository.findByNombre(request.getTipoPrestamoNombre().trim())
                .switchIfEmpty(Mono.error(new InvalidLoanTypeException("Tipo de préstamo inválido: " + request.getTipoPrestamoNombre())))
                .map(tipo -> Solicitud.builder()
                        .id(null)
                        .documentoIdentidad(request.getDocumentoIdentidad().trim())
                        .monto(request.getMonto())
                        .plazo(request.getPlazo())
                        .tipoPrestamo(tipo)
                        .build());
    }

    public SolicitudResponse toResponse(Solicitud solicitud) {
        if (solicitud == null) {
            throw new IllegalArgumentException("La solicitud no puede ser nula");
        }

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

    public static SolicitudListadoResponse.SolicitudItem toSolicitudItem(Solicitud solicitud) {
        return new SolicitudListadoResponse.SolicitudItem(
                solicitud.getId(),
                solicitud.getMonto(),
                solicitud.getPlazo(),
                solicitud.getEmailCliente(),
                solicitud.getNombreCliente(),
                solicitud.getTipoPrestamo() != null ? solicitud.getTipoPrestamo().getNombre() : null,
                solicitud.getTasaInteres(),
                solicitud.getEstado() != null ? solicitud.getEstado().getNombre() : "Sin estado",
                solicitud.getSalarioCliente(),
                calcularMontoMensual(solicitud.getMonto(), solicitud.getPlazo(), solicitud.getTasaInteres()),
                solicitud.getFechaCreacion()
        );
    }

    private static BigDecimal calcularMontoMensual(BigDecimal monto, Integer plazo, BigDecimal tasaInteres) {
        if (monto == null || plazo == null || tasaInteres == null || plazo <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal tasaMensual = tasaInteres.divide(BigDecimal.valueOf(100 * 12), 8, RoundingMode.HALF_UP);
        if (tasaMensual.compareTo(BigDecimal.ZERO) == 0) {
            return monto.divide(BigDecimal.valueOf(plazo), 2, RoundingMode.HALF_UP);
        }
        BigDecimal unoPlusTasa = BigDecimal.ONE.add(tasaMensual);
        BigDecimal factorPotencia = unoPlusTasa.pow(plazo);
        BigDecimal numerador = monto.multiply(tasaMensual).multiply(factorPotencia);
        BigDecimal denominador = factorPotencia.subtract(BigDecimal.ONE);
        return numerador.divide(denominador, 2, RoundingMode.HALF_UP);
    }

}