package oidc.model;

import java.util.Date;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * A container for a login request
 */
@Getter
@Setter
public class CreateUserRequest {


    @NotNull(message = "Can't be empty")
    @Size(min=1, message = "Must not be empty")
    private String username;

    @NotNull(message = "Can't be empty")
    @Size(min = 4, message = "Must have more than 4 symbols")
    private String password;

    private String passwordCheck;
    private boolean passwordsEqual;

    @NotNull(message = "Can't be empty")
    @Size(min=1, message = "Must not be empty")
    private String firstName;

    @NotNull(message = "Can't be empty")
    @Size(min=1, message = "Must not be empty")
    private String lastName;

    @NotNull(message = "Can't be empty")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date birthdate;

    @NotNull(message = "Can't be empty")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date vaccinationdate;

    @NotNull(message = "Can't be empty")
    @Size(min=1, message = "Must not be empty")
    private String email;

    @AssertTrue(message = "Passwords should match")
    public boolean isPasswordsEqual() {
        if(password != null){
            return password.equals(passwordCheck);
        }
        return false;
    }
}
