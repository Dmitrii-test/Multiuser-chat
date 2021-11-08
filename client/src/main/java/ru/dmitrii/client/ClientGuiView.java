package ru.dmitrii.client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

import static java.awt.Frame.MAXIMIZED_BOTH;

public class ClientGuiView {
    private final ClientGuiController controller;

    private final JFrame frame = new JFrame("Чат");
    private final JTextField textField = new JTextField();
    private final JTextArea messages = new JTextArea(10, 80 );
    private final JTextArea users = new JTextArea(10, 10);

    public ClientGuiView(ClientGuiController controller) {
        this.controller = controller;
        initView();
    }

    private void initView() {
        textField.setEditable(false);
        messages.setEditable(false);
        users.setEditable(false);
        frame.setExtendedState(MAXIMIZED_BOTH);

        textField.setFont(new Font("Dialog", Font.PLAIN, 14));
        users.setFont(new Font("Dialog", Font.PLAIN, 16));
        JFrame.setDefaultLookAndFeelDecorated(true);

        // Слушатель на отправку сообщений
        ActionListener actionListener = e -> {
            if (!textField.getText().isEmpty()) {
                controller.sendTextMessage(textField.getText());
                textField.setText("");
            }
        };

        JButton button = new JButton("Отправить");
        // Подключение слушателей событий
        button.addActionListener(actionListener);
        textField.addActionListener(actionListener);
        // Главная разделяемая панель
        final JSplitPane splitHorizontal = new JSplitPane();
        // Размер разделяемой панели
        splitHorizontal.setDividerSize(4);
        // Положение разделяемой панели
        splitHorizontal.setDividerLocation(0.9);
        splitHorizontal.setResizeWeight(0.9);
        // Панель отправки сообщений
        JSplitPane splitMessages = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true);
        splitMessages.setLeftComponent(textField);
        splitMessages.setRightComponent(button);
        splitMessages.setDividerLocation(0.9);
        splitMessages.setResizeWeight(0.9);

        // Вертикальная разделяемая панель
        JSplitPane splitVertical = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true);
        splitVertical.setTopComponent (messages);
        splitVertical.setBottomComponent(splitMessages);
        splitVertical.setDividerSize(20);
        // Положение разделяемой панели
        splitVertical.setDividerLocation(0.9);
        splitVertical.setResizeWeight(0.9);
        // Настройка главной панели
        splitHorizontal.setRightComponent(new JScrollPane(new JScrollPane(users)));
        splitHorizontal.setLeftComponent(splitVertical);

        frame.getContentPane().add(splitHorizontal);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
        textField.grabFocus();
        textField.requestFocus();
    }

    public String getServerAddress() {
        return JOptionPane.showInputDialog(
                frame,
                "Введите адрес сервера:",
                "Конфигурация клиента",
                JOptionPane.QUESTION_MESSAGE);
    }

    public int getServerPort() {
        while (true) {
            String port = JOptionPane.showInputDialog(
                    frame,
                    "Введите порт сервера:",
                    "Конфигурация клиента",
                    JOptionPane.QUESTION_MESSAGE);
            try {
                return Integer.parseInt(port.trim());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(
                        frame,
                        "Был введен некорректный порт сервера. Попробуйте еще раз.",
                        "Конфигурация клиента",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public String getUserName() {
        return JOptionPane.showInputDialog(
                frame,
                "Введите ваше имя:",
                "Конфигурация клиента",
                JOptionPane.QUESTION_MESSAGE);
    }

    public String getPassword() {
        return JOptionPane.showInputDialog(
                frame,
                "Введите ваш пароль:",
                "Конфигурация клиента",
                JOptionPane.QUESTION_MESSAGE);
    }

    public void notifyConnectionStatusChanged(boolean clientConnected) {
        textField.setEditable(clientConnected);
        if (clientConnected) {
            JOptionPane.showMessageDialog(
                    frame,
                    "Соединение с сервером установлено",
                    "Чат",
                    JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(
                    frame,
                    "Клиент не подключен к серверу",
                    "Чат",
                    JOptionPane.ERROR_MESSAGE);
        }

    }

    /**
     * Сохраненем и обновляем поле Messages
     */
    public void refreshMessages() {
        messages.append(controller.getModel().getNewMessage() + "\n");
    }

    /**
     * Обновляем список пользователей в поле
     */
    public void refreshUsers() {
        ClientGuiModel model = controller.getModel();
        StringBuilder sb = new StringBuilder();
        for (String userName : model.getAllUserNames()) {
            sb.append(userName).append("\n");
        }
        users.setText(sb.toString());
    }
}
