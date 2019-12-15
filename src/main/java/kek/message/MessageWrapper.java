package kek.message;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kek.person.Person;
import kek.person.PersonType;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Builder
@Data
@Getter
@Setter
@AllArgsConstructor
public class MessageWrapper {

    // Передаваемый объект
    private String str;
    // Его тип
    private MessageType messageType;

    @Getter
    @Setter
    public List<FROM_TO> history_List = new ArrayList<>();

    public void init(){
        if(history_List == null)
            history_List = new ArrayList<>();
    }

    public MessageWrapper() {
        init();
    }

    @JsonIgnore
    public Integer getFromPort_last(){
        return history_List.get(history_List.size() - 1).fromPort;
    }
    @JsonIgnore
    public Person getFromPerson_last(){
        return history_List.get(history_List.size() - 1).fromPerson;
    }
    @JsonIgnore
    public PersonType getFromPersonType_last(){
        return history_List.get(history_List.size() - 1).fromPersonType;
    }

    @JsonIgnore
    public Integer getToPort_last(){
        return history_List.get(history_List.size() - 1).toPort;
    }
    @JsonIgnore
    public Person getToPerson_last(){
        return history_List.get(history_List.size() - 1).toPerson;
    }
    @JsonIgnore
    public PersonType getToPersonType_last(){
        return history_List.get(history_List.size() - 1).toPersonType;
    }

    ////////////////////////////////////////////
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

    ////////////////////////////////////////////
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

    ////////////////////////////////////////////
    // Сообщение об ожидании для определенных персон
    @JsonIgnore
    public MessageFreeForPersonType getMessageFreeForPersonType() throws JsonProcessingException {
        return MessageFreeForPersonType.deserialize(str);
    }
    @JsonIgnore
    public void setMessageFreeForPersonType(MessageFreeForPersonType messageFreeForPersonType) throws JsonProcessingException {
        this.str = messageFreeForPersonType.serialize();
        this.messageType = MessageType.MESSAGE_FREE_FOR_PERSONTYPE;
    }


    ////////////////////////////////////////////
    // Сообщение о снабжении ресурсами для философов
    @JsonIgnore
    public MessResource getMessResource() throws JsonProcessingException {
        return MessResource.deserialize(str);
    }
    @JsonIgnore
    public void setMessResource(MessResource message) throws JsonProcessingException {
        this.str = message.serialize();
        this.messageType = MessageType.MESSAGE_RESOURCE;
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
