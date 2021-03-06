package vexatos.conventional.command;

import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import org.apache.commons.lang3.tuple.Pair;
import vexatos.conventional.Conventional;
import vexatos.conventional.reference.Config;
import vexatos.conventional.util.RayTracer;

import java.util.List;

/**
 * @author Vexatos
 */
public class CommandRemoveBlock extends SubCommand {

	public CommandRemoveBlock() {
		super("block");
	}

	@Override
	public void processCommand(ICommandSender sender, String[] args) {
		if(!(sender instanceof EntityPlayerMP)) {
			throw new WrongUsageException("cannot process unless called from a player on the server side");
		}
		if(args.length < 1 || (!args[0].equalsIgnoreCase("right") && !args[0].equalsIgnoreCase("left") && !args[0].equalsIgnoreCase("break"))) {
			throw new WrongUsageException("third argument needs to be 'left' or 'right' or 'break'.");
		}
		Config.BlockList list = args[0].equalsIgnoreCase("right") ?
			Conventional.config.blocksAllowRightclick : args[0].equalsIgnoreCase("left") ?
			Conventional.config.blocksAllowLeftclick : Conventional.config.blocksAllowBreak;
		EntityPlayerMP player = (EntityPlayerMP) sender;
		RayTracer.instance().fire(player, 10);
		MovingObjectPosition mop = RayTracer.instance().getTarget();
		if(mop.typeOfHit != MovingObjectType.BLOCK) {
			throw new WrongUsageException("the player is not looking at any block");
		}
		Block block = player.worldObj.getBlock(mop.blockX, mop.blockY, mop.blockZ);
		if(!block.isAir(player.worldObj, mop.blockX, mop.blockY, mop.blockZ)) {
			GameRegistry.UniqueIdentifier uid = GameRegistry.findUniqueIdentifierFor(block);
			if(uid == null) {
				throw new WrongUsageException("unable to find identifier for block: " + block.getUnlocalizedName());
			}
			Pair<Block, Integer> pair = Pair.of(block, (args.length >= 2 && args[1].equalsIgnoreCase("ignore")) ? -1 : player.worldObj.getBlockMetadata(mop.blockX, mop.blockY, mop.blockZ));
			if(!list.contains(pair)) {
				throw new WrongUsageException("Block is not in the whitelist.");
			}
			list.remove(pair);
			sender.addChatMessage(new ChatComponentText(String.format("Block '%s' removed!", uid.toString())));
			Conventional.config.save();
		}
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return "/cv remove block <left|right|break> [ignore] - removes the block you are currently looking at. 'ignore' makes it search for an entry that ignores metadata.";
	}

	@Override
	public List addTabCompletionOptions(ICommandSender sender, String[] args) {
		if(args.length <= 1) {
			return CommandBase.getListOfStringsMatchingLastWord(args, "left", "right", "break");
		} else if(args.length == 2) {
			return CommandBase.getListOfStringsMatchingLastWord(args, "ignore");
		}
		return super.addTabCompletionOptions(sender, args);
	}
}
