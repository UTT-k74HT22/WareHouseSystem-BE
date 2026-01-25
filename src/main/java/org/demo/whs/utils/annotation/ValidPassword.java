package org.demo.whs.utils.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.regex.Pattern;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Target({FIELD, METHOD, PARAMETER})
@Retention(RUNTIME)
@Constraint(validatedBy = ValidPassword.ValidPasswordValidator.class)
public @interface ValidPassword {

    String message() default "Password must be 8–50 characters and include uppercase, lowercase, digit and special character.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /** Validator Class */
    class ValidPasswordValidator implements ConstraintValidator<ValidPassword, String> {

        private static final Pattern PATTERN =
                Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).{8,50}$");
        private String message;

        @Override
        public void initialize(ValidPassword constraintAnnotation) {
            this.message = constraintAnnotation.message();
        }

        @Override
        public boolean isValid(String password, ConstraintValidatorContext context) {
            if (password == null) {
                return false;
            }
            boolean valid = PATTERN.matcher(password).matches();
            if (!valid) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(message)
                        .addConstraintViolation();
            }
            return valid;
        }
    }
}
