package com.company;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.*;
import java.net.Socket;
import java.util.*;

public class ClientAPI
{
    LoginMenuController loginMenuController = new LoginMenuController();
    private MainMenuController mainMenuController = new MainMenuController();
    private DirectChat directChatMenuController;
    private MakeGroup makeGroup;
    private GroupChat groupChatMenuController;
    ChatPage currentChatPage;

    private Set<ChatBox> userBoxes = Collections.synchronizedSet(new HashSet<>());
    private Set<ChatBox> groupBoxes = Collections.synchronizedSet(new HashSet<>());

    private String userName;

    private String IP;
    private int port;
    private Socket socket;
    private OutputStream out;
    private InputStream in;
    private Scanner scanner;

    public ClientAPI() throws FileNotFoundException
    {
        this.IP = "localhost";
        this.port = 5555;
    }

    public void connect() throws IOException
    {
        this.socket = new Socket(IP, port);
        this.out = this.socket.getOutputStream();
        this.in = this.socket.getInputStream();
        this.scanner = new Scanner(in);
    }

    public void startMessageReader()
    {
        String line;
        while ((line = scanner.nextLine()) != null)
        {
            try
            {
                System.out.println("Message From Server --> " + line);

                String[] tokens = line.split(" ");
                if (tokens.length > 0)
                {
                    String command = tokens[0];
                    if (command.equalsIgnoreCase("online"))
                        handleOnline(tokens[1]);

                    else if (command.equalsIgnoreCase("offline"))
                        handleOffline(tokens[1]);

                    else if (command.equalsIgnoreCase("Quit"))
                        handleQuit();

                    else if (command.equalsIgnoreCase("InviteGroup"))
                        handleInvite(tokens);

                    else if (command.equalsIgnoreCase("Message"))
                        handleMessage(tokens);
                }
            }
            catch (IOException | InterruptedException e)
            {
                e.printStackTrace();
            }
        }
    }

    private void handleInvite(String[] tokens)
    {
        if (tokens.length > 1)
        {
            String groupName = tokens[1].replaceFirst("#", "");
            groupBoxes.add(new ChatBox(groupName, ChatType.GROUP));
        }
    }

    private void handleMessage(String[] tokens)
    {
        boolean fromGroup = tokens[1].charAt(0) == '#';

        if (fromGroup)
        {
            String groupName = tokens[1];
            String messenger = tokens[2].replaceFirst("@", "");
            String messageBody = tokens[3];

            ChatBox groupBox = findGroupBox(groupName);

            ChatPage.Message message = groupBox.chatPage.new Message(messenger, messageBody, false);

            Platform.runLater(() ->
            {
                groupBox.chatPage.messageVBox.getChildren().add(message);
                message.playAnimation();
            });
        }
        else
        {
            String messenger = tokens[1].replaceFirst("@", "");
            String messageBody = tokens[2];

            ChatBox userBox = findUserBox(messenger);

            ChatPage.Message message = userBox.chatPage.new Message(messenger, messageBody, false);

            Platform.runLater(() ->
            {
                userBox.chatPage.messageVBox.getChildren().add(message);
                message.playAnimation();
            });
        }
    }

    private ChatBox findUserBox(String userName)
    {
        for (ChatBox userBox : this.userBoxes)
        {
            String receiver = userBox.receiver.replaceFirst("@", "");
            if (receiver.equals(userName))
                return userBox;
        }

        return null;
    }

    private ChatBox findGroupBox(String groupName)
    {
        for (ChatBox groupBox : this.groupBoxes)
        {
            String receiver = groupBox.receiver;
            if (receiver.equals(groupName))
                return groupBox;
        }

        return null;
    }

    void handleOnline(String userName) throws FileNotFoundException, InterruptedException
    {
        Platform.runLater(() ->
        {
            if (findUserBox(userName) == null)
                userBoxes.add(new ChatBox(userName, ChatType.DIRECT));
        });
    }

    void handleOffline(String userName) throws FileNotFoundException, InterruptedException
    {
        Platform.runLater(() ->
        {
            userBoxes.removeIf(userBox -> userBox.receiver.equals(userName));
        });
    }

    void handleQuit() throws IOException
    {
        String command = "Quit";
        out.write(command.getBytes());
        out.flush();
    }

    void sendMessage(String message) throws IOException
    {
        this.out.write((message + "\n").getBytes());
        this.out.flush();
    }


    public class LoginMenuController
    {
        TextField userNameTextField = new TextField();
        VBox vBox;

        LoginMenuController() throws FileNotFoundException
        {
            userNameTextField.setOnKeyPressed(event ->
            {
                try
                {
                    if (event.getCode() == KeyCode.ENTER)
                    {
                        String userName = userNameTextField.getText();
                        ClientAPI.this.userName = userName;
                        ClientAPI.this.sendMessage("Login " + userName + " " + "NoPassWord");
                        changeMenuTo(event, mainMenuController.getParent());
                    }
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            });

            userNameTextField.setPromptText("Enter User Name");
            userNameTextField.setAlignment(Pos.CENTER);
            userNameTextField.setStyle("-fx-background-color: #d3ffff;" +
                    "-fx-background-radius: 100;" +
                    "-fx-border-style: Solid;" +
                    "-fx-border-radius:100;" +
                    "-fx-border-width: 2;" +
                    "-fx-border-color: #65e4ff");

            vBox = new VBox(userNameTextField);
            vBox.setAlignment(Pos.BOTTOM_CENTER);
            vBox.setPadding(new Insets(100));
            vBox.setStyle("-fx-background-image: url(com/company/loginmenubc.jpg);" +
                    "-fx-background-repeat: Stretch;" +
                    "-fx-background-size: 450 600;" +
                    "-fx-background-position: center center;");

        }

        Parent getParent()
        {
            return vBox;
        }
    }

    class MainMenuController extends AbstractMenu
    {
        Button makeGroupButton = new Button();
        Button directChatButton = new Button();
        Button groupChatButton = new Button();
        Button quitButton = new Button();
        VBox vBox = new VBox(directChatButton, groupChatButton, makeGroupButton, quitButton);

        MainMenuController()
        {
            directChatButton.setText("Direct Chat");
            directChatButton.setTextFill(Color.WHITE);
            directChatButton.setStyle("-fx-background-color: #00dbff;" +
                    "-fx-background-radius: 100;" +
                    "-fx-border-style: Solid;" +
                    "-fx-border-color: #006a7d;" +
                    "-fx-border-radius: 100");

            directChatButton.setPrefSize(120, 30);
            directChatButton.resize(120, 30);
            directChatButton.setOnMouseClicked(event ->
            {
                directChatMenuController = new DirectChat();
                directChatMenuController.initialize();
                changeMenuTo(event, directChatMenuController.getParent());
            });

            groupChatButton.setText("Group Chat");
            groupChatButton.setTextFill(Color.WHITE);
            groupChatButton.setStyle("-fx-background-color: #00dbff;" +
                    "-fx-background-radius: 100;" +
                    "-fx-border-style: Solid;" +
                    "-fx-border-color: #006a7d;" +
                    "-fx-border-radius: 100");

            groupChatButton.setPrefSize(155, 35);
            groupChatButton.resize(155, 35);
            groupChatButton.setOnMouseClicked(event ->
            {
                groupChatMenuController = new GroupChat();
                groupChatMenuController.initialize();
                changeMenuTo(event, groupChatMenuController.getParent());
            });

            makeGroupButton.setText("Make Group");
            makeGroupButton.setTextFill(Color.WHITE);
            makeGroupButton.setStyle("-fx-background-color: #00dbff;" +
                    "-fx-background-radius: 100;" +
                    "-fx-border-style: Solid;" +
                    "-fx-border-color: #006a7d;" +
                    "-fx-border-radius: 100");

            makeGroupButton.setOnMouseClicked(event ->
            {
                makeGroup = new MakeGroup();
                changeMenuTo(event, makeGroup.getParent());
            });

            makeGroupButton.setPrefSize(195, 40);
            makeGroupButton.resize(195, 40);


            quitButton.setText("Quit");
            quitButton.setTextFill(Color.WHITE);
            quitButton.setStyle("-fx-background-color: #00dbff;" +
                    "-fx-background-radius: 100;" +
                    "-fx-border-style: Solid;" +
                    "-fx-border-color: #006a7d;" +
                    "-fx-border-radius: 100");


            quitButton.setPrefSize(240, 45);
            quitButton.resize(240, 45);
            quitButton.setOnMouseClicked(event ->
            {
                System.exit(0);
            });

            vBox.setPadding(new Insets(15));
            vBox.setSpacing(10);
            vBox.setAlignment(Pos.BOTTOM_RIGHT);
            vBox.setStyle("-fx-background-image: url(com/company/mainmenubc.jpg);" +
                    "-fx-border-style: Solid;" +
                    "-fx-border-width: 5;" +
                    "-fx-border-color: #cdf8ff;");
        }

        @Override
        Parent getParent()
        {
            return vBox;
        }
    }

    class MakeGroup extends AbstractMenu
    {
        TextField groupNameTextField = new TextField();
        ArrayList<CheckBox> checkBoxes = new ArrayList<>();
        Button makeGroupButton = new Button();
        VBox mainVBox = new VBox();

        MakeGroup()
        {
            groupNameTextField.setPromptText("Enter Group Name");
            groupNameTextField.setMaxSize(300, 30);
            groupNameTextField.setPrefSize(300, 30);
            groupNameTextField.setMinSize(300, 30);
            groupNameTextField.resize(300, 30);
            groupNameTextField.setStyle("-fx-background-radius: 50;" +
                    "-fx-background-color: #dffcff;");

            System.out.println("This Is Fucking " + userBoxes.size());
            for (ChatBox userBox : userBoxes)
            {
                CheckBox checkBox = new CheckBox(userBox.receiver);

                checkBox.resize(300, 25);
                checkBox.setMaxSize(300, 25);
                checkBox.setMinSize(300, 25);

                checkBox.setTextFill(Color.WHITE);
                checkBox.setStyle("-fx-background-color: #00d9ff;" +
                        "-fx-border-style: Solid;" +
                        "-fx-border-color: #006a7d;" +
                        "-fx-border-width: 1");

                checkBoxes.add(checkBox);
            }

            mainVBox.setPadding(new Insets(10));
            mainVBox.setSpacing(6);
            mainVBox.setStyle("-fx-background-image: url(com/company/makegroupbc.jpg);" +
                    "-fx-background-size: auto");
            mainVBox.setAlignment(Pos.TOP_CENTER);

            makeGroupButton.setText("Make Group");
            makeGroupButton.setStyle("-fx-background-color: #dffcff;" +
                    "-fx-background-radius: 50");

            makeGroupButton.setOnMouseClicked(event ->
            {
                String messageToServer = "MakeGroup ";

                String groupName = groupNameTextField.getText();
                messageToServer = messageToServer.concat("#" + groupName);

                String groupBuilderUsername = ClientAPI.this.userName;
                messageToServer = messageToServer.concat(" @" + groupBuilderUsername);

                for (CheckBox checkBox : checkBoxes)
                    if (checkBox.isSelected())
                    {
                        String userSelectedToAddInGroup = " " + checkBox.getText();
                        messageToServer = messageToServer.concat(userSelectedToAddInGroup);
                    }

                try
                {
                    sendMessage(messageToServer);
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            });

            mainVBox.getChildren().add(groupNameTextField);
            mainVBox.getChildren().addAll(checkBoxes);
            mainVBox.getChildren().addAll(makeGroupButton);

            mainVBox.setOnKeyPressed(keyEvent ->
            {
                if (keyEvent.getCode() == KeyCode.ESCAPE)
                {
                    mainMenuController = new MainMenuController();
                    changeMenuTo(keyEvent, mainMenuController.getParent());
                }
            });
        }

        @Override
        Parent getParent()
        {
            return mainVBox;
        }
    }

    abstract class BasicChat extends AbstractMenu
    {
        protected Label label = new Label();
        protected VBox mainVBox = new VBox(10, label);

        BasicChat()
        {
            mainVBox.setAlignment(Pos.TOP_CENTER);
            mainVBox.setStyle("-fx-background-image: url(com/company/directmenubc.jpg);" +
                    "-fx-border-color: #cdf8ff;" +
                    "-fx-border-style: Solid;" +
                    "-fx-border-radius: 20");
            mainVBox.setPadding(new Insets(15));

            label.setAlignment(Pos.CENTER);
            label.setTextFill(Color.BLACK);
            label.setFont(Font.font("Sakkal Majalla", 24));
            label.setPrefSize(120, 50);
            label.resize(120, 50);
            label.setStyle("-fx-background-color: #cdf8ff;" +
                    "-fx-border-style: Solid;" +
                    "-fx-border-radius: 5;" +
                    "-fx-border-color: White;");

            label.setOnMouseClicked(event ->
            {
                mainMenuController = new MainMenuController();
                changeMenuTo(event, mainMenuController.getParent());
            });
        }

        abstract VBox initialize();

        Parent getParent()
        {
            return mainVBox;
        }
    }

    class DirectChat extends BasicChat
    {
        DirectChat()
        {
            super();
            label.setText("Online :");

        }

        @Override
        public VBox initialize()
        {
            for (ChatBox userBox : userBoxes)
                this.mainVBox.getChildren().add(userBox);

            return null;
        }
    }

    class GroupChat extends BasicChat
    {
        GroupChat()
        {
            super();
            label.setText("Groups :");

        }

        @Override
        public VBox initialize()
        {
            for (ChatBox groupBox : groupBoxes)
                this.mainVBox.getChildren().add(groupBox);

            return null;
        }

    }

    class ChatPage extends AbstractMenu
    {
        String receiver;

        VBox mainVBox = new VBox(3);

        Button backButton = new Button();
        VBox messageVBox = new VBox(2);

        HBox textFieldAndDir = new HBox(4);
        Button dirButton = new Button("...");
        TextField chatTextField = new TextField();

        ChatPage(String receiver)
        {
            this.receiver = receiver;

            backButton.setPrefSize(450, 32);
            backButton.resize(450, 32);
            backButton.setText("Back");
            backButton.setAlignment(Pos.CENTER);
            backButton.setFont(Font.font("Georgia", 24));
            backButton.setStyle("-fx-background-color: #00dbff");

            backButton.setOnMouseClicked(event ->
            {
                MainMenuController menu = new MainMenuController();
                changeMenuTo(event, menu.getParent());
            });

            messageVBox.setPadding(new Insets(10));
            messageVBox.setStyle("-fx-background-image: url(com/company/chatbc.jpg);" +
                    "-fx-background-radius: 10;");

            messageVBox.setMaxSize(450, 50);
            messageVBox.setPrefSize(450, 500);
            messageVBox.resize(450, 500);
            messageVBox.setAlignment(Pos.TOP_CENTER);

            chatTextField.setMaxSize(400, 40);
            chatTextField.setPrefSize(400, 40);
            chatTextField.resize(400, 40);
            chatTextField.setPadding(new Insets(3));
            chatTextField.setPromptText(" ✔ Enter Your Message                                               (Emoji Win + . Key)");
            chatTextField.setStyle("-fx-background-radius: 15;" +
                    "-fx-border-style: Solid;" +
                    "-fx-border-width: 2;" +
                    "-fx-border-radius: 15;" +
                    "-fx-border-color: #5bddff");

            chatTextField.setOnKeyPressed(keyEvent ->
            {
                if (keyEvent.getCode() == KeyCode.ENTER)
                {
                    try
                    {
                        String messageToServer = "Message " + receiver + " " + chatTextField.getText();
                        Message messageToShow = new Message(ClientAPI.this.userName, chatTextField.getText(), true);
                        this.messageVBox.getChildren().add(messageToShow);
                        messageToShow.playAnimation();
                        sendMessage(messageToServer);
                        chatTextField.clear();
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
            });

            dirButton.setMaxSize(40, 40);
            dirButton.setMinSize(40, 40);
            dirButton.resize(40, 40);

            dirButton.setStyle("-fx-background-color: #dffcff;" +
                    "-fx-background-radius: 100;" +
                    "-fx-border-style: Solid;" +
                    "-fx-border-color: #5bddff");

            DirectoryChooser dirChooser = new DirectoryChooser();
            dirChooser.setInitialDirectory(new File("src"));
            dirButton.setOnMouseClicked(event ->
            {
                Stage currentStage = ((Stage) ((Node) event.getSource()).getScene().getWindow());
                File selectedDirectory = dirChooser.showDialog(currentStage);
            });

            textFieldAndDir.getChildren().addAll(chatTextField, dirButton);

            mainVBox.getChildren().addAll(backButton, messageVBox, textFieldAndDir);
            mainVBox.setStyle("-fx-background-image: url(com/company/chatbc.jpg)");
            mainVBox.setAlignment(Pos.TOP_CENTER);
        }

        class Message extends HBox
        {
            Label label = new Label();
            Label messengerLabel = new Label();

            Message(String messenger, String messageBody, boolean fromLeft)
            {
                this.setSpacing(3);

                messengerLabel.setPrefSize(30, 30);
                messengerLabel.resize(30, 30);
                messengerLabel.setText(((Character) messenger.charAt(0)).toString());
                messengerLabel.setAlignment(Pos.CENTER);
                messengerLabel.setFont(Font.font("Comic Sans MS", 18));
                messengerLabel.setStyle("-fx-background-color: #00d9ff;" +
                        "-fx-background-radius: 500;" +
                        "-fx-border-style: Solid;" +
                        "-fx-border-width: 3;" +
                        "-fx-border-radius: 500;" +
                        "-fx-border-color: #cdf8ff;");

                label.setText(messageBody);
                label.setFont(Font.font("Comic Sans MS", 12));
                label.setPrefWidth(400);
                label.setTextAlignment(TextAlignment.CENTER);
                label.setAlignment(Pos.CENTER);
                label.setPrefHeight(30);
                label.setMinHeight(30);
                label.setPadding(new Insets(5));
                label.setStyle("-fx-background-color: #b9fcff;" +
                        "-fx-background-radius: 100;" +
                        "-fx-border-style: Solid;" +
                        "-fx-border-color: #cdf8ff;" +
                        "-fx-border-radius: 100");

                if (fromLeft)
                {
                    this.label.setAlignment(Pos.CENTER_LEFT);
                    this.getChildren().addAll(this.messengerLabel, label);
                }
                else
                {
                    this.label.setAlignment(Pos.CENTER_RIGHT);
                    this.getChildren().addAll(label, this.messengerLabel);
                }

                label.setOnMouseClicked(event ->
                {
                    try
                    {
                        Message intendedMessage = this;
                        intendedMessage.messengerLabel.setEffect(new GaussianBlur(.4));

                        String messageToServer = "Message " + receiver + " ";

                        String replyToThisMessage = intendedMessage.label.getText();
                        messageToServer = messageToServer.concat(replyToThisMessage);

                        messageToServer = messageToServer.concat("-(♋)-");

                        String replyMessage = chatTextField.getText();
                        messageToServer = messageToServer.concat(replyMessage);
                        Message messageToShow = new Message(ClientAPI.this.userName, replyToThisMessage + "-(♋)-" + replyMessage, true);
                        currentChatPage.messageVBox.getChildren().add(messageToShow);
                        messageToShow.playAnimation();
                        sendMessage(messageToServer);
                        chatTextField.clear();
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                });

                this.setAlignment(Pos.CENTER);
            }

            public void playAnimation()
            {
                FadeTransition fadeMessage = new FadeTransition(Duration.seconds(.5), this);
                fadeMessage.setFromValue(0);
                fadeMessage.setToValue(1);
                fadeMessage.play();
            }
        }

        @Override
        Parent getParent()
        {
            return mainVBox;
        }
    }

    class ChatBox extends HBox
    {
        ChatPage chatPage;
        String receiver;

        Label image = new Label();
        Label label = new Label();

        ChatBox(String receiver, ChatType chatType)
        {
            super(10);

            label.setPrefSize(400, 40);
            label.setText(" " + receiver);
            label.setFont(Font.font("Microsoft JhengHei Light", 24));
            label.setTextAlignment(TextAlignment.LEFT);
            label.setAlignment(Pos.CENTER_LEFT);
            label.setPadding(new Insets(5));

            label.setStyle("-fx-background-radius: 100;" +
                    "-fx-border-style: Solid;" +
                    "-fx-border-width: 2;" +
                    "-fx-border-color: #006a7d;" +
                    "-fx-border-radius: 100");

            image.setPrefSize(48, 48);
            image.resize(48, 48);
            image.setMinSize(48, 48);
            image.setTextAlignment(TextAlignment.CENTER);
            image.setAlignment(Pos.CENTER);
            image.setFont(Font.font("Comic Sans MS", 18));
            image.setTextFill(Color.WHITE);
            image.setText(((Character) receiver.charAt(0)).toString());
            image.setStyle("-fx-background-color: #78ff76;" +
                    "-fx-background-radius: 100;" +
                    "-fx-border-style: Solid;" +
                    "-fx-border-color: #006a7d;" +
                    "-fx-border-radius: 100");

            this.getChildren().addAll(image, label);

            switch (chatType)
            {
                case GROUP:
                    this.receiver = "#" + receiver;
                    break;
                case DIRECT:
                    this.receiver = "@" + receiver;
                    break;
            }


            label.setOnMouseClicked(event ->
            {
                chatPage = new ChatPage(this.receiver);
                currentChatPage = chatPage;
                changeMenuTo(event, chatPage.getParent());
            });
        }
    }

    enum ChatType
    {
        DIRECT, GROUP
    }

    public void changeMenuTo(Event event, Parent parent)
    {
        Stage stage = ((Stage) ((Node) event.getSource()).getScene().getWindow());
        Scene scene = new Scene(parent, 450, 600);
        stage.setTitle("Chat!");
        stage.setScene(scene);

        FadeTransition fillTransition = new FadeTransition(Duration.millis(500), parent);
        fillTransition.setFromValue(.2);
        fillTransition.setToValue(1);

        fillTransition.play();

        stage.show();
    }
}
