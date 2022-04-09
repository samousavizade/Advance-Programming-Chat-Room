package com.company;

import java.net.ServerSocket;
import java.nio.file.Path;
import java.util.ArrayList;

public class Group
{
    public Group(String groupName, ServerWorker userNameGroupBuilder, boolean isPrivate)
    {
        this.groupName = groupName;
        this.groupBuilder = userNameGroupBuilder;
        admins.add(userNameGroupBuilder);
        groupMembers.add(userNameGroupBuilder);
        this.isPrivate = isPrivate;
    }

    private String groupName;
    private ServerWorker groupBuilder;
    private ArrayList<ServerWorker> admins = new ArrayList<>();
    private ArrayList<ServerWorker> groupMembers = new ArrayList<>();
    private boolean isPrivate;

    public ServerWorker findUserInGroup(String userName)
    {
        for (ServerWorker groupMember : groupMembers)
            if(groupMember.getUserName().equals(userName))
                return groupMember;

        return null;
    }

    public void addUserToGroup(ServerWorker user)
    {
        groupMembers.add(user);
    }

    public void removeUserFromGroup(ServerWorker user)
    {
        groupMembers.remove(user);
        admins.remove(user);
    }

    public void demoteFromAdmin(ServerWorker user)
    {
        admins.remove(user);
    }

    public void promoteToAdmin(ServerWorker user)
    {
        admins.add(user);
    }

    public String getGroupName()
    {
        return groupName;
    }

    public void setGroupName(String groupName)
    {
        this.groupName = groupName;
    }

    public boolean isPrivate()
    {
        return isPrivate;
    }

    public void setPrivate(boolean aPrivate)
    {
        isPrivate = aPrivate;
    }

    public ServerWorker getGroupBuilder()
    {
        return groupBuilder;
    }

    public ArrayList<ServerWorker> getAdmins()
    {
        return admins;
    }
}
