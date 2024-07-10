import lombok.Data;

@Data
public class NestedDto {
    private String strValue;
    private CoolInterface coolInterface;
    private Nested nested;

    @Data
    public static class Nested {
        private String strValue;
    }

    public interface CoolInterface {

    }

    @Data
    public static class Obj1 implements CoolInterface {
        private String field1;
    }

    @Data
    public static class Obj2 implements CoolInterface {
        private String field2;
    }
}
