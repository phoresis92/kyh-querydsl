package tk.youngdk.querydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import tk.youngdk.querydsl.entity.Member;
import tk.youngdk.querydsl.entity.QTeam;
import tk.youngdk.querydsl.entity.Team;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static tk.youngdk.querydsl.entity.QMember.*;
import static tk.youngdk.querydsl.entity.QTeam.team;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @PersistenceContext
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach
    public void beforeEach(){
        queryFactory = new JPAQueryFactory(em);

        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

    }

    @Test
    public void startJpql() throws Exception {
        // member1을 찾아라
        Member findByJpql = em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        assertThat(findByJpql.getUsername()).isEqualTo("member1");
    }

    @Test
    public void startQuerydsl () throws Exception {
//        QMember m = new QMember("m");

        Member findByQuerydsl = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        assertThat(findByQuerydsl.getUsername()).isEqualTo("member1");
    }

    @Test
    public void search() throws Exception {
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1")
                        .and(member.age.eq(10)))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
        assertThat(findMember.getAge()).isEqualTo(10);

    }

    @Test
    public void searchAndParam() throws Exception {
        Member findMember = queryFactory
                .selectFrom(member)
                .where(
                        member.username.eq("member1"),
                        (member.age.eq(10))
                )
                .where(member.id.isNotNull().or(member.id.eq(1L)))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
        assertThat(findMember.getAge()).isEqualTo(10);

    }

    @Test
    public void resultFetch() {
//        List<Member> fetch = queryFactory
//                .selectFrom(member)
//                .fetch();
//
//        queryFactory
//                .selectFrom(member)
//                .fetchOne();
//
//        queryFactory
//                .selectFrom(member)
////                .limit(1).fetchOne();
//                .fetchFirst();

        QueryResults<Member> results = queryFactory
                .selectFrom(member)
                .fetchResults();

        long total = results.getTotal();
        long limit = results.getLimit();
        long offset = results.getOffset();
        List<Member> contents = results.getResults();

        System.out.println("total = " + total);
        System.out.println("limit = " + limit);
        System.out.println("offset = " + offset);
        System.out.println("contents = " + contents);

        System.out.println("==============================");
        System.out.println("==============================");
        System.out.println("==============================");
        System.out.println("==============================");

        long fetchCount = queryFactory
                .selectFrom(member)
                .fetchCount();

        System.out.println("fetchCount = " + fetchCount);
    }

    /**
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순(desc)
     * 2. 회원 이름 올림차순(asc)
     * 단 2에서 회원 이름이 없으면 마지막에 출력(nulls last)
     * */
    @Test
    public void sort() {

        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);

        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();

    }

    @Test
    public void paging1() {
//        List<Member> result = queryFactory
//                .selectFrom(member)
//                .orderBy(member.username.desc())
//                .offset(0)
//                .limit(2)
//                .fetch();
//
//        assertThat(result.size()).isEqualTo(2);

        QueryResults<Member> queryResults = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(0)
                .limit(2)
                .fetchResults();

        long total = queryResults.getTotal();
        long limit = queryResults.getLimit();
        long offset = queryResults.getOffset();
        List<Member> results = queryResults.getResults();

        System.out.println("total = " + total);
        System.out.println("limit = " + limit);
        System.out.println("offset = " + offset);
        System.out.println("results = " + results);

        assertThat(total).isEqualTo(4);
        assertThat(limit).isEqualTo(2);
        assertThat(results.size()).isEqualTo(2);

    }

    @Test
    public void aggregation() {
        List<Tuple> result = queryFactory
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetch();

        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }

    /**
     * 팀의 이름과 각 팀의 평균 연령을 구하라.
     * */
    @Test
    public void group () throws Exception {
        List<Tuple> result = queryFactory
                .select(
                        team.name,
                        member.age.avg()
                )
                .from(member)
                .join(member.team, team)
                .groupBy(team.id)
                .orderBy(team.name.asc())
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        String teamAName = teamA.get(team.name);
        Double teamAAgeAvg = teamA.get(member.age.avg());
        System.out.println("teamAName = " + teamAName);
        System.out.println("teamAAgeAvg = " + teamAAgeAvg);

        assertThat(teamAName).isEqualTo("teamA");
        assertThat(teamAAgeAvg).isEqualTo(15);

        String teamBName = teamB.get(team.name);
        Double teamBAgeAvg = teamB.get(member.age.avg());
        System.out.println("teamBName = " + teamBName);
        System.out.println("teamBAgeAvg = " + teamBAgeAvg);

        assertThat(teamBName).isEqualTo("teamB");
        assertThat(teamBAgeAvg).isEqualTo(35);

    }

    /**
     * 팀 A에 소속된 모든 회원
     * */
    @Test
    public void join() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        result.stream()
                .forEach(member -> {
                    System.out.println("member = " + member);
                });
        assertThat(result.size()).isEqualTo(2);

        assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2");

    }

    /**
     * 세타 조인
     * 회원의 이름이 팀 이름과 같은 회원 조회
     * */
    @Test
    public void theta_join(){
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        List<Member> result = queryFactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        result.stream()
                .forEach(member -> System.out.println("member = " + member));

        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");

    }

    /**
     * 예) 회원과 팀을 조회하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
     * JPQL: select m, t from Member m left join m.team t on t.name = 'teamA'
     */
    @Test
    public void join_on_filtering () {
        List<Tuple> result = queryFactory
                .select(
                        member,
                        team
                )
                .from(member)
                .leftJoin(member.team, team)
//                .innerJoin(member.team, team)
                .on(team.name.eq("teamA"))
//                .where(team.name.eq("teamA"))
                .fetch();

        /*
        * on 절을 확용한 조인 대상 필터링을 사용할 때,
        * 내부조인 이면 익숙한 where 절로 해결하고,
        * 정말 외부조인이 필요한 경우에만 이 기능을 사용하자!
        * */

        result.stream()
                .forEach(tuple -> {
                    System.out.println("tuple = " + tuple);
                });
    }

    /**
     * 연관관계 없는 엔티티 외부 조인
     * 회원의 이름이 팀 이름과 같은 대상 외부 조인
     * */
    @Test
    public void join_on_no_relation(){
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        List<Tuple> result = queryFactory
                .select(
                        member,
                        team
                )
//                .from(member, team)
                .from(member)
                .leftJoin(team)
                .on(member.username.eq(team.name))
                .fetch();

        result.stream()
                .forEach(tuple -> System.out.println("tuple = " + tuple));

//        assertThat(result)
//                .extracting("username")
//                .containsExactly("teamA", "teamB");

    }

}
