package clinet;

import java.io.*;
import java.net.*;

public class ChatApplication {
    private static final String SERVER_IP = "localhost"; // Замените на IP-адрес сервера
    private static final int SERVER_PORT = 54321;
    private Socket chatSocket;
    private PrintWriter writer;
    private BufferedReader reader;
    private BufferedReader userInput;
    private static final int SHIFT = 3;  // Сдвиг для шифра Цезаря (можно изменить)

    public ChatApplication() {
        try {
            // Подключаемся к чату на сервере
            chatSocket = new Socket(SERVER_IP, SERVER_PORT);
            System.out.println("Успешно подключено к чату. Пожалуйста, введите ваше имя:");

            // Инициализация потоков для чтения и записи данных
            reader = new BufferedReader(new InputStreamReader(chatSocket.getInputStream()));
            writer = new PrintWriter(chatSocket.getOutputStream(), true);
            userInput = new BufferedReader(new InputStreamReader(System.in));

            // Запускаем потоки для получения и отправки сообщений
            new Thread(new MessageReceiver()).start();
            new Thread(new MessageSender()).start();

        } catch (IOException e) {
            System.err.println("Ошибка подключения к серверу: " + e.getMessage());
        }
    }

    // Метод для кодирования сообщений с использованием шифра Цезаря
    private String encryptMessage(String message) {
        StringBuilder encryptedMessage = new StringBuilder();
        for (char ch : message.toCharArray()) {
            encryptedMessage.append((char) (ch + SHIFT));  // Сдвигаем символ на заданное количество позиций
        }
        return encryptedMessage.toString();
    }

    // Метод для декодирования сообщений с использованием шифра Цезаря
    private String decryptMessage(String message) {
        StringBuilder decryptedMessage = new StringBuilder();
        for (char ch : message.toCharArray()) {
            decryptedMessage.append((char) (ch - SHIFT));  // Сдвигаем символ обратно на заданное количество позиций
        }
        return decryptedMessage.toString();
    }

    private class MessageReceiver implements Runnable {
        @Override
        public void run() {
            String incomingMessage;
            try {
                while ((incomingMessage = reader.readLine()) != null) {
                    String decryptedMessage = decryptMessage(incomingMessage);  // Дешифруем сообщение
                    System.out.println(decryptedMessage);  // Отображаем сообщение
                }
            } catch (IOException e) {
                System.err.println("Ошибка при получении сообщения: " + e.getMessage());
            } finally {
                closeResources();
            }
        }
    }

    private class MessageSender implements Runnable {
        @Override
        public void run() {
            String outgoingMessage;
            try {
                while ((outgoingMessage = userInput.readLine()) != null) {
                    // Добавьте возможность отправлять команды
                    if (outgoingMessage.startsWith("/")) {
                        handleCommand(outgoingMessage);
                    } else {
                        String encryptedMessage = encryptMessage(outgoingMessage);  // Шифруем сообщение перед отправкой
                        writer.println(encryptedMessage);  // Отправляем шифрованное сообщение
                    }
                }
            } catch (IOException e) {
                System.err.println("Ошибка при отправке сообщения: " + e.getMessage());
            } finally {
                closeResources();
            }
        }

        private void handleCommand(String command) {
            switch (command) {
                case "/help":
                    System.out.println("Доступные команды:");
                    System.out.println("/help - показать доступные команды");
                    System.out.println("/exit - выйти из чата");
                    break;
                case "/exit":
                    System.out.println("Вы вышли из чата.");
                    closeResources();
                    System.exit(0);
                    break;
                default:
                    System.out.println("Неизвестная команда. Пожалуйста, введите /help для списка команд.");
            }
        }
    }

    private void closeResources() {
        try {
            if (chatSocket != null && !chatSocket.isClosed()) {
                chatSocket.close();
            }
            if (reader != null) {
                reader.close();
            }
            if (writer != null) {
                writer.close();
            }
            if (userInput != null) {
                userInput.close();
            }
            System.out.println("Подключение закрыто.");
        } catch (IOException e) {
            System.err.println("Ошибка при закрытии соединений: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        new ChatApplication();
    }
}
