# 1.17.1-0.6.0-alpha.7
Its a bird, its a plane, its a, rock?

# Turbine
 - Rotor is rendered using Quartz render system
 - Fix validation

# Misc
 - Fix recipes that use copper

# 1.17.1-0.6.0-alpha.6

# Reactor
 - Fix NPEs caused by new simulation interface
 - Implement simulation description serialization
 - Add version to CC API
 - Temperatures are in Kelvin now
 - Remove range limit for max size

# Turbine
 - Add version to CC API
 - Remove range limit for max size

# Heat Exchanger
 - Add version to CC API
 - Add config options for max size

# 1.17.1-0.6.0-alpha.5
Not your grandpa's reactors anymore

# Reactor
 - Remove classic simulation
 - correctly write to rfTransferredLastTick
 - switch to new internal simulation interface
   - allows simulation to be loaded independent of Forge/Minecraft
 - ensure EMTPY_MODERATOR isn't going to cause divide by zero errors

# Turbine
 - Remove classic simulation

# Misc
 - Move config to new object based system

# 1.17.1-0.6.0-alpha.4
I'm a strong independent mod that doesn't need any other repository to build me 

# Misc
 - Built directly from BiggerReactors repo, w/ Phosphophyllite submodule
 - Oregen fixed (mostly in Phos)
 - Config format changed to TOML by default, will still read/use json5 for existing configs

# 1.17.1-0.6.0-alpha.3.1
update forge to 37.0.53

# Reactor
 - improve chunk unload time

# 1.17.1-0.6.0-alpha.3
*I AM, **S P E E D***

~~except on unload, that needs work~~

## Reactor
 - Assembly tick optimizations, 50% magic
 - Delegate to correct capability super method

## Turbine
 - Assembly tick optimizations
 - Delegate to correct capability super method
 - Query for ASSEMBLED blocks state existence before value

## Heat Exchanger
 - Delegate to correct capability super method
 - Query for ASSEMBLED blocks state existence before value

## Misc
 - Switch to parchment mappings

# 1.17.1-0.6.0-alpha.2
ComputerCraft

## Reactor
 - Re-enable computer port
 - fix possible deadlock state and race condition

## Turbine
 - Re-enable computer port
 - fix possible deadlock state and race condition

## Heat Exchanger
 - Re-enable computer port
 - Fix stack overflow with coolant port
 
# 1.17.1-0.6.0-alpha.1
Modular multi-blocks

## Turbine
 - Fix stack overflow with rotor bearing
 - Fix maybe rendering 

## Misc
 - update to Phos 0.6.0-alpha.1, and its multiblock changes


# 1.17.1-0.6.0-alpha
1.17.1

# 1.16.5-0.5.0
That's all folks!

Next stop, 1.17!

## Reactor
 - Add experimental reactor simulation
   - cripples servers on all cores, not recommended for normal use
 - GUI displays fuel heat instead of casing heat
 - Sync CC functions properly
 - Increase internal battery size

## Misc
 - Animated terminal textures (thx Ridanisaurus)
 - All parts of a tooltip will give their denomination
 - Reduce tile update size, avoids chunkbaning when building large reactors/etc

# 1.16.5-0.5.0-beta.3

## Reactor
 - fix manifold recipe
 - Add allthemodium/vibranium/unobtainium fluid/vapor moderator support
 - Add moderator values for superheated sodium, lava, and liquid obsidian
 - Add fluid support to moderator registry
 - Have manifold only consider cold side for dynamic moderator properties

## Heat Exchanger
 - Use correct tank sizes in GUI

## Misc
 - Add allthemodium/vibranium/unobtainium fluid/vapor transitions

# 1.16.5-0.5.0-beta.2

## Reactor
 - Fix negative power bug
 - Keep CC modem connected on reload
 - fix controlRodCount CC function

## Turbine
 - Fix negative power bug
 - fix CC battery function
 - fix CC vent controls
 - fix CC coil controls

# 1.16.5-0.5.0-beta.1
99 little bugs in the code, 99 little bugs. \
Take one down, patch it around, 127 little bugs in the code...

## Reactor
 - Fix CC battery function
 - Remove extra underscore from CC peripheral name
 - Add manifold crafting recipe

## Turbine
 - Fixed NPE crash
 - Fixed displaying NaN efficiency in some cases
 - Remove extra underscore from CC peripheral name

## Heat Exchanger
 - Updated the GUI to better reflect internal workings
 - Remove extra underscore from CC peripheral name

## Misc
 - Correct itemgroup key in lang file

# 1.16.5-0.5.0-beta
Still april fools, right? no? That was last week? *well fuck.*

## Reactor
 - FissionEventsPerFuelUnit increased by 10x for modern simulation
 - Tank size increased by 100x for modern simulation
 - Mekanism gas handling re-dded, handles all gases with valid fluid, not just steam
 - Manifold model/texture added, modified/corrected assembly behavior
 - GUIs switch on the fly, no longer requiring re-opening to switch type
 - New OOP CC API to match new simulation, works for old one as well (subject to race conditions)

## Heat Exchanger
 - Increase tank size
 - Increase internal surface area
 - Increase conductivities
 - Increase channel heat capacity
 - Terminal added (GUI still needs work)
 - Computer port added, CC API that matches new OOP based ones for reactor/turbine
 - Manifolds add to tank capacity
 - Allow power to be pulled from power taps
 - New fuel rendering textures (thx Ridanisaurus)
 - Errors in lang file

## Turbine
 - New modern simulation
    - Drag is squared, not linear/fixed
    - Rotor speed responds quicker
    - Rotor speed not coupled to blade area directly
    - Rotor fluid capacity tied to rotor RPM, faster rotor more capacity
 - Increase steam capacity per blade by 10x for modern simulation
   - Also increase coil drag and rotor mass by 10x, so, turbines are 10x denser now
 - Support for multiple coil layers (exact behavior may change)
 - New OOP CC API to match new simulation, works for old one as well (subject to race conditions)
 - Selective transition selection, so only steam, other stuff in the heat exchanger
 - State saves
 - Allow power to be pulled from power taps
 - GUI displays correct fluid being used

## Misc
 - GUIs retextured (thx Ridanisaurus)
 - Bucket textures added (#2) (thx Ridanisaurus)
 - Textures rearranged, texture packs may need to update
 - Yellorium renamed to Uranium, lang file only, ID kept the same
 - Uranium bucket no longer registered
 - Add superheated_sodium fluid for mekanism sodium compatibility

# 1.16.5-0.5.0-alpha.1
The steam is lava, the steam is lava

## Reactor
 - GUI displays correct fluid
 - manifolds added (no model/texture yet)
 - config option to switch between simulations added
 - modern simulation active cooling mode fixed
 - modern simulation saves its data
 - power extraction draws from internal energy correctly
 - fix fuel level rendering

## Turbine
 - uses transition registry for fluids
 - simulation decoupled from multiblock controller 
 - fix rotor renderer

## Misc
 - correct handling of transitions with multiple inlet fluids

# 1.16.5-0.5.0-alpha.0
Boldly go where no reactor has gone before

## Reactor
 - capability to boil fluids other than water
 - new simulation with radiation moving in all directions, identical mechanics
 - new simulation corrects heat transfer rates

## Heat Exchanger
 - added

## Misc
 - new textures all around (thx Ridanisaurus)
 - added fluid obsidian (mostly for testing, may end up staying)

# 1.16.4-0.4.3

## Turbine
 - correct tank size and flow rate calculations

## misc
 - move JEI integration to remove hard dependency

# 1.16.4-0.4.3-beta
I tip my hat to you, one legend to another

## Reactor
 - Fix NPE with access port

## Turbine
 - power port reports power 

# 1.16.4-0.4.2
I rate this bug, 0/0

## Reactor
 - Fix CC Nan issue, #50

# 1.16.4-0.4.1

## Turbine
 - fix GUIs

# 1.16.4-0.4.0
hotter, bigger, faster, stronger, *wait, that doesn't sound quite right*

## Reactor
 - default size limit increased to 128x128x192
 - fixed bug with control rod heat transfer
 - corrected fuel heat display
 - added moderator registry JEI integration (thx MutantGumdrop)
 - fix rare NPE with coolant port
 - correct enderium registration
 - send blockupdate in front of the redstone port to signal torches
 - access port manual ejection works
 - missing CC API functions added
 - moderators added, check the list

## Turbine
 - added coi registry JEI integration (thx MutantGumdrop)
 - missing CC API functions added
 - flow rate/tank size now scales with physical turbine size, not blade count

# Cyanite Reprocessor
 - fix voiding issues

## Misc
 - config options flagged as advanced
 - fixed GUI update bug
 - CSV and wikimedia moderator/coil exports added to github actions (thx MutantGumdrop)

# 1.16.4-0.3.0
you spin me right round baby

## Reactor
 - new GUIs
 - control rods can be named now
 - give correct y level for fuel rod gaps
 - some more moderators
 - yellorium tag accepted for fuel
 - errors with both coolant port and power tap
 - moderator registry moved to datapack
 - fix some redstone port state saving bugs
 - minimize fuel rod updates at assembly
 - correct getEnergyStoredUnscaled CC API name

## Turbine
 - new GUIs
 - rotor animation, it exists now
 - coil registry moved to datapack
 - fix near edge rotors confusing casing/glass as coil
 - add rotor_bearing_count error to lang file

## Misc
 - extranious blockstates reduced
 - tag yellorite ore
 - reprocessor also has a new GUI
 - fix reprocessor voiding blutonium when output slot is full 
 - LGPL v2.1 now
 - 1.16.4

# 1.16.3-0.2.6
1/0 bugs

## Reactor
 - fix divide by zero bug with fuel rendering

# 1.16.3-0.2.5
Redstone port bugs

## Reactor
 - fix active reactor overheating when output multiplier was > 1, and it had coolant
 - allow power to be extracted from the power tap, not just pushed out
 - fuel can now be extracted from a reactor 
 - add unscaled methods to CC peripheral
 - fix redstone port energy percent reporting incorrectly
 - fix redstone port not recommitting values after a world reload
 - added mode switch button for control rod insertion
 - fix redstone port powering themselves through the reactor
 - optimize fuel rendering updates

# 1.16.3-0.2.4
Blocks and bugs

## Reactor
 - accepts uranium and blutonium blocks as fuel
 - renders fuel, shows to accuracy of 1/16th of a block
 - Dynamic battery size, default 10k RF per external block
 - Steam tank size limit removed
 
## Turbine
 - Dynamic flow rate/tank size, based on blade count
 - Dynamic battery size default 30k * (coil block count + 1)
 - increased default max turbine length to 192
 - Allthemodium, Vibranium, and Unobtanium added to possible coils

## Misc
 - storage block items are tagged now, should work with other recipes and mods correctly
 - Fix CC API backwards compatibility with new battery sizes, scales it to the old sizes

# 1.16.3-0.2.3
Connecting, textures

## Reactor
 - Connecting, textures, for the glass
 
## Turbine
 - Connecting, textures, for the glass
 - fix dangling coil bug
 - add errors to lang file

## Misc
 - More ER2 compatibility
 - tag yellorite ore item as uranium

# 1.16.3-0.2.2
Bug fixes! *again*
And compat

## Reactor
 - Fix redstone port recipe

## Turbine
 - Fix a new turbine not assembling
 
## Misc
 - Compatibility with ER2, tagging for items/blocks and recipes where applicable

# 1.16.3-0.2.1
Bug fixing

## Reactor
 - Fix NPE with Mekanism gas handler
 - Allow access ports to be crafted with any wooden chest

## Turbine
 - Fix NPE with Mekanism gas handler

# 1.16.3-0.2.0
Redstone Port

## Reactor
 - Added redstone port
 - Added air types to default moderator list
 - Increase default maximum size to 64x64x96
 - Increase config maximum size limit to 192x192x256
 - Computer Port drop added
 - Change transparent blocks sound type to glass, mostly for the glass
 - Fix overproduction bug with a production multiplier in wrong place, added more config options to allow tuning of both passive and active output separately
 
## Turbine
 - Increase default maximum size to 64x64x96
 - Increase config maximum size limit to 192x192x256
 - Computer Port drop added
 - Input/Exhaust GUI icons corrected
 
## Misc
 - Bucket added for irradiated steam
 - Yellorium fluid shows up in JEI now, it was transparent

# 1.16.3-0.1.9
CC Bug fix

## Reactor
 - Clamp control rod insertion with CC API

# 1.16.3-0.1.8
ComputerCraft API

## Reactor
 - Added ability to get the maximum amount of fluid that *could* have been produced this tick
 - Added ability to query battery size, as it is now configurable
 - Ported ability to query active reactor tank size, missed in first round
 
## Turbine
 - Added ability to query batter size, as it is now configurable

## Misc
 - update to zh_cn translation

# 1.16.3-0.1.7
more bug fixes

## Reactor
 - Fix loading issue due to mekanism steam compatability
 - Fix crash when using gui to toggle directions
 - Fix dupe glitch with mekanism pipes, also causes log spawm

## Misc
 - Uranium to cyanite actually fixed now

# 1.16.3-0.1.6
bug fixes

## Reactor
 - GUIs added for ports, only allow toggle
 - OutputMultiplier fixed for active reactors
 - Control rod levels apply after reload

## Turbine
 - GUIs added for coolant port, only allows toggle
 - Default turbine max flow rate changed to 5,000mB/t, and tank size to 10,000mB
 - Internal battery size adjusted by 2.5x to match above change

## Misc
 - fixed cyanite reprocessor voiding items
 - corrected the uranium to cyanite recipe

# 1.16.3-0.1.5
more tagging, fix for ATM6 dust smelting

## Misc
 - add ingots to forge:ingots
 - add dusts to forge:dusts
 - add yellorium_dust to forge:dusts/uranium_dust

# 1.16.3-0.1.4
tagging

## Reactor
 - Reactor accepts any forge:ingots/uranium
 - Glass reports errors correctly
 - Reactor will save generated power through reload
 - Added more moderators

## Turbine
 - Inductor drag config option fixed
 - Computerport will properly set vent all
 - Added inductors

## Misc
 - Cyanite reprocessor works with pipes properly
 - Yellorite is tagged as forge:ores/uranium, and forge:ores
 - AE2 grindstone recipes added
 - zh_cn translation added (thanks qsefthuopq)

# 1.16.3-0.1.3
Mekanism compatibility

## Reactor
 - Reactor can now produce Mekanism gas steam, just connect a Mekanism gas pipe to the coolant port

## Turbine
 - Turbine can now accept Mekanism gas steam, just connect a Mekanism gas pipe to the coolant port.
 - Computer Craft API bug fixed. Steam and water were being reported as each other.

## Misc
 - Mekanism Enrichment Chamber and Crusher compatibility added. Pretty much the same as the Pulverizer.
 - Computer ports have names in the lang file now.

# 1.16.3-0.1.2
wrench compat

## Reactor
 - Fixed wrench compat with mekanism
 - Control rods save their insertion

## Turbine
 - Fixed wrench compat with mekanism

# 1.16.3-0.1.1
computer ports and rotor rotation

## Reactor
 - Computer port implemented
## Turbine
 - Computer port implemented
 - Rotor will now rotate to the correct axis
 
## Misc
 - Added config option to disable oregen

# 1.16.3-0.1.0
fist 1.16 release, minor changes

## Reactor
 - Control rod has a GUI now
## Turbine
 - Flow rate change buttons work properly (bug discovered because of control rod)
## Misc
 - Yellorium bucket named fixed
 - Phosphophyllite separated into different jar, required for BiR (see Phosphophyllite changelog for details)

# 1.15.2-0.0.1
bug fix

## Reactor
 - blocks registered with tags not added to registry correct (thermal blocks)

# 1.15.2-0.0.0
initial version, changes from BR 0.4.3A

## Reactor
 - Access port must be pushed into, and does not have a gui nor internal buffer
 - Terminal GUI works a bit different, but not majorly, also called terminal now, not controller
 - Control rod does not have a GUI (yet)
 - Computer port does not exist (yet)

## Turbine
 - Terminal GUI works a bit different, but not majorly, also called terminal now, not controller
 - Turbine requires two bearings, one on each end
 - Computer port does not exist (yet)
 - You can't over speed turbines anymore, there is a soft limit to the speed, so long as you were under 2000 rpm it's the same
     - Small sub-note there, i did modify the efficiency function in the 450-500 RPM range to have a better transition, shouldn't really matter though
 - You now need to rotor bearings, one at each end, orientation of rotor doesn't matter
 
## Misc
 - There is a pile of stuff in the config now, sooo many internal values that were magic before
 

 
#### Beginning of time?
