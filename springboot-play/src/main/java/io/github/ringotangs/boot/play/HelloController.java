package io.github.ringotangs.boot.play;

import io.github.ringotangs.commons.core.BusinessException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping("/user")
    public User user(@RequestParam(name = "id", defaultValue = "1") long id) {
        if (id <= 0) {
            throw new BusinessException(UserProblemType.INVALID_USER_ID);
        }
        if (id != 1) {
            throw new BusinessException(UserProblemType.USER_NOT_FOUND);
        }

        return User.builder().name("zs").age(18).build();
    }

}
