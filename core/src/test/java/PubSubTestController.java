import java.util.UUID;

public class PubSubTestController implements PubSubTestResource {
    @Override
    public int multiply(int number) {
        return number * 2;
    }

    @Override
    public UUID noArgMethod() {
        return TestData.randomUUID;
    }
}
