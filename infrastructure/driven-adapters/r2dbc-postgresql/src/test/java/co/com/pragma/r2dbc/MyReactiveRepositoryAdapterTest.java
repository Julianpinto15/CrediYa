package co.com.pragma.r2dbc;

import co.com.pragma.model.user.User;
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

    private MyReactiveRepositoryAdapter repositoryAdapter;

    @Mock
    private MyReactiveRepository repository;

    @Mock
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        repositoryAdapter = new MyReactiveRepositoryAdapter(repository, mapper);
    }

    @Test
    void shouldReturnTrueWhenEmailExists() {
        // Given
        String email = "test@example.com";
        when(repository.existsByCorreoElectronico(email)).thenReturn(Mono.just(true));

        // When
        Mono<Boolean> result = repositoryAdapter.existsByCorreo(email);

        // Then
        StepVerifier.create(result)
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void shouldReturnFalseWhenEmailDoesNotExist() {
        // Given
        String email = "nonexistent@example.com";
        when(repository.existsByCorreoElectronico(email)).thenReturn(Mono.just(false));

        // When
        Mono<Boolean> result = repositoryAdapter.existsByCorreo(email);

        // Then
        StepVerifier.create(result)
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void shouldSaveUserSuccessfully() {
        // Given
        User user = User.builder()
                .correoElectronico("test@example.com")
                .nombres("Test User")
                .apellidos("Test Apellido")
                .documentoIdentidad("12345678")
                .telefono("3001234567")
                .fechaNacimiento(LocalDate.parse("1990-01-01"))
                .build();

        UserData userData = new UserData();
        userData.setCorreoElectronico("test@example.com");
        userData.setNombres("Test User");
        userData.setApellidos("Test Apellido");
        userData.setDocumentoIdentidad("12345678");
        userData.setTelefono("3001234567");
        userData.setFechaNacimiento(LocalDate.of(1990, 1, 1));

        UserData savedUserData = new UserData();
        savedUserData.setId("generated-id");
        savedUserData.setCorreoElectronico("test@example.com");
        savedUserData.setNombres("Test User");
        savedUserData.setApellidos("Test Apellido");
        savedUserData.setDocumentoIdentidad("12345678");
        savedUserData.setTelefono("3001234567");
        savedUserData.setFechaNacimiento(LocalDate.of(1990, 1, 1));

        User savedUser = User.builder()
                .id("generated-id")
                .correoElectronico("test@example.com")
                .nombres("Test User")
                .apellidos("Test Apellido")
                .documentoIdentidad("12345678")
                .telefono("3001234567")
                .fechaNacimiento(LocalDate.parse("1998-03-21"))
                .build();

        when(mapper.map(eq(user), eq(UserData.class))).thenReturn(userData);
        when(repository.save(eq(userData))).thenReturn(Mono.just(savedUserData));
        when(mapper.mapBuilder(any(UserData.class), eq(User.UserBuilder.class)))
                .thenReturn(savedUser.toBuilder());

        // When
        Mono<User> result = repositoryAdapter.save(user);

        // Then
        StepVerifier.create(result)
                .expectNextMatches(savedUserResult ->
                        savedUserResult.getId().equals("generated-id") &&
                                savedUserResult.getCorreoElectronico().equals("test@example.com")
                )
                .verifyComplete();
    }

    @Test
    void shouldReturnTrueWhenDocumentExists() {
        // Given
        String documentoIdentidad = "12345678";
        UserData userData = new UserData();
        userData.setId("user-id");
        userData.setDocumentoIdentidad(documentoIdentidad);
        userData.setCorreoElectronico("test@example.com");

        when(repository.findByDocumentoIdentidad(documentoIdentidad))
                .thenReturn(Mono.just(userData));

        // When
        Mono<Boolean> result = repositoryAdapter.existsByDocumentoIdentidad(documentoIdentidad);

        // Then
        StepVerifier.create(result)
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void shouldReturnFalseWhenDocumentDoesNotExist() {
        // Given
        String documentoIdentidad = "87654321";
        when(repository.findByDocumentoIdentidad(documentoIdentidad))
                .thenReturn(Mono.empty());

        // When
        Mono<Boolean> result = repositoryAdapter.existsByDocumentoIdentidad(documentoIdentidad);

        // Then
        StepVerifier.create(result)
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void shouldHandleErrorWhenCheckingEmailExists() {
        // Given
        String email = "error@example.com";
        when(repository.existsByCorreoElectronico(email))
                .thenReturn(Mono.error(new RuntimeException("Database connection error")));

        // When
        Mono<Boolean> result = repositoryAdapter.existsByCorreo(email);

        // Then
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void shouldHandleErrorWhenSaving() {
        // Given
        User user = User.builder()
                .correoElectronico("test@example.com")
                .nombres("Test User")
                .build();

        UserData userData = new UserData();
        userData.setCorreoElectronico("test@example.com");
        userData.setNombres("Test User");

        when(mapper.map(eq(user), eq(UserData.class))).thenReturn(userData);
        when(repository.save(eq(userData)))
                .thenReturn(Mono.error(new RuntimeException("Save failed")));

        // When
        Mono<User> result = repositoryAdapter.save(user);

        // Then
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();
    }
}