import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Scanner;

public class Server {

    private static final String CONFIG_FILE = "static/settings.txt";
    private static final String LOG_FILE = "static/file_server.log";
    private static final String FIRST_CONNECT_KEY = "FiRsTCoNnEcT*******";
    private static final String DELIMITER = "###";

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
                System.out.println("подключение");
                try (Socket clientSocket = serverSocket.accept(); // ждем подключения
                     PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                     BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
                    // новое подключение 1) userNick 2) лог с клиента 3) отправка лога клиенту с сервера
                    Runnable newUserCheck = () -> {

                    };
                    System.out.println("New connection accepted");
                    String userNick = in.readLine();
                    String message = "";
                    System.out.println("wait start");

                    System.out.println("first_connect");
                    out.println("server_log_begin");
                    out.println(extractLog());
                    out.println("server_log_end");
                    System.out.println(userNick + " has joined to chat.");
                    out.println(userNick + " welcome to chat!");
//System.out.println(userNick + " welcome to chat!");

                    // получение сообщения с клиента
                    Runnable checkReader = () -> {
                        while (true) {
                            try {
                                if (in.ready()) {
                                    String raw_mes = in.readLine();
                                    synchronized (raw_mes) {
                                        System.out.println(raw_mes);
                                        String mes = mesMaker(cleaningMes(raw_mes));
                                        out.println(mes.replaceAll("\n", "*"));
                                        saveMessage(mes);
                                    }
                                }
//                                Thread.sleep(5000);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    };

                    Thread fromServerThread = new Thread(checkReader);
                    fromServerThread.start();
                    fromServerThread.join();
                } catch (IOException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void saveMessage(String message) {
        Path logPath = Path.of(LOG_FILE);

        if (!Files.exists(logPath)) {
            try {
                Files.createFile(logPath);
            } catch (IOException e) {
                e.getMessage();
            }
        }
        try (BufferedWriter writer = Files.newBufferedWriter(logPath, StandardOpenOption.APPEND)) {
            writer.write(message);
            writer.flush();
        } catch (IOException e) {
            e.getMessage();
        }
    }

    public static String extractLog() {
        Path logPath = Path.of(LOG_FILE);
        StringBuilder builder = new StringBuilder();
        String serverLog = "";
        try (BufferedReader reader = Files.newBufferedReader(logPath)) {
            synchronized (reader) {
                while (reader.ready()) {
                    builder.append(reader.readLine());
                    builder.append("\n");
                }
            }
            System.out.println(serverLog);
        } catch (IOException e) {
            e.getMessage();
        }
        return serverLog;
    }

    public static String cleaningMes(String rawMes) {
        String mes = rawMes.replaceAll("\\*", "\n");
        return mes;
    }

    public static String mesMaker(String protoMes) {
        String userNick = protoMes.split(DELIMITER, 2)[0].strip();
//        System.out.println(userNick);
//        System.out.println(protoMes);
//        System.out.println(Arrays.toString(protoMes.split(DELIMITER, 2)));
        String body = protoMes.split(DELIMITER, 2)[1].strip();


        LocalDateTime mesTime = LocalDateTime.now();

        String finalMes = "Message time: " + mesTime +
                "\nFrom: " + userNick +
                "\nText: " + body + "\n";
        return finalMes;
    }
}
