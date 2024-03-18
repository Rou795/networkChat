import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.Scanner;

public class Client {
    private static final String CONFIG_FILE = "static/settings.txt";
    private static final String LOG_FILE = "static/file_client.log";
    private static final String DELIMITER = "###";

    public static void main(String[] args) {
        int port = 0;
        Path settingsPath = Path.of(CONFIG_FILE);
        Scanner scanner = new Scanner(System.in);

// поиск конфигурационного файла. Если его нет, то просьба ввести номер порта вручную

        if (Files.exists(settingsPath)) {
            try (BufferedReader reader = Files.newBufferedReader(settingsPath)) {
                while (reader.ready()) {
                    port = Integer.parseInt(reader.readLine().split(":")[1]);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            System.out.println("Config_file не найден, введите, пожалуйста, номер порта для подключения к серверу:");
            port = scanner.nextInt();
        }

// подключение к серверу        

        try (Socket clientSocket = new Socket("localhost", port);
             PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);
             BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {

            StringBuilder builder = new StringBuilder();
            System.out.println("Отправляем запрос серверу netology.homework");
            System.out.println("Подключение к серверу стабильно. Укажите свой ник для общения.");
            String nickName = scanner.nextLine();
            writer.println(nickName);

// получение file_log от сервера и перезапись локального с помощью функции logEnrichment()

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
                logEnrichment(serverLog);
            }
            response = reader.readLine();
            System.out.println(response);

// задача по получению писем от сервера, печатанию их в консоль и сохранение в file_log с помощью saveMessage()
// для потока fromServerThread

            Runnable checkReader = () -> {
                while (true) {
                    try {
                        if (reader.ready()) {
                            String rawMesText = reader.readLine();
                            String mesText = rawMesText.replaceAll("\\*", "\n");
                            System.out.println(mesText.strip());
                            saveMessage(mesText);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            };

            Thread fromServerThread = new Thread(checkReader);

// задача по получению сообщений и команд от клиента, отправки их на сервер и сохранение в file_log с помощью saveMessage()
// для потока fromUserThread. Также обработка команды /exit.

            Runnable checkInput = () -> {
                while (true) {
                    if (scanner.hasNextLine()) {
                        String clientRequest = scanner.nextLine();
                        if (clientRequest.equals("/exit")) {
                            System.out.println("Направлен сигнал на окончание работы");
                            fromServerThread.interrupt();
                            while (true) {
                                if (fromServerThread.isInterrupted()) {
                                    System.out.println("Работа окончена. До свидания.");
                                    break;
                                }
                            }
                        } else {
                            String message = mesMaker(nickName, clientRequest);
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

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

// функция создания сообщения

    public static void saveMessage(String mesText) {
        Path logPath = Path.of(LOG_FILE);

        if (!Files.exists(logPath)) {
            try {
                Files.createFile(logPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        try (BufferedWriter writer = Files.newBufferedWriter(logPath, StandardOpenOption.APPEND)) {
            writer.write(mesText + "\n");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

//функция сохранения сообщения в file_log

    public static void saveMessage(String mesText, LocalDateTime mesTime) {
        Path logPath = Path.of(LOG_FILE);

        if (!Files.exists(logPath)) {
            try {
                Files.createFile(logPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        try (BufferedWriter writer = Files.newBufferedWriter(logPath, StandardOpenOption.APPEND)) {
            writer.write("Message time: " + mesTime + "\n" + mesText + "\n");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

// функция обновления лога по данным от сервера

    public static void logEnrichment(String serverLog) {
        Path logPath = Path.of(LOG_FILE);

        if (!Files.exists(logPath)) {
            try {
                Files.createFile(logPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        try (BufferedWriter writer = Files.newBufferedWriter(logPath)) {
            writer.write(serverLog);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

// функция создания соообщения

    public static String mesMaker(String nickName, String body) {
        String message = nickName + "\n" + DELIMITER + "\n" + body;
        message = message.replaceAll("\n", "*");
        return message;
    }
}
