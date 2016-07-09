package vexatos.conventional;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.WorldSettings.GameType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.BreakSpeed;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent.BreakEvent;
import net.minecraftforge.event.world.BlockEvent.PlaceEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import net.minecraftforge.fml.common.eventhandler.Event.Result;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.logging.log4j.Logger;
import vexatos.conventional.command.CommandAddBlock;
import vexatos.conventional.command.CommandAddEntity;
import vexatos.conventional.command.CommandAddItem;
import vexatos.conventional.command.CommandArea;
import vexatos.conventional.command.CommandExclude;
import vexatos.conventional.command.CommandList;
import vexatos.conventional.command.CommandReload;
import vexatos.conventional.command.CommandRemoveBlock;
import vexatos.conventional.command.CommandRemoveEntity;
import vexatos.conventional.command.CommandRemoveItem;
import vexatos.conventional.command.ConventionalCommand;
import vexatos.conventional.command.SubCommand;
import vexatos.conventional.command.area.CommandAreaCreate;
import vexatos.conventional.command.area.CommandAreaRemove;
import vexatos.conventional.command.area.CommandPosition;
import vexatos.conventional.integration.chiselsandbits.ChiselsBitsHandler;
import vexatos.conventional.reference.Config;
import vexatos.conventional.reference.Mods;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Mod(modid = Mods.Conventional, name = Mods.Conventional, version = "@VERSION@", acceptableRemoteVersions = "*", dependencies = "after:" + Mods.ChiselsBits + "@[10.9,)")
public class Conventional {

	@Instance
	public static Conventional instance;

	public static Config config;
	public static Logger log;
	public static File configDir;

	public static List<Function<Config.Area, SubCommand>> areaCommands = new ArrayList<>();

	@EventHandler
	public void preInit(FMLPreInitializationEvent e) {
		log = e.getModLog();
		configDir = e.getModConfigurationDirectory();
		MinecraftForge.EVENT_BUS.register(this);
		if(Mods.isLoaded(Mods.ChiselsBits)) {
			MinecraftForge.EVENT_BUS.register(new ChiselsBitsHandler());
		}
		//FMLCommonHandler.instance().bus().register(this);
		//config = new Config(new Configuration(e.getSuggestedConfigurationFile()));
		config = new Config();
		config.reload();

		areaCommands.add(a -> new CommandList(() -> a));
		areaCommands.add(a -> {
			ConventionalCommand addCmd = new ConventionalCommand("add");
			addCmd.addCommand(new CommandAddBlock(() -> a));
			addCmd.addCommand(new CommandAddItem(() -> a));
			addCmd.addCommand(new CommandAddEntity(() -> a));
			return addCmd;
		});
		areaCommands.add(a -> {
			ConventionalCommand rmvCmd = new ConventionalCommand("remove");
			rmvCmd.addCommand(new CommandRemoveBlock(() -> a));
			rmvCmd.addCommand(new CommandRemoveItem(() -> a));
			rmvCmd.addCommand(new CommandRemoveEntity(() -> a));
			return rmvCmd;
		});
	}

	@EventHandler
	public void postInit(FMLPostInitializationEvent e) {
		config.save();
	}

	@EventHandler
	public void onServerStart(FMLServerAboutToStartEvent e) {
		config.reload();
	}

	@EventHandler
	public void onServerStarting(FMLServerStartingEvent e) {
		ConventionalCommand cmd = new ConventionalCommand("cv");
		cmd.addCommand(new CommandReload());
		cmd.addCommand(new CommandList(() -> config.ALL));
		cmd.addCommand(new CommandExclude());
		ConventionalCommand addCmd = new ConventionalCommand("add");
		addCmd.addCommand(new CommandAddBlock(() -> config.ALL));
		addCmd.addCommand(new CommandAddItem(() -> config.ALL));
		addCmd.addCommand(new CommandAddEntity(() -> config.ALL));
		cmd.addCommand(addCmd);
		CommandArea areaCmd = new CommandArea();
		areaCmd.addCommand(new CommandAreaCreate(areaCmd));
		areaCmd.addCommand(new CommandAreaRemove());
		cmd.addCommand(areaCmd);
		cmd.addCommand(new CommandPosition());
		ConventionalCommand rmvCmd = new ConventionalCommand("remove");
		rmvCmd.addCommand(new CommandRemoveBlock(() -> config.ALL));
		rmvCmd.addCommand(new CommandRemoveItem(() -> config.ALL));
		rmvCmd.addCommand(new CommandRemoveEntity(() -> config.ALL));
		cmd.addCommand(rmvCmd);
		e.registerServerCommand(cmd);
	}

	@EventHandler
	public void onServerStopping(FMLServerStoppingEvent e) {
		config.save();
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onPlace(PlaceEvent event) {
		if(isAdventureMode(event.getPlayer()) && !config.mayRightclick(event.getPlayer(), event.getItemInHand())) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onBreakSpeed(BreakSpeed event) {
		if(isAdventureMode(event.getEntityPlayer()) && !config.mayBreak(event.getEntityPlayer().worldObj, event.getPos())) {
			//event.setCanceled(true);
			event.setNewSpeed(Float.MIN_VALUE);
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onBreak(BreakEvent event) {
		if(isAdventureMode(event.getPlayer()) && !config.mayBreak(event.getWorld(), event.getPos())) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onInteract(PlayerInteractEvent event) {
		if(event.isCanceled()) {
			return;
		}
		if(isAdventureMode(event.getEntityPlayer())) {
			if(event instanceof PlayerInteractEvent.LeftClickBlock) {
				if(!config.mayLeftclick(event.getWorld(), event.getPos())) {
					event.setCanceled(true);
				}
			} else if(event instanceof PlayerInteractEvent.RightClickBlock) {
				final PlayerInteractEvent.RightClickBlock rcevent = (PlayerInteractEvent.RightClickBlock) event;
				final boolean
					validBlock = config.mayRightclick(rcevent.getWorld(), rcevent.getPos()),
					validItem = config.mayRightclick(event.getEntityPlayer(), rcevent.getItemStack());
				if(validBlock && validItem) {
					// Just return.
				} else if(!validBlock && !validItem) {
					rcevent.setCanceled(true);
				} else if(validBlock) {
					rcevent.setUseBlock(Result.ALLOW);
					rcevent.setUseItem(Result.DENY);
				} else {
					rcevent.setUseBlock(Result.DENY);
					rcevent.setUseItem(Result.ALLOW);
				}
			} else if(event instanceof PlayerInteractEvent.EntityInteract) {
				if(isAdventureMode(event.getEntityPlayer()) && !config.mayRightclick(((PlayerInteractEvent.EntityInteract) event).getTarget())) {
					event.setCanceled(true);
				}
			} else if(event instanceof PlayerInteractEvent.EntityInteractSpecific) {
				if(isAdventureMode(event.getEntityPlayer()) && !config.mayRightclick(((PlayerInteractEvent.EntityInteractSpecific) event).getTarget())) {
					event.setCanceled(true);
				}
			} else if(!config.mayRightclick(event.getEntityPlayer(), event.getItemStack())) {
				event.setCanceled(true);
			}
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onEntityLeftclick(AttackEntityEvent event) {
		if(isAdventureMode(event.getEntityPlayer()) && !config.mayLeftclick(event.getTarget())) {
			event.setCanceled(true);
		}
	}

	// checks for serverside and adventure mode
	public static boolean isAdventureMode(EntityPlayer player) {
		/*if(player.worldObj.isRemote) {
			return isAdventureMode_Client(player);
		}*/
		//return !player.worldObj.isRemote && ((EntityPlayerMP) player).theItemInWorldManager.getGameType().isAdventure();
		return !(player instanceof FakePlayer)
			&& !player.worldObj.isRemote
			&& ((EntityPlayerMP) player).interactionManager.getGameType() != GameType.CREATIVE
			&& !config.isExcluded(player.getName())
			/* && !player.canCommandSenderUseCommand(2, "cv")*/;
	}

	/*private boolean isAdventureMode_Client(EntityPlayer player) {
		return Minecraft.getMinecraft().playerController.isNotCreative() && !player.canCommandSenderUseCommand(2, "cv");
	}*/
	/*@SubscribeEvent
	public void logEvent(Event e) {
		String s = e.toString();
		if(s.contains("client") || e.toString().contains("Render") || s.contains("Update") || s.contains("Tick") || s.contains("Chunk") || s.contains("EntityConstructing")
			|| e instanceof InputEvent || s.contains("Spawn")) {
			return;
		}
		if(s.contains("Attack") || s.contains("Interact"))
			log.info(Thread.currentThread().getName() + ": " + e.toString());
	}*/
}
