package co.com.pragma.r2dbc;

import co.com.pragma.model.user.User;
import co.com.pragma.r2dbc.adapter.MyReactiveRepositoryAdapter;
import co.com.pragma.r2dbc.data.UserData;
import co.com.pragma.r2dbc.repository.MyReactiveRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.reactivecommons.utils.ObjectMapper;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MyReactiveRepositoryAdapterTest {

    @Mock
    private MyReactiveRepository repository;

    @Mock
    private ObjectMapper mapper;

    private MyReactiveRepositoryAdapter adapter;

    private User user;
    private UserData userData;

    @BeforeEach
    void setUp() {
        adapter = new MyReactiveRepositoryAdapter(repository, mapper);

        user = User.builder()
                .id("1")
                .nombres("Juan")
                .apellidos("Perez")
                .documentoIdentidad("123456789")
                .fechaNacimiento(LocalDate.of(1990, 1, 1))
                .direccion("Calle 123")
                .telefono("3101234567")
                .correoElectronico("juan@example.com")
                .salarioBase(BigDecimal.valueOf(1000000))
                .build();

        userData = new UserData();
        userData.setId(1);
        userData.setNombres("Juan");
        userData.setApellidos("Perez");
        userData.setDocumentoIdentidad("123456789");
        userData.setFechaNacimiento(LocalDate.of(1990, 1, 1));
        userData.setDireccion("Calle 123");
        userData.setTelefono("3101234567");
        userData.setCorreoElectronico("juan@example.com");
        userData.setSalarioBase(BigDecimal.valueOf(1000000));
    }

    @Test
    void save_success_returnsUser() {
        // Configuramos los mocks específicos para este test
        when(mapper.map(eq(user), eq(UserData.class))).thenReturn(userData);
        when(repository.save(any(UserData.class))).thenReturn(Mono.just(userData));

        // El mapBuilder debe devolver un User completo, no solo el builder
        User.UserBuilder userBuilder = User.builder()
                .id("1")
                .nombres("Juan")
                .apellidos("Perez")
                .documentoIdentidad("123456789")
                .fechaNacimiento(LocalDate.of(1990, 1, 1))
                .direccion("Calle 123")
                .telefono("3101234567")
                .correoElectronico("juan@example.com")
                .salarioBase(BigDecimal.valueOf(1000000));

        when(mapper.mapBuilder(eq(userData), eq(User.UserBuilder.class)))
                .thenReturn(userBuilder);

        StepVerifier.create(adapter.save(user))
                .expectNextMatches(savedUser ->
                        savedUser.getId().equals("1") &&
                                savedUser.getNombres().equals("Juan") &&
                                savedUser.getCorreoElectronico().equals("juan@example.com"))
                .verifyComplete();
    }

    @Test
    void existsByCorreo_exists_returnsTrue() {
        when(repository.existsByCorreoElectronico("juan@example.com")).thenReturn(Mono.just(true));

        StepVerifier.create(adapter.existsByCorreo("juan@example.com"))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void existsByCorreo_notExists_returnsFalse() {
        when(repository.existsByCorreoElectronico("notfound@example.com")).thenReturn(Mono.just(false));

        StepVerifier.create(adapter.existsByCorreo("notfound@example.com"))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void existsByDocumentoIdentidad_exists_returnsTrue() {
        when(repository.findByDocumentoIdentidad("123456789")).thenReturn(Mono.just(userData));

        StepVerifier.create(adapter.existsByDocumentoIdentidad("123456789"))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void existsByDocumentoIdentidad_notExists_returnsFalse() {
        when(repository.findByDocumentoIdentidad("999999999")).thenReturn(Mono.empty());

        StepVerifier.create(adapter.existsByDocumentoIdentidad("999999999"))
                .expectNext(false)
                .verifyComplete();
    }
}