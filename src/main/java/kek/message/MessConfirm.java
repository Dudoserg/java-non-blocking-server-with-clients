package kek.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kek.person.Person;
import lombok.*;


@Builder(builderMethodName = "hiddenBuilder")
@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MessConfirm {
    private String message;
    private Integer port;
    private Person person;

    public MessConfirm(String message, Integer port) {
        this.message = message;
        this.port = port;
    }


    public static MessConfirmBuilder builder(String message, Integer port){
        return hiddenBuilder()
                .message(message)
                .port(port);
    }

    // Jackson
    public static MessConfirm deserialize(String serializedObject) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        MessConfirm messageFromClient = objectMapper.readValue(serializedObject, MessConfirm.class);
        return messageFromClient;
    }

    //Jackson
    public static String serialize(MessConfirm message) throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(message);
    }

    //Jackson
    public String serialize() throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(this);
    }


}
