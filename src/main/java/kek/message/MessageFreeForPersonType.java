package kek.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kek.person.PersonType;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MessageFreeForPersonType {
    private List<PersonType> list = new ArrayList<>();



    // Jackson
    public static MessageFreeForPersonType deserialize(String serializedObject) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        MessageFreeForPersonType messageFromClient = objectMapper.readValue(serializedObject, MessageFreeForPersonType.class);
        return messageFromClient;
    }

    //Jackson
    public static String serialize(MessageFreeForPersonType message) throws JsonProcessingException {

        return new ObjectMapper().writeValueAsString(message);
    }

    //Jackson
    public String serialize() throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(this);
    }
}
