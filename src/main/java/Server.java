import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Scanner;

public class Server {

    private static final String CONFIG_FILE = "static/settings.txt";
    private static final String LOG_FILE = "static/file_server.log";
    private static final String DELIMITER = "###";
    private static int PORT;
    private static volatile ArrayList<PrintWriter> outList = new ArrayList<>();

    public static void main(String[] args) throws InterruptedException {

// поиск конфигурационного файла. Если его нет, то просьба ввести номер порта вручную

        Path settingsPath = Path.of(CONFIG_FILE);
        Scanner scanner = new Scanner(System.in);
        if (Files.exists(settingsPath)) {
            try (BufferedReader reader = Files.newBufferedReader(settingsPath)) {
                while (reader.ready()) {
                    PORT = Integer.parseInt(reader.readLine().split(":")[1]);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            System.out.println("Config_file не найден, введите, пожалуйста, номер порта для запуска сервера:");
            PORT = scanner.nextInt();
        }
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Сервер стартовал");

// задача по ожитднаию подключения к серверу, отправку клиенту лога с сервера, сохранению подключения с ним
// и добавление его потока вывода к числу остальных (outlist). Для потоков connection в листе threads

            Runnable socketConnect = () -> {
                while (true) {
                    try (Socket clientSocket = serverSocket.accept(); // ждем подключения
                         PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                         BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
                        // новое подключение 1) userNick 2) лог с клиента 3) отправка лога клиенту с сервера
                        synchronized (outList) {
                            outList.add(out);
                        }
                        System.out.println("New connection accepted");
                        String userNick = in.readLine();
                        out.println("server_log_begin");
                        out.println(extractLog());
                        out.println("server_log_end");
                        for (PrintWriter channel : outList) {
                            channel.println(userNick + " has joined to chat.");
                        }

// задача по получению сообщения от клиентов, отправке их другим клиентам и сохранению его в file_log
// с помощью функции saveMessage(). Для потоков connection в листе threads

                        Runnable checkReader = () -> {
                            while (true) {
                                try {
                                    if (in.ready()) {
                                        String raw_mes = in.readLine();
                                        synchronized (raw_mes) {
                                            String mes = mesMaker(cleaningMes(raw_mes));
                                            for (PrintWriter channel : outList) {
                                                channel.checkError();
                                                channel.println(mes.replaceAll("\n", "*"));
                                            }
                                            saveMessage(mes);
                                        }
                                    }
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
            };

            ArrayList<Thread> threads = new ArrayList<>();
            Runnable checkerConnect = () -> {
                while (true) {
                    if (threads.size() == outList.size()) {
                        for (int i = 0; i < (threads.size() / 2) + 1; i++) {
                            Thread connection = new Thread(socketConnect);
                            threads.add(connection);
                            connection.start();
                        }
                        System.out.println("Update: " + threads.size() + " " + outList.size());
                    }
                    for (PrintWriter channel : outList) {
                        if (channel.checkError()) outList.remove(channel);
                    }
                }
            };
            Thread connectController = new Thread(checkerConnect);
            connectController.start();
            connectController.join();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

// функция созранения сообщения в лог

    public static void saveMessage(String message) {
        Path logPath = Path.of(LOG_FILE);
        if (!Files.exists(logPath)) {
            try {
                Files.createFile(logPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        try (BufferedWriter writer = Files.newBufferedWriter(logPath, StandardOpenOption.APPEND)) {
            writer.write(message);
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

// функция извлечения лога

    public static String extractLog() {
        Path logPath = Path.of(LOG_FILE);
        StringBuilder builderLog = new StringBuilder();
        try (BufferedReader reader = Files.newBufferedReader(logPath)) {
            synchronized (reader) {
                while (reader.ready()) {
                    builderLog.append(reader.readLine());
                    builderLog.append("\n");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return builderLog.toString();
    }

// функция для "очистки грязного" сообщения, полученного от клиента

    public static String cleaningMes(String rawMes) {
        return rawMes.replaceAll("\\*", "\n");
    }

// ФЫункция создания сообщений

    public static String mesMaker(String protoMes) {
        String userNick = protoMes.split(DELIMITER, 2)[0].strip();
        String body = protoMes.split(DELIMITER, 2)[1].strip();
        LocalDateTime mesTime = LocalDateTime.now();

        String finalMes = "Message time: " + mesTime +
                "\nFrom: " + userNick +
                "\nText: " + body + "\n";
        return finalMes;
    }
}
