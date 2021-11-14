package ru.dmitrii.client.gui;

import ru.dmitrii.client.Client;
import ru.dmitrii.utils.models.MessageType;

// Реализуем Client в swing
public class ClientGuiController extends Client {
    private final ClientGuiModel model;
    private final ClientGuiView view;

    public ClientGuiController() {
        model = new ClientGuiModel();
        view = new ClientGuiView(this);
    }

    @Override
    protected SocketThread getSocketThread() {
        return new GuiSocketThread();
    }

    @Override
    public void run() {
        SocketThread socketThread = getSocketThread();
        socketThread.run();
    }

    @Override
    protected String getServerAddress() {
        return view.getServerAddress();
    }

    @Override
    protected int getServerPort() {
        return view.getServerPort();
    }

    @Override
    protected MessageType getAuthorization() {
        return view.getAuthorization();
    }

    @Override
    protected String getUserName() {
        return view.getUserName();
    }

    @Override
    protected String getPassword() {
        return view.getPassword();
    }

    public ClientGuiModel getModel() {
        return model;
    }


    public static void main(String[] args) {
        ClientGuiController clientGuiController = new ClientGuiController();
        clientGuiController.run();
    }

    /**
     * Переопределяем внутренний класс Client для работы с Gui
     */
    public class GuiSocketThread extends SocketThread {

        /**
         * Полученное сообщение сохраняем и обновляем поле
         *
         * @param message String
         */
        @Override
        protected void processIncomingMessage(String message) {
            model.setNewMessage(message);
            view.refreshMessages();
        }

        /**
         * Сохраняем имя пользователя и обновляем список
         *
         * @param userName String
         */
        @Override
        protected void informAddUser(String userName) {
            model.addUser(userName);
            view.refreshUsers();
        }

        /**
         * Удаляем имя пользователя и обновляем список
         *
         * @param userName String
         */
        @Override
        protected void informDeleteUser(String userName) {
            model.deleteUser(userName);
            view.refreshUsers();
        }

        /**
         * Изменить статус соединения
         *
         * @param clientConnected boolean
         */
        @Override
        protected void notifyStatusConnection(boolean clientConnected) {
            super.notifyStatusConnection(clientConnected);
            view.notifyConnectionStatusChanged(clientConnected);
        }
    }
}
