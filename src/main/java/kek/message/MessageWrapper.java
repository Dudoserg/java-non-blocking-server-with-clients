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
    private PersonType fromPersonType;

    // Кому адресовано
    private Integer toPort;
    private Person toPerson;
    private PersonType toPersonType;

    //    @JsonIgnoreProperties
    // Сообщение с подтверждением регистрации
    @JsonIgnore
    public MessConfirm getMessConfirm() throws JsonProcessingException {
        return MessConfirm.deserialize(str);
    }

    @JsonIgnore
    public void setMessConfirm(MessConfirm messConfirm) throws JsonProcessingException {
        this.str = messConfirm.serialize();
        this.messageType = MessageType.MESSAGE_CONFIRM;
    }

    // Обычное сообщение
    @JsonIgnore
    public Message getMessage() throws JsonProcessingException {
        return Message.deserialize(str);
    }

    @JsonIgnore
    public void setMessage(Message message) throws JsonProcessingException {
        this.str = message.serialize();
        this.messageType = MessageType.MESSAGE;
    }

    // Обычное сообщение
    @JsonIgnore
    public MessageFreeForPersonType getMessageFreeForPersonType() throws JsonProcessingException {
        return MessageFreeForPersonType.deserialize(str);
    }

    @JsonIgnore
    public void setMessageFreeForPersonType(MessageFreeForPersonType messageFreeForPersonType) throws JsonProcessingException {
        this.str = messageFreeForPersonType.serialize();
        this.messageType = MessageType.MESSAGE_FREE_FOR_PERSONTYPE;
    }


//    @JsonIgnore
//    public void setMess(MessConfirm message) throws JsonProcessingException {
//        this.setMessConfirm(message);
//    }
//    @JsonIgnore
//    public void setMess(Message message) throws JsonProcessingException {
//        this.setMessage();
//    }


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
