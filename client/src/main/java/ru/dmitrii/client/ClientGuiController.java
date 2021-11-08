package ru.dmitrii.client;

public class ClientGuiController extends Client{
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
        SocketThread socketThread= getSocketThread();
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
    protected String getUserName() {return view.getUserName();
    }
    @Override
    protected String getPassword() { return view.getPassword();
    }

    public ClientGuiModel getModel() {
        return model;
    }



    public static void main(String[] args) {
        ClientGuiController clientGuiController = new ClientGuiController();
        clientGuiController.run();
    }

    public class GuiSocketThread extends SocketThread{
        @Override
        protected void processIncomingMessage(String message) {
            model.setNewMessage(message);
            view.refreshMessages();
        }
        @Override
        protected void informAddUser(String userName) {
            model.addUser(userName);
            view.refreshUsers();
        }
        @Override
        protected void informDeleteUser(String userName) {
            model.deleteUser(userName);
            view.refreshUsers();
        }

        @Override
        protected void notifyStatusConnection(boolean clientConnected) {
            super.notifyStatusConnection(clientConnected);
            view.notifyConnectionStatusChanged(clientConnected);
        }
    }
}
