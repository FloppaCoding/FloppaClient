package floppaclient.module.impl.dungeon

import floppaclient.FloppaClient.Companion.inDungeons
import floppaclient.FloppaClient.Companion.mc
import floppaclient.events.EntityRemovedEvent
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.settings.impl.NumberSetting
import floppaclient.module.settings.impl.SelectorSetting
import floppaclient.module.settings.impl.StringSetting
import floppaclient.utils.Utils
import net.minecraft.entity.item.EntityItem
import net.minecraft.init.Blocks
import net.minecraft.tileentity.TileEntitySkull
import net.minecraftforge.client.event.sound.PlaySoundSourceEvent
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

/**
 * Module to play a sound when a secret is collected.
 * @author Aton
 */
object SecretChime : Module(
    "Secret Chime",
    category = Category.DUNGEON,
    description = "Plays a sound whenever you click a secret. Also plays this sound for aura clicks. \n" +
            "§4Do not use the bat deaht sound or your game will freeze!"
){

    private val sound: SelectorSetting
    private val customSound = StringSetting("Custom Sound", "mob.blaze.hit", description = "Name of a custom sound to play. This is used when Custom is selected in the Sound setting.")
    private val dropSound: SelectorSetting
    private val customDropSound = StringSetting("Custom Drop Sound", "mob.blaze.hit", description = "Name of a custom sound to play for item pickups. This is used when Custom is selected in the DropSound setting.")
    private val volume = NumberSetting("Volume", 1.0, 0.0, 1.0, 0.01, description = "Volume of the sound.")
    private val pitch = NumberSetting("Pitch", 2.0, 0.0, 2.0, 0.01, description = "Pitch of the sound.")

    private var lastPlayed = System.currentTimeMillis()

    private var drops = listOf(
        "Health Potion VIII Splash Potion", //"§5Health Potion VIII Splash Potion"
        "Decoy", //"§aDecoy"
        "Inflatable Jerry", //  "§fInflatable Jerry"
        "Spirit Leap", // "§9Spirit Leap"
        "Trap", // "§aTrap"
        "Training Weights", // "§aTraining Weights"
        "Defuse Kit", // "§aDefuse Kit"
        "Dungeon Chest Key", // "§9Dungeon Chest Key"
        "Treasure Talisman", // Name: "§9Treasure Talisman"
        "Revive Stone",
    )



/*
List of good sound effects:

fire.ignite - pitch 1
mob.blaze.hit - pitch 2
random.orb - pitch 1
random.break - 2
mob.guardian.land.hit - 2

 */


    init {
        val soundOptions = arrayListOf(
            "mob.blaze.hit",
            "fire.ignite",
            "random.orb",
            "random.break",
            "mob.guardian.land.hit",
            "Custom"
        )
        sound = SelectorSetting("Sound", "mob.blaze.hit", soundOptions, description = "Sound selection.")
        dropSound = SelectorSetting("Drop Sound", "mob.blaze.hit", soundOptions, description = "Sound selection for item pickups.")

        this.addSettings(
            sound,
            customSound,
            dropSound,
            customDropSound,
            volume,
            pitch
        )
    }

    /**
     * Registers right clicking a secret.
     */
    @SubscribeEvent
    fun onInteract(event: PlayerInteractEvent) {
        if ( !inDungeons) return
        val blockPos = event.pos
        try { // for some reason getBlockState can throw null pointer exception
        val block = mc.theWorld?.getBlockState(blockPos)?.block ?: return

        if (block == Blocks.chest || block == Blocks.lever ||
            block == Blocks.trapped_chest
        ){
            playSecretSound()
        }else if (block == Blocks.skull) {
            val tileEntity: TileEntitySkull = mc.theWorld.getTileEntity(blockPos) as TileEntitySkull
            if (tileEntity.playerProfile.id.toString() == "26bb1a8d-7c66-31c6-82d5-a9c04c94fb02") {
                playSecretSound()
            }
        }
        } catch (_: Exception) { }
    }

    /**
     * For bat death detection
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    fun onSoundPlay(event: PlaySoundSourceEvent) {
        if ( !inDungeons) return
        when(event.name) {
            "mob.bat.death"-> {
                playSecretSound()
            }
        }
    }

    /**
     * For item pickup detection. The forge event for item pickups cant be used, because item pickups are handled server side.
     */
    @SubscribeEvent
    fun onRemoveEntity(event: EntityRemovedEvent) {
        if(!inDungeons) return
        if(event.entity !is EntityItem) return
        if(mc.thePlayer.getDistanceToEntity(event.entity) > 6) return
        // Check the item name to filter for secrets.
        if (event.entity.entityItem.displayName.run isSecret@ {
            for(drop in drops){
                if (this.contains(drop)) return@isSecret true
            }
            return@isSecret false
        }) {
            playSecretSound(getSound(true))
        }
    }

    /**
     * Returns the sound from the selector setting, or the custom sound when the last element is selected
     */
    private fun getSound(isItemDrop: Boolean = false): String {
        return if(isItemDrop)
            if ( dropSound.index < sound.options.size - 1)
                dropSound.selected
            else
                customDropSound.text
        else if (sound.index < sound.options.size - 1)
            sound.selected
        else
            customSound.text
    }

    fun playSecretSound(sound: String = getSound()) {
        if (System.currentTimeMillis() - lastPlayed > 10) {
            Utils.playLoudSound(sound, volume.value.toFloat(), pitch.value.toFloat())
            lastPlayed = System.currentTimeMillis()
        }
    }
}