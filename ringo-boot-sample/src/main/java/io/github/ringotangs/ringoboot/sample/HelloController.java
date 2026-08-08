package io.github.ringotangs.ringoboot.sample;

import io.github.ringotangs.ringoboot.problem.ProblemException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
public class HelloController {

    @GetMapping("/user")
    public User user(@RequestParam(name = "id", defaultValue = "1") long id) {
        if (id <= 0) {
            throw new ProblemException(UserProblemType.INVALID_USER_ID);
        }
        if (id != 1) {
            throw ProblemException.withArguments(UserProblemType.USER_NOT_FOUND, id);
        }

        return User.builder().name("zs").age(18).build();
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public User createUser(@Valid @RequestBody CreateUserRequest request) {
        return User.builder().name(request.name()).age(request.age()).build();
    }

    @GetMapping("/validated-user")
    public User validatedUser(@RequestParam(name = "id") @Min(1) long id) {
        return User.builder().name("zs").age(18).build();
    }
}
