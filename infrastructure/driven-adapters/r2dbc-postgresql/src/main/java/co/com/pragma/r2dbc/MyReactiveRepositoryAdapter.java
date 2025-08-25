package co.com.pragma.r2dbc;

import co.com.pragma.model.user.User;
import co.com.pragma.model.user.gateways.UserRepository;
import co.com.pragma.r2dbc.helper.ReactiveAdapterOperations;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Repository
public class MyReactiveRepositoryAdapter extends ReactiveAdapterOperations<User,
    UserData,
    String,
    MyReactiveRepository
> implements UserRepository {
    public MyReactiveRepositoryAdapter(MyReactiveRepository repository, ObjectMapper mapper) {
        // Como tu User tiene @Builder, puedes usar mapBuilder:
        super(repository, mapper, d -> mapper.mapBuilder(d, User.UserBuilder.class).build());
        // Si no usaras builder sería:
        // super(repository, mapper, d -> mapper.map(d, User.class));
    }

    @Override
    public Mono<Boolean> existsByCorreo(String correo) {
        return repository.existsByCorreoElectronico(correo);
    }

    @Override
    @Transactional
    public Mono<User> save(User user) {
        return super.save(user);
    }

}
