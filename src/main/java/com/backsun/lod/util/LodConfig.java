package com.backsun.lod.util;

import com.backsun.lod.util.enums.FogDistance;
import com.backsun.lod.util.enums.FogQuality;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 
 * @author James Seibel
 * @version 02-14-2021
 */
@Config(modid = Reference.MOD_ID)
public class LodConfig
{
	// save the config file when it is changed
	@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
	private static class EventHandler
	{
		@SubscribeEvent
		public static void onConfigChanged(final ConfigChangedEvent.OnConfigChangedEvent event)
		{
			if (event.getModID().equals(Reference.MOD_ID))
			{
				ConfigManager.sync(Reference.MOD_ID, Config.Type.INSTANCE);
			}
		}
	}
	
	
	@Config.Comment(
			{"Enable LODs", 
			"If true LODs will be drawn, if false LODs will "
			+ "still be generated and stored in your world's save folder, "
			+ "but they won't be rendered."})
	public static boolean drawLODs = true;
	
	@Config.Comment(
			{"Fog Distance", 
			"What distance should Fog be drawn on the LODs?"})
	public static FogDistance fogDistance = FogDistance.NEAR_AND_FAR;
	
	@Config.Comment(
			{"Use Optifine Fog Quality Setting", 
			"Should the LODs use Optifine's fog quality (Fast or Fancy) setting?"})
	public static boolean useOptifineFogQuality = true;
	
	@Config.Comment(
			{"Fog Quality Override", 
			"This is only used if \"Use Optifine FogQuality\" "
			+ "is set to false, or if Optifine can't be found."})
	public static FogQuality fogQualityOverride = FogQuality.FANCY;
	
	@Config.Comment(
			{"Draw Debugging Checkerboard", 
			"If false the LODs will draw with their normal world colors."
			+ "If true they will draw as a black and white checkerboard."
			+ "This can be used for debugging or imagining you are playing a "
			+ "giant game of chess ;)"})
	public static boolean drawCheckerBoard = false;
	
	
}

/*
class ExampleConfig
{
	@Config.Comment("This is an example boolean property.")
	public static boolean fooBar = false;
	
	@Config.Comment(
			{"This is an example enum property.", 
			"It will use a GuiConfigEntries.CycleValueEntry in the config GUI."})
	public static EnumExample exampleEnumProperty = EnumExample.VALUE_1;
	
	@Config.Comment({"This an example Map field.", "It will be converted to a category containing a property for each key-value pair."})
	public static final Map<String, Double> exampleMapField = new HashMap<>();
	
	static
	{
		exampleMapField.put("foobar", 2.0);
		exampleMapField.put("foobaz", 100.5);
		exampleMapField.put("barbaz", Double.MAX_VALUE);
	}
	
	public static final Client client = new Client();
	
	public enum EnumExample {
		VALUE_1,
		VALUE_2,
		VALUE_3,
		VALUE_4
	}
	
	public static class Client {
		
		@Config.Comment("This is an example int property.")
		public int baz = -100;
		
		@Config.Comment("This is an example enum property in a subcategory "
				+ "of the main category.")
		public EnumExample exampleSubcategoryEnumProperty = EnumExample.VALUE_3;
		
		@Config.Comment("This is an example enum property that "
				+ "uses an enum defined in a nested class.")
		public EnumExampleNested exampleNestedEnumProperty = EnumExampleNested.NESTED_2;
		
		
		public enum EnumExampleNested {
			NESTED_1,
			NESTED_2,
			NESTED_3,
			NESTED_4,
			NESTED_5
		}
	}
	
	
	
	
	
	@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
	private static class EventHandler
	{
		
		
		// Inject the new values and save 
		// to the config file when the 
		// config has been changed from the GUI.
		@SubscribeEvent
		public static void onConfigChanged(final ConfigChangedEvent.OnConfigChangedEvent event) {
			if (event.getModID().equals(Reference.MOD_ID))
			{
				ConfigManager.sync(Reference.MOD_ID, Config.Type.INSTANCE);
			}
		}
	}
	
}
*/
