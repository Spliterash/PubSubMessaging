import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class MyCoolException extends Exception {
    String field1;
    int a;
}
