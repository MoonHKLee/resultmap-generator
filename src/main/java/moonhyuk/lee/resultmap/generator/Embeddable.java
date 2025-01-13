package moonhyuk.lee.resultmap.generator;

import java.lang.annotation.*;

/**
 * Nested/Embedded 클래스라는 의미로 사용.
 * (JPA의 @Embeddable과 유사한 개념)
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Embeddable {
}
