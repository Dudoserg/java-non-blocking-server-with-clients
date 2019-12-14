package kek.message;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kek.person.Person;
import kek.person.PersonType;
import lombok.*;

@Builder
@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MessageWrapper {
    // Передаваемый объект
    private String str;
    // Его тип
    private MessageType messageType;

    // От кого сообщение
    private Integer fromPort;
    private Person fromPerson;

    // Кому адресовано
    private Integer toPort;
    private Person toPerson;
    private PersonType toPersonType;

    //    @JsonIgnoreProperties
    @JsonIgnore
    public MessConfirm getMessConfirm() throws JsonProcessingException {
        return MessConfirm.deserialize(str);
    }

    @JsonIgnore
    public void setMessConfirm(MessConfirm messConfirm) throws JsonProcessingException {
        this.str = messConfirm.serialize();
        this.messageType = MessageType.MESSAGE_CONFIRM;
    }

    @JsonIgnore
    public void setMessage(MessConfirm message) throws JsonProcessingException {
        this.setMessConfirm(message);
    }

    // Jackson
    public static MessageWrapper deserialize(String serializedObject) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        MessageWrapper messageFromClient = objectMapper.readValue(serializedObject, MessageWrapper.class);
        return messageFromClient;
    }

    //Jackson
    public static String serialize(MessageWrapper message) throws JsonProcessingException {

        return new ObjectMapper().writeValueAsString(message);
    }

    //Jackson
    public String serialize() throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(this);
    }
}
