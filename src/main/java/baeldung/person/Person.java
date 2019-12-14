package baeldung.person;

import lombok.*;

@Getter
@Setter
@Data
@AllArgsConstructor
@NoArgsConstructor

public class Person  {

    protected PersonType personType;
    protected String personName;

    public Person(PersonType personType) {
        this.personType = personType;
    }
}
