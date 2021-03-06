package vexatos.conventional.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.util.ChatComponentText;
import vexatos.conventional.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Vexatos
 */
public class ConventionalCommand extends SubCommand {

	protected List<SubCommand> commands = new ArrayList<SubCommand>();

	public ConventionalCommand(String name) {
		super(name);
	}

	public void addCommand(SubCommand cmd) {
		commands.add(cmd);
	}

	@Override
	public String getUsage(ICommandSender sender) {
		String text = "";
		for(SubCommand cmd : commands) {
			text += (cmd.getUsage(sender) + "\n");
		}
		return text.replaceAll("\\n$", "");
	}

	@Override
	public void processCommand(ICommandSender sender, String[] args) {
		if(args.length < 1) {
			throw new WrongUsageException(getCommandUsage(sender));
		}
		if(args[0].equalsIgnoreCase("help")) {
			String[] usage = getUsage(sender).split("\n");
			for(String s : usage) {
				sender.addChatMessage(new ChatComponentText(" - " + s));
			}
			return;
		}
		for(SubCommand cmd : commands) {
			if(cmd.getCommandName().equalsIgnoreCase(args[0])) {
				cmd.processCommand(sender, StringUtil.dropArgs(args, 1));
				return;
			}
		}
		throw new WrongUsageException(getCommandUsage(sender));
	}

	@Override
	public List addTabCompletionOptions(ICommandSender sender, String[] args) {
		if(args.length <= 1) {
			List<String> words = new ArrayList<String>();
			for(SubCommand cmd : commands) {
				words.add(cmd.getCommandName());
			}
			return CommandBase.getListOfStringsMatchingLastWord(args, words.toArray(new String[words.size()]));
		} else {
			String cmdname = args[0];
			for(SubCommand cmd : commands) {
				if(cmd.getCommandName().equalsIgnoreCase(cmdname)) {
					return cmd.addTabCompletionOptions(sender, StringUtil.dropArgs(args, 1));
				}
			}
		}
		return super.addTabCompletionOptions(sender, args);
	}
}
