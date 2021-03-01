This program is an attempt to implement a Level Of Detail system for Minecraft.
With the purpose of increasing the maximum view distance in game without harming performance.

Forge version: 1.12.2-14.23.5.2847

Notes:
This version will run in eclipse perfectly fine, but will not work correctly 
in retail Minecraft. Specifically the core mod doesn't work in retail minecraft,
which means that LODs will be drawn on top of normal terrain.
This version also doesn't work with optifine at all, since it breaks the core mod.


Used in congunction with:
https://gitlab.com/jeseibel/minecraft-lod-core-mod


========================
source code installation
========================

See the Forge Documentation online for more detailed instructions:
http://mcforge.readthedocs.io/en/latest/gettingstarted/

Step 1: open a command line in the project folder

Step 2: run the command: "./gradlew setupDecompWorkspace"

Step 3: run the command: "./gradlew eclipse"

Step 4: Import project

Step 5: Create a system variable called "JAVA_MC_HOME" with the location of the JDK 1.8.0_251 (This is needed for gradle to work correctly)
		And make sure it is used in the gradle.bat file.

Step 6: Import the lodcore and lodcore_source jar files into the referenced libraries.

Step 7: Make sure the eclipse has the JDK 1.8.0_251 installed. (This is needed so that eclipse can run minecraft)


Other commands: 
	"gradlew --refresh-dependencies" to refresh local dependencies. 
	"gradlew clean" to reset everything (this does not affect your code) and then start the process again.



Tip:
	The Minecraft source code is NOT added to your workspace in a editable way. Minecraft is treated like a normal Library. Sources are there for documentation and research purposes only.

	Current location of mcp-srg.srg:
	"C:/Users/James Seibel/.gradle/caches/minecraft/de/oceanlabs/mcp/mcp_snapshot/20171003/1.12.2/srgs/"
