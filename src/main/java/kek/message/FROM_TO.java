package kek.message;

import kek.person.Person;
import kek.person.PersonType;
import lombok.*;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FROM_TO {
    // От кого сообщение
    Integer fromPort;
    Person fromPerson;
    PersonType fromPersonType;

    // Кому адресовано
    Integer toPort;
    Person toPerson;
    PersonType toPersonType;
}
