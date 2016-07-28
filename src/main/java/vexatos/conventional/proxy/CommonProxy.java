package vexatos.conventional.proxy;

import net.minecraft.network.INetHandler;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import vexatos.conventional.Conventional;
import vexatos.conventional.network.MessageHandlerBase;
import vexatos.conventional.network.Packet;

import javax.annotation.Nullable;
import java.io.File;

public class CommonProxy {

	public boolean isClient() {
		return false;
	}

	public File getMinecraftDirectory() {
		return new File(".");
	}

	@Nullable
	public World getWorld(int dimensionId) {
		return FMLCommonHandler.instance().getMinecraftServerInstance().worldServerForDimension(dimensionId);
	}

	public int getCurrentClientDimension() {
		return -9001;
	}

	public void handlePacket(@Nullable MessageHandlerBase client, @Nullable MessageHandlerBase server, Packet packet, INetHandler handler) {
		try {
			if(server != null) {
				server.onMessage(packet, handler, ((NetHandlerPlayServer) handler).playerEntity);
			}
		} catch(Exception e) {
			Conventional.log.warn("Caught a network exception! Is someone sending malformed packets?");
			e.printStackTrace();
		}
	}
}
