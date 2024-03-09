import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {

    public static void main(String[] args) {
        String port;
        try (BufferedReader reader = Files.newBufferedReader(Path.of("settings.txt"))) {
            while (reader.ready()) {
                port = reader.readLine().split(":")[1];
            }
        } catch (IOException e) {
            e.getMessage();
        }

    }
}
