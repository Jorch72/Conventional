package vexatos.conventional.command;

import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import vexatos.conventional.Conventional;

/**
 * @author Vexatos
 */
public class CommandReload extends SubCommand {

	public CommandReload() {
		super("reload");
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return "/cv reload - reloads the whitelists";
	}

	@Override
	public void processCommand(ICommandSender sender, String[] args) {
		Conventional.config.reload();
		Conventional.config.save();
		sender.addChatMessage(new ChatComponentText("Whitelists reloaded!"));
	}
}
