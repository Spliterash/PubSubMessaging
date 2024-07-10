import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.spliterash.pubSubMessaging.pubsub.binary.mapper.json.JacksonMapper;

import java.util.List;

public class JacksonMapperTests {
    private static final JacksonMapper mapper = new JacksonMapper(List.of());

    @Test
    public void simpleSerialization() throws Exception {
        var dto = new SimpleDto();

        dto.setStrValue("CoolValue");
        dto.setIntValue(1488);

        byte[] stringValue = mapper.write(dto);
        Object readDto = mapper.read(stringValue);

        Assertions.assertEquals(dto, readDto);
    }

    @Test
    public void nestedSerialization() throws Exception {
        var dto = new NestedDto();
        var nestedDto = new NestedDto.Nested();
        var coolInterface = new NestedDto.Obj2();

        coolInterface.setField2("cool field2 value");
        nestedDto.setStrValue("cool str");

        dto.setStrValue("CoolValue");
        dto.setNested(nestedDto);
        dto.setCoolInterface(coolInterface);

        byte[] stringValue = mapper.write(dto);
        Object readDto = mapper.read(stringValue);

        Assertions.assertEquals(dto, readDto);
    }

}
