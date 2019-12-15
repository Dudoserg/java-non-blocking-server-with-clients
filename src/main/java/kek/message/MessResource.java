package kek.message;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MessResource {
    List<Integer> resource;
    ResourceType resourceType;

    // Jackson
    @JsonIgnore
    public static MessResource deserialize(String serializedObject) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        MessResource messResource = objectMapper.readValue(serializedObject, MessResource.class);
        return messResource;
    }

    //Jackson
    @JsonIgnore
    public static String serialize(MessResource message) throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(message);
    }

    //Jackson
    @JsonIgnore
    public String serialize() throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(this);
    }

}
