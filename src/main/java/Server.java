import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.Scanner;

public class Server {

    private static final String CONFIG_FILE = "static/settings.txt";
    private static final String LOG_FILE = "static/file_server.log";
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
            System.out.println("Config_file не найден, введите, пожалуйста, номер порта для запуска сервера:");
            port = scanner.nextInt();
        }

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Сервер стартовал");
            while (true) {
                try (Socket clientSocket = serverSocket.accept(); // ждем подключения
                     PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                     BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
                    System.out.println("New connection accepted");
                    String userNick = "";
                    String message = "";
                    System.out.println("wait start");
                    String request = in.readLine().strip();

                    if (request.equals(FIRST_CONNECT_KEY)) {
                        // новое подключение 1) userNick 2) лог с клиента 3) отправка лога клиенту с сервера
                        System.out.println("first_connect");
                        userNick = in.readLine();
                        StringBuilder builder = new StringBuilder();
                        while (in.ready()) {
                            builder.append(in.readLine());
                        }
                        out.println("server_log_begin");
                        out.println(extractLog(builder.toString()));
                        out.println("server_log_end");
                        System.out.println(userNick + " has joined to chat.");
                        out.println(userNick + " welcome to chat!");
                        System.out.println(userNick + " welcome to chat!");
                    } else {
                        // получение сообщения с клиента
                        System.out.println(request);
                        userNick = request.split(DELIMITER, 1)[0].strip();
                        message = request.split(DELIMITER, 1)[1].strip();
                        LocalDateTime mesTime = LocalDateTime.now();
                        saveMessage(userNick, mesTime, message);
//                        out.println("message");
                        out.println("Message time: " + mesTime + "\nFrom: " + userNick + "\n" + message);
                    }
                    System.out.println("wait end");
                } catch (IOException e) {
                    throw new RuntimeException(e);
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
        try (BufferedWriter writer = Files.newBufferedWriter(logPath, StandardOpenOption.APPEND)) {
            writer.write("Message time: " + mesTime +
                    "\nFrom: " + from +
                    "\nText: " + message + "\n");
            writer.flush();
        } catch (IOException e) {
            e.getMessage();
        }
    }

    public static String extractLog(String clientLog) {
        Path logPath = Path.of(LOG_FILE);
        StringBuilder builder = new StringBuilder();
        String serverLog = "";
        try (BufferedReader reader = Files.newBufferedReader(logPath)) {
            while (reader.ready()) {
                builder.append(reader.readLine());
                builder.append("\n");
            }
            serverLog = builder.toString().replaceAll(clientLog, "");
            System.out.println(serverLog);
        } catch (IOException e) {
            e.getMessage();
        }
        return serverLog;
    }
}
