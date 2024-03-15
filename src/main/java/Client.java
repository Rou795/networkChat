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

    public static void main(String[] args) throws IOException {
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
        try (Socket clientSocket = new Socket("localhost", port);
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
            String response = reader.readLine();
            if (response.equals("server_log_begin")) {
                response = "";
                while (!response.equals("server_log_end")) {
                    try {
                        builder.append(response);
                        response = reader.readLine();
                        builder.append("\n");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                String serverLog = builder.toString().strip();
                System.out.println(serverLog);
                logEnrichment(serverLog);
            }
            response = reader.readLine();
            System.out.println(response);
            Runnable checkReader = () -> {
                while (true) {
//                    System.out.println("1");
                    try {

                        if (reader.ready()) {
//                            System.out.println("1");
                            String mesText = reader.readLine();
                            LocalDateTime mesTime = LocalDateTime.now();
                            System.out.println(mesTime);
                            System.out.println(mesText);
                            saveMessage(mesText, mesTime);
                        }
                        Thread.sleep(5000);
                    } catch (IOException | InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            };

            Thread fromServerThread = new Thread(checkReader);

            Runnable checkInput = () -> {
                while (true) {
//                    System.out.println("2");
                    if (scanner.hasNextLine()) {
                        String clientRequest = scanner.nextLine();
                        if (clientRequest.equals("/exit")) {
                            System.out.println("Направлен сигнал на окончание работы");
                            fromServerThread.interrupt();
                            while (!fromServerThread.isInterrupted()) {
                                try {
                                    Thread.sleep(100);
                                } catch (InterruptedException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                            System.out.println("Работа окончена. До свидания.");
                            break;
                        } else {
                            String message = mesMaker(nickName, clientRequest);
 //                           System.out.println(message);
                            writer.println(message);
                            saveMessage(clientRequest, LocalDateTime.now());
                        }
                    }
                }
            };
            Thread fromUserThread = new Thread(checkInput);
            fromServerThread.start();
            fromUserThread.start();
            fromUserThread.join();
            fromServerThread.join();

/*            while (true) {
                if (scanner.hasNextLine()) {
                    String clientRequest = scanner.nextLine();
                    if (clientRequest.equals("/exit")) {
                        System.out.println("Направлен сигнал на окончание работы");
                        inputThread.interrupt();
                        while (!inputThread.isInterrupted()) {
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }
                        System.out.println("Работа окончена. До свидания.");
                        break;
                    } else {
                        String message = mesMaker(nickName, clientRequest);
                        writer.println(message);
                    }
                }
            }
 */
        } catch (IOException e) {
            e.getMessage();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static void saveMessage(String mesText, LocalDateTime mesTime) {
        Path logPath = Path.of(LOG_FILE);

        if (!Files.exists(logPath)) {
            try {
                Files.createFile(logPath);
            } catch (IOException e) {
                e.getMessage();
            }
        }
        try (BufferedWriter writer = Files.newBufferedWriter(logPath)) {
            writer.write("Message time: " + mesTime + "\n" + mesText);
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
            System.out.println("enrichment" + serverLog);
            writer.write(serverLog);
            writer.flush();
        } catch (IOException e) {
            e.getMessage();
        }
    }

    public static String mesMaker(String nickName, String body) {
        String message = nickName + "\n" + DELIMITER + "\n" + body;
        message = message.replaceAll("\n", "*-*");
        return message;
    }

    public static void printMes(String sender, String body) {
        System.out.println("From: " + sender + "\n" + body);
    }
}
