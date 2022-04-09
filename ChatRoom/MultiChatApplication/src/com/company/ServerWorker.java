package com.company;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class ServerWorker extends Thread
{
    private final Socket clientSocket;
    private final Server server;
    private String userName;
    private String passWord;
    private OutputStream out;
    private InputStream in;

    private Set<Group> groups = Collections.synchronizedSet(new HashSet<>());


    public ServerWorker(Server server, Socket clientSocket)
    {
        this.server = server;
        this.clientSocket = clientSocket;
    }

    @Override
    public void run()
    {
        try
        {
            handleClientSocket();
        }
        catch (IOException | InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    private void handleClientSocket() throws IOException, InterruptedException
    {
        this.in = clientSocket.getInputStream();
        this.out = clientSocket.getOutputStream();

        Scanner scanner = new Scanner(in);

        String line;
        while ((line = scanner.nextLine()) != null)
        {
            System.out.println("New Commend From User : " + this.userName + " --> "  + line);
            String[] tokens = line.split(" ");
            if (tokens.length > 0)
            {
                String commandType = tokens[0];
                if (commandType.equalsIgnoreCase("Quit")) // logout
                    handleLogout();

                else if (commandType.equalsIgnoreCase("Login")) // login
                    handleLogin(tokens);

                else if (commandType.equalsIgnoreCase("MakeGroup")) // make group
                    handleMakeGroup(tokens);

                else if (commandType.equalsIgnoreCase("JoinToGroup")) // join group
                    handleJoinToGroup(tokens);

                else if (commandType.equalsIgnoreCase("LeaveGroup")) // lve group
                    handleLeave(tokens);

                else if (commandType.equalsIgnoreCase("AdminGroup")) // group admins
                    handleShowAdmins(tokens);

                else if (commandType.equalsIgnoreCase("PromoteToAdmin")) //promote to admin
                    handlePromoteToAdmin(tokens);

                else if (commandType.equalsIgnoreCase("KickFromGroup")) // kick from group
                    handleKick(tokens);

                else if (commandType.equalsIgnoreCase("AllGroups")) //all gps
                    handleShowAllGps();

                else if (commandType.equalsIgnoreCase("OnlineUsers"))
                    handleToShowOnlineUsers();

                else if (commandType.equalsIgnoreCase("ClientGroups")) //client gps
                    handleShowClientGps();

                else if (commandType.equalsIgnoreCase("Message")) // message gp or direct
                    handleMessage(tokens);

            }
        }
    }

    private void handleToShowOnlineUsers() throws IOException
    {
        for (ServerWorker worker : this.server.getWorkers())
            this.send("online " + worker.getUserName());

    }

    private void handleShowAdmins(String[] tokens) throws IOException
    {
        if (tokens.length == 2)
        {
            String groupName = tokens[1].replaceFirst("#", "");
            Group group = findGroupInClient(groupName);

            for (ServerWorker admin : group.getAdmins())
                this.send(admin.getUserName());
        }
    }

    private void handlePromoteToAdmin(String[] tokens) throws IOException
    {
        if (tokens.length > 2)
        {
            String groupName = tokens[1].replaceFirst("#", "");
            String userNameOfWhoWantToPromote = tokens[2];

            System.out.println(" userNameOfWhoWantToPromote :  " + userNameOfWhoWantToPromote);

            Group group = findGroupInClient(groupName);

            if (group != null)
            {
                ServerWorker userWhoWantToPromote = group.findUserInGroup(userNameOfWhoWantToPromote);
                if (this.isAdmin(group))
                {
                    if (userWhoWantToPromote != null)
                    {
                        if (!userWhoWantToPromote.isAdmin(group))
                            group.promoteToAdmin(userWhoWantToPromote);

                        else
                            this.send(userWhoWantToPromote.userName + " is Admin Already");
                    }
                    else
                        this.send("No Such User With This Name In This Group !");
                }
                else
                    this.send("You are Not Admin");
            }
            else
                this.send("No Such Group With this Name");
        }
    }

    private void handleLeave(String[] tokens) throws IOException
    {
        if (tokens.length > 1)
        {
            String groupName = tokens[1].replaceFirst("#", "");
            Group group = findGroupInClient(groupName);

            if (group != null)
            {
                group.removeUserFromGroup(this);
                this.groups.remove(group);
                this.out.write(("You Left From " + group.getGroupName()).getBytes());
            }
            else
                this.out.write("No Such Group in Your Groups !".getBytes());
        }
    }

    private void handleKick(String[] tokens) throws IOException
    {
        if (tokens.length > 2)
        {
            String groupName = tokens[1].replaceFirst("#", "");
            String userNameOfWhoWantToKick = tokens[2];

            Group group = findGroupInClient(groupName);

            if (group != null)
            {
                ServerWorker userWhoWantToKick = group.findUserInGroup(userNameOfWhoWantToKick);
                if (userNameOfWhoWantToKick != null)
                {
                    if (this.isGroupBuilder(group))
                    {
                        group.removeUserFromGroup(userWhoWantToKick);
                        userWhoWantToKick.groups.remove(group);
                    }
                    else if (this.isAdmin(group))

                        if (!userWhoWantToKick.isAdmin(group))
                        {
                            group.removeUserFromGroup(userWhoWantToKick);
                            userWhoWantToKick.groups.remove(group);
                        }
                        else
                            this.send("Admins Cant Kick Other Admins\n");

                    else
                        this.send("You are Not Admin\n");
                }
                else
                    this.send("No Such User With This Name In This Group !\n");
            }
            else
                this.send("No Such Group With This Name !\n");
        }
    }

    private boolean isAdmin(Group group)
    {
        return group.getAdmins().contains(this);
    }


    private void handleShowClientGps() throws IOException
    {
        for (Group group : this.groups)
            this.send(group.getGroupName());
    }

    private void handleShowAllGps() throws IOException
    {
        for (ServerWorker worker : this.server.getWorkers())
            for (Group group : worker.groups)
                this.send(group.getGroupName() + "\n");
    }

    // make #gpname
    private void handleMakeGroup(String[] tokens) throws IOException
    {
        if (tokens.length > 2)
        {
            String groupName = tokens[1].replaceFirst("#", "");
            Group group = new Group(groupName, this, false);

            this.groups.add(group);

            for (int i = 2; i < tokens.length; i++)
            {
                String userInvitedUserName = tokens[i].replaceFirst("@", "");
                ServerWorker userInvited = this.server.findUserByName(userInvitedUserName);
                userInvited.send("InviteGroup #" + groupName);
                userInvited.groups.add(group);
                group.addUserToGroup(userInvited);
            }

            this.send("Group " + groupName + " Made");
        }
    }

    private Group findGroupInServer(String groupName)
    {
        for (ServerWorker worker : this.server.getWorkers())
            for (Group gp : worker.groups)
                if (gp.getGroupName().equalsIgnoreCase(groupName))
                    return gp;

        return null;
    }

    private Group findGroupInClient(String groupName)
    {
        for (Group group : groups)
            if (group.getGroupName().equals(groupName))
                return group;

        return null;
    }

    private void handleJoinToGroup(String[] tokens) throws IOException
    {
        if (tokens.length > 1)
        {
            String groupName = tokens[1].replaceFirst("#", "");

            Group group = findGroupInServer(groupName);

            if (group != null)
            {
                if (group.isPrivate())
                {
                    //TODO private group
                }
                else
                {
                    this.groups.add(group);
                    group.addUserToGroup(this);
                    this.send("You Joined To Group " + groupName);
                }
            }
            else
                this.send("No Such Group With this Group Name !");
        }

    }

    private boolean isMemberOfGroup(String groupName)
    {
        return findGroupInClient(groupName) != null;
    }

    private boolean isGroupBuilder(Group group)
    {
        return group.getGroupBuilder().equals(this);
    }

    private void handleMessage(String[] tokens) throws IOException
    {
        // message format : msg + sendToUsername + bodyMessage
        String sendToDetails = tokens[1];
        String bodyMessage = "";

        for (int i = 2; i < tokens.length; i++)
            bodyMessage = bodyMessage.concat(tokens[i]);

        boolean inGroup = sendToDetails.charAt(0) == '#';

        if (inGroup)
        {
            String groupMessageSendToGroup = sendToDetails.replaceFirst("#", "");

            if (this.isMemberOfGroup(groupMessageSendToGroup))
            {
                for (ServerWorker worker : server.getWorkers())
                    if (worker.isMemberOfGroup(groupMessageSendToGroup))
                        if (!worker.equals(this))
                            worker.send("Message " + "#" + groupMessageSendToGroup + " " + "@" + this.userName + " " + bodyMessage);
            }
            else
                this.send("You are not in this group");

        }
        else
        {
            String directMessageSendToUser = sendToDetails.replaceFirst("@", "");
            for (ServerWorker worker : server.getWorkers())
                if (directMessageSendToUser.equals(worker.userName))
                    worker.send("Message " + this.userName + " " + bodyMessage);
        }
    }

    private void handleLogout() throws IOException
    {
        server.getWorkers().remove(this);
        notifyAllChangeState("offline ");
        clientSocket.close();
    }

    private void handleLogin(String[] tokens) throws IOException
    {
        if (tokens.length == 3)
        {
            String userName = tokens[1];
            String passWord = tokens[2];

            this.userName = userName;
            this.passWord = passWord;

            notifyAllChangeState("online ");
            notifyFromConnectedUsers();
        }
    }

    private void notifyFromConnectedUsers() throws IOException
    {
        for (ServerWorker worker : server.getWorkers())
            if (worker.userName != null && !this.equals(worker))
                this.send("online " + worker.userName);
    }

    private void notifyAllChangeState(String userCurrentState) throws IOException
    {
        String clientConnectedMessage = userCurrentState + this.userName;
        for (ServerWorker worker : server.getWorkers())
            if (!this.equals(worker))
                worker.send(clientConnectedMessage);
    }

    private void send(String clientConnectedMessage) throws IOException
    {
        out.write((clientConnectedMessage + "\n").getBytes());
        out.flush();
    }

    String getUserName()
    {
        return userName;
    }
}
