package com.company;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class ClientMain extends Application
{
    @Override
    public void start(Stage stage)
    {
        Parent root = clientAPI.loginMenuController.getParent();

        Scene scene = new Scene(root, 450, 600);
        stage.setScene(scene);
        stage.setTitle("Chat!");
        stage.show();
    }

    private static ClientAPI clientAPI;

    static
    {
        try
        {
            clientAPI = new ClientAPI();
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException
    {
        clientAPI.connect();

        new Thread(() ->
        {
            Platform.setImplicitExit(false);
            launch(args);
        }).start();

        clientAPI.startMessageReader();
    }
}
