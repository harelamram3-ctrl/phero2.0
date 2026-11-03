GauntletSockets 1.0.0
====================

Adds a six-socket GUI to the Infinity Gauntlet created by SuperHeroes 3.1.1.
Designed for Paper 1.21.11 and Java 21.

INSTALLATION
1. Keep SuperHeroes-3.1.1_v4.jar and InfinityStones-2.0.0.jar installed.
2. Build this project with Maven: mvn clean package
3. Copy target/GauntletSockets-1.0.0.jar into the server's plugins folder.
4. Restart the server (do not use /reload).
5. Hold the Infinity Gauntlet and right-click to open its GUI.

USAGE
- Click an Infinity Stone in your player inventory to insert it into the gauntlet.
- Left-click an inserted stone in the GUI to activate its power.
- Right-click an inserted stone in the GUI to remove it.
- When all six stones are inserted, click the Nether Star in the middle to activate Snap.
- The six socket contents are stored directly on the gauntlet item and persist after restart/trading.

DEPENDENCIES
- Paper 1.21.11
- Java 21
- SuperHeroes 3.1.1
- InfinityStones 2.0.0

NOTE
This is an add-on plugin, not a replacement for either original plugin. It connects to
InfinityStones' existing stoneItem/use/snap methods. If those plugins are updated and their
internal API changes, this add-on may need an update too.
