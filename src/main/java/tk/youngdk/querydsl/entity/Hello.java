package tk.youngdk.querydsl.entity;

import lombok.Getter;
import lombok.ToString;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
@Getter
@ToString
public class Hello {

    @Id
    @GeneratedValue
    private Long id;

    private String name;

}
