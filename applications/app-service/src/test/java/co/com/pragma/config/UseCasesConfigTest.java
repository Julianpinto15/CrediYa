package co.com.pragma.config;

import co.com.pragma.model.solicitud.gateways.ClienteGateway;
import co.com.pragma.model.solicitud.gateways.EstadoSolicitudRepository;
import co.com.pragma.model.solicitud.gateways.SolicitudRepository;
import co.com.pragma.model.user.gateways.UserRepository;
import co.com.pragma.usecase.solicitud.SolicitudUseCase;
import co.com.pragma.usecase.user.UserUseCase;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = UseCasesConfig.class)
class UseCasesConfigTest {

    @Autowired
    private UserUseCase userUseCase;

    @Autowired
    private SolicitudUseCase solicitudUseCase;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SolicitudRepository solicitudRepository;

    @Mock
    private EstadoSolicitudRepository estadoSolicitudRepository;

    @Mock
    private ClienteGateway clienteGateway;

    @Test
    void testUseCaseBeansExist() {
        assertNotNull(userUseCase, "UserUseCase bean should be created");
       // assertNotNull(solicitudUseCase, "SolicitudUseCase bean should be created");
    }
}