package tk.youngdk.querydsl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;
import tk.youngdk.querydsl.entity.Hello;
import tk.youngdk.querydsl.entity.QHello;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@SpringBootTest
@Transactional
public class InitTest {

    @PersistenceContext
    EntityManager em;

    @Test
    @DisplayName("검증 테스트")
    @Commit
    public void init() throws Exception {
        System.out.println("InitTest.init");
        //given
        Hello hello = new Hello();
        em.persist(hello);

        System.out.println("hello = " + hello);

        //when
        JPAQueryFactory query = new JPAQueryFactory(em);
        QHello qHello = new QHello("h");

        Hello result = query
                .selectFrom(qHello)
                .fetchOne();

        System.out.println("result = " + result);
        System.out.println("hello = " + hello);

        Assertions.assertThat(result).isEqualTo(hello);
        Assertions.assertThat(result.getId()).isEqualTo(hello.getId());

        //then
    }
}
