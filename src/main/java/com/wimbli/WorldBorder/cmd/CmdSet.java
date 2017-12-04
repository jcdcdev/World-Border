package com.wimbli.WorldBorder.cmd;

import com.wimbli.WorldBorder.Config;
import com.wimbli.WorldBorder.WorldBorder;
import com.wimbli.WorldBorder.forge.Util;
import com.wimbli.WorldBorder.forge.Worlds;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;

import java.util.List;


public class CmdSet extends WBCmd
{
    public CmdSet()
    {
        name = permission = "set";
        hasWorldNameInput = true;
        consoleRequiresWorldName = false;
        minParams = 1;
        maxParams = 4;

        addCmdExample(nameEmphasizedW() + "<radiusX> [radiusZ] <x> <z> - use x/z coords.");
        addCmdExample(nameEmphasizedW() + "<radiusX> [radiusZ] ^spawn - use spawn point.");
        addCmdExample(nameEmphasized()  + "<radiusX> [radiusZ] - set border, centered on you.", true, false, true);
        addCmdExample(nameEmphasized()  + "<radiusX> [radiusZ] ^player <name> - center on player.");
        helpText = "Set a border for a world, with several options for defining the center location. [world] is " +
            "optional for players and defaults to the world the player is in. If [radiusZ] is not specified, the " +
            "radiusX value will be used for both. The <x> and <z> coordinates can be decimal values (ex. 1.234).";
    }

    @Override
    public void execute(ICommandSender sender, EntityPlayerMP player, List<String> params, String worldName)
    {
        // passsing a single parameter (radiusX) is only acceptable from player
        if ((params.size() == 1) && player == null)
        {
            sendErrorAndHelp(sender, "You have not provided a sufficient number of parameters.");
            return;
        }

        WorldServer world = null;

        // "set" command from player or console, world specified
        if (worldName != null)
        {
            if (params.size() == 2 && ! params.get(params.size() - 1).equalsIgnoreCase("spawn"))
            {	// command can only be this short if "spawn" is specified rather than x + z or player name
                sendErrorAndHelp(sender, "You have not provided a sufficient number of arguments.");
                return;
            }

            world = Worlds.getWorld(worldName);
            if (world == null)
            {
                if (params.get(params.size() - 1).equalsIgnoreCase("spawn"))
                {
                    sendErrorAndHelp(sender, "The world you specified (\"" + worldName + "\") could not be found on the server, so the spawn point cannot be determined.");
                    return;
                }
                Util.chat(sender, "The world you specified (\"" + worldName + "\") could not be found on the server, but data for it will be stored anyway.");
            }
        }
        // "set" command from player using current world since it isn't specified, or allowed from console only if player name is specified
        else
        {
            if (player == null)
            {
                if (! params.get(params.size() - 2).equalsIgnoreCase("player"))
                {	// command can only be called by console without world specified if player is specified instead
                    sendErrorAndHelp(sender, "You must specify a world name from console if not specifying a player name.");
                    return;
                }
                player = WorldBorder.SERVER.getPlayerList().getPlayerByUsername( params.get(params.size() - 1) );
                if (player == null)
                {
                    sendErrorAndHelp(sender, "The player you specified (\"" + params.get(params.size() - 1) + "\") does not appear to be online.");
                    return;
                }
            }
            worldName = Worlds.getWorldName(player.worldObj);
        }

        int radiusX, radiusZ;
        double x, z;
        int radiusCount = params.size();

        try
        {
            if (params.get(params.size() - 1).equalsIgnoreCase("spawn"))
            {	// "spawn" specified for x/z coordinates
                assert world != null;
                BlockPos loc = world.getSpawnPoint();
                x = loc.getX();
                z = loc.getZ();
                radiusCount -= 1;
            }
            else if (params.size() > 2 && params.get(params.size() - 2).equalsIgnoreCase("player"))
            {	// player name specified for x/z coordinates
                EntityPlayerMP playerT = WorldBorder.SERVER.getPlayerList().getPlayerByUsername(params.get(params.size() - 1));
                if (playerT == null)
                {
                    sendErrorAndHelp(sender, "The player you specified (\"" + params.get(params.size() - 1) + "\") does not appear to be online.");
                    return;
                }
                worldName = Worlds.getWorldName(playerT.worldObj);
                x = playerT.posX;
                z = playerT.posZ;
                radiusCount -= 2;
            }
            else
            {
                if (player == null || radiusCount > 2)
                {	// x and z specified
                    x = Double.parseDouble(params.get(params.size() - 2));
                    z = Double.parseDouble(params.get(params.size() - 1));
                    radiusCount -= 2;
                }
                else
                {	// using coordinates of command sender (player)
                    x = player.posX;
                    z = player.posZ;
                }
            }

            radiusX = Integer.parseInt(params.get(0));
            if (radiusCount < 2)
                radiusZ = radiusX;
            else
                radiusZ = Integer.parseInt(params.get(1));

            if (radiusX < Config.getKnockBack() || radiusZ < Config.getKnockBack())
            {
                sendErrorAndHelp(sender, "Radius value(s) must be more than the knockback distance.");
                return;
            }
        }
        catch(NumberFormatException ex)
        {
            sendErrorAndHelp(sender, "Radius value(s) must be integers and x and z values must be numerical.");
            return;
        }

        assert worldName != null;
        Config.setBorder(worldName, radiusX, radiusZ, x, z);
        Util.chat(sender, "Border has been set. " + Config.BorderDescription(worldName));
    }
}
