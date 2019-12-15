package kek.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kek.person.Person;
import lombok.*;

@Builder(builderMethodName = "hiddenBuilder")
@Data
@Getter
@Setter
@NoArgsConstructor
public class Message {

    private String message;


    public Message(String message) {
        this.message = message;
    }


    public static MessageBuilder builder(String message){
        return hiddenBuilder()
                .message(message);
    }

    // Jackson
    public static Message deserialize(String serializedObject) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        Message messageFromClient = objectMapper.readValue(serializedObject, Message.class);
        return messageFromClient;
    }

    //Jackson
    public static String serialize(Message message) throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(message);
    }

    //Jackson
    public String serialize() throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(this);
    }


}

