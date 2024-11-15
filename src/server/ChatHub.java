package server;

import java.io.*;
import java.net.*;
import java.util.*;

public class ChatHub {
    private static final int SERVER_PORT = 54321;  // Порт для подключения клиентов
    private static final Set<UserConnection> activeConnections = Collections.synchronizedSet(new HashSet<>());  // Коллекция для отслеживания активных пользователей
    private static final int SHIFT = 3;  // Сдвиг для шифра Цезаря (можно изменить)

    public static void main(String[] args) {
        System.out.println("Чат-сервер запущен...");
        try (ServerSocket serverSocket = new ServerSocket(SERVER_PORT)) {
            // Запускаем поток для обработки сообщений, отправляемых сервером
            new Thread(new AdminMessageHandler()).start();

            while (true) {
                // Ожидаем подключения нового клиента
                Socket clientSocket = serverSocket.accept();
                UserConnection userConnection = new UserConnection(clientSocket);
                activeConnections.add(userConnection);  // Добавляем новое соединение в список активных
                new Thread(userConnection).start();  // Запускаем поток для обработки этого клиента
            }
        } catch (IOException ex) {
            System.err.println("Серверная ошибка: " + ex.getMessage());  // Логируем ошибку, если сервер не может запуститься
        }
    }

    // Отправка сообщения всем подключённым пользователям, кроме указанного
    public static void sendGlobalMessage(String message, UserConnection excludeUser) {
        synchronized (activeConnections) {
            for (UserConnection user : activeConnections) {
                if (user != excludeUser) {
                    user.sendMessage(message);  // Отправляем кодированное сообщение
                }
            }
        }
    }

    // Удаление соединения пользователя из активных
    public static void removeUserConnection(UserConnection userConnection) {
        activeConnections.remove(userConnection);  // Удаляем пользователя из списка активных
    }

    // Метод для кодирования сообщений с использованием шифра Цезаря
    public static String encryptMessage(String message) {
        StringBuilder encryptedMessage = new StringBuilder();
        for (char ch : message.toCharArray()) {
            encryptedMessage.append((char) (ch + SHIFT));  // Сдвигаем символ на заданное количество позиций
        }
        return encryptedMessage.toString();
    }

    // Метод для декодирования сообщений с использованием шифра Цезаря
    public static String decryptMessage(String message) {
        StringBuilder decryptedMessage = new StringBuilder();
        for (char ch : message.toCharArray()) {
            decryptedMessage.append((char) (ch - SHIFT));  // Сдвигаем символ обратно на заданное количество позиций
        }
        return decryptedMessage.toString();
    }

    private static class UserConnection implements Runnable {
        private final Socket socket;  // Сокет для клиента
        private PrintWriter outStream;  // Поток для отправки сообщений клиенту
        private BufferedReader inStream;  // Поток для получения сообщений от клиента
        private String username;  // Имя пользователя

        public UserConnection(Socket socket) {
            this.socket = socket;  // Инициализируем сокет
        }

        @Override
        public void run() {
            try {
                inStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));  // Поток для чтения от клиента
                outStream = new PrintWriter(socket.getOutputStream(), true);  // Поток для записи клиенту

                // Запрашиваем у клиента имя
                sendGlobalMessage(encryptMessage("Введите ваше имя"), null);
                username = decryptMessage(inStream.readLine());
                System.out.println(username + " присоединился к чату.");  // Логируем подключение
                sendGlobalMessage(username + " присоединился к чату.", this);  // Сообщаем всем о подключении

                String message;
                while ((message = inStream.readLine()) != null) {
                    String decryptedMessage = decryptMessage(message);  // Дешифруем сообщение
                    System.out.println(username + ": " + decryptedMessage);  // Логируем сообщение от клиента
                    sendGlobalMessage(encryptMessage(username + ": " + decryptedMessage), this);  // Шифруем и рассылаем сообщение всем клиентам
                }
            } catch (IOException ex) {
                System.err.println("Ошибка при работе с клиентом: " + ex.getMessage());  // Логируем ошибки ввода/вывода
            } finally {
                closeConnections();  // Закрываем соединения, если клиент отключился
            }
        }

        // Метод для отправки сообщения конкретному пользователю
        public void sendMessage(String message) {
            outStream.println(message);  // Отправляем шифрованное сообщение
        }

        // Закрытие всех потоков и сокета, когда клиент покидает чат
        private void closeConnections() {
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();  // Закрытие соединения
                }
                if (inStream != null) {
                    inStream.close();  // Закрытие потока ввода
                }
                if (outStream != null) {
                    outStream.close();  // Закрытие потока вывода
                }
                System.out.println(username + " покинул чат.");  // Логируем выход
                sendGlobalMessage(username + " покинул чат.", this);  // Сообщаем всем о выходе
                removeUserConnection(this);  // Удаляем пользователя из активных соединений
            } catch (IOException ex) {
                System.err.println("Ошибка при закрытии соединений: " + ex.getMessage());  // Логируем ошибку при закрытии соединений
            }
        }
    }

    // Класс для обработки сообщений от администратора сервера
    private static class AdminMessageHandler implements Runnable {

        private final BufferedReader adminInput;  // Поток для ввода команд администратора

        public AdminMessageHandler() {
            adminInput = new BufferedReader(new InputStreamReader(System.in));  // Инициализация потока для ввода с консоли
        }

        @Override
        public void run() {
            String adminMessage;
            try {
                // Чтение сообщений администратора и рассылка их всем пользователям
                while ((adminMessage = adminInput.readLine()) != null) {
                    sendGlobalMessage(encryptMessage("Администратор: " + adminMessage), null);  // Отправляем зашифрованное сообщение всем пользователям
                }
            } catch (IOException ex) {
                System.err.println("Ошибка при вводе сообщения администратора: " + ex.getMessage());  // Логируем ошибку
            }
        }
    }
}
