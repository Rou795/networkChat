import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Scanner;

public class Client {
    private static final String CONFIG_FILE = "static/settings.txt";
    private static final String LOG_FILE = "static/file_client.log";
    private static final String FIRST_CONNECT_KEY = "FiRsTCoNnEcT*******";
    private static final String DELIMITER = "_*_*_*_*_*_*_*_*_*_*_*_*_*_*_*_*_*_*_*_*_*_*_";

    public static void main(String[] args) {
        int port = 0;
        Path settingsPath = Path.of(CONFIG_FILE);
        Scanner scanner = new Scanner(System.in);
        if (Files.exists(settingsPath)) {
            try (BufferedReader reader = Files.newBufferedReader(settingsPath)) {
                while (reader.ready()) {
                    port = Integer.parseInt(reader.readLine().split(":")[1]);
                }
            } catch (IOException e) {
                e.getMessage();
            }
        } else {
            System.out.println("Config_file не найден, введите, пожалуйста, номер порта для подключения к серверу:");
            port = scanner.nextInt();
        }
        try (Socket clientSocket = new Socket("netology.homework", port);
             PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);
             BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {

            StringBuilder builder = new StringBuilder();

            System.out.println("Отправляем запрос серверу netology.homework");
            writer.println(FIRST_CONNECT_KEY);
            System.out.println("Подключение к серверу стабильно. Укажите свой ник для общения.");
            String nickName = scanner.nextLine();
            writer.println(nickName);
            String clientLog = extractLog();
            writer.println(clientLog);
            while (reader.ready()) {
                builder.append(reader.readLine());
            }
            logEnrichment(builder.toString());
            while (true) {
                String response = reader.readLine();
                if (response.equals(nickName + " welcome to chat!")) {
                    System.out.println(response);
                }
                if (reader.ready()) {
                    String sender = reader.readLine().split(":")[1].strip();
                    String body = reader.readLine();
                    saveMessage(sender, LocalDateTime.now(), body);
                    printMes(sender, body);
                }
                String body = scanner.nextLine();
                if (body.equals("/exit")) {
                    break;
                } else {
                    LocalDateTime now = LocalDateTime.now();
                    writer.println(mesMaker(nickName, body));
                    saveMessage(nickName, now, body);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void saveMessage(String from, LocalDateTime mesTime, String message) {
        Path logPath = Path.of(LOG_FILE);

        if (!Files.exists(logPath)) {
            try {
                Files.createFile(logPath);
            } catch (IOException e) {
                e.getMessage();
            }
        }
        try (BufferedWriter writer = Files.newBufferedWriter(logPath)) {
            writer.write("From: " + from +
                    "\nMessage time: " + mesTime +
                    "\nText: " + message);
            writer.flush();
        } catch (IOException e) {
            e.getMessage();
        }
    }

    public static String extractLog() {
        Path logPath = Path.of(LOG_FILE);
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = Files.newBufferedReader(logPath)) {
            while (reader.ready()) {
                builder.append(reader.readLine());
            }
        } catch (IOException e) {
            e.getMessage();
        }
        return builder.toString();
    }

    public static void logEnrichment(String serverLog) {
        Path logPath = Path.of(LOG_FILE);

        if (!Files.exists(logPath)) {
            try {
                Files.createFile(logPath);
            } catch (IOException e) {
                e.getMessage();
            }
        }
        try (BufferedWriter writer = Files.newBufferedWriter(logPath)) {
            writer.write(serverLog);
            writer.flush();
        } catch (IOException e) {
            e.getMessage();
        }
    }

    public static String mesMaker(String nickName, String body) {
        String message = nickName + "\n" + DELIMITER + "\n" + body;
        return message;
    }

    public static void printMes(String sender, String body) {
        System.out.println("From: " + sender + "\n" + body);
    }
}
