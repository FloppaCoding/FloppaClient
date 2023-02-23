package floppaclient.module.impl.render

import floppaclient.FloppaClient
import floppaclient.FloppaClient.Companion.mc
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.settings.impl.BooleanSetting
import floppaclient.module.settings.impl.ColorSetting
import floppaclient.module.settings.impl.NumberSetting
import floppaclient.module.settings.impl.SelectorSetting
import floppaclient.utils.render.WorldRenderUtils.drawBoxByEntity
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.entity.Entity
import net.minecraft.entity.boss.EntityWither
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.monster.EntityEnderman
import net.minecraft.entity.passive.EntityBat
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.StringUtils
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color

/**
 * Show the position of dungoen mobs through walls.
 * @author Aton
 */
object DungeonESP : Module(
    "Dungeon ESP",
    category = Category.RENDER,
    description = "Draws an esp around dungeon mobs."
){
    private val style = SelectorSetting("Style", "Outline", arrayListOf("Outline", "Filled"), description = "Style of the ESP box")

    private val boxWidth = NumberSetting("Box Width",0.9,0.1,2.0,0.05, description = "Width of the esp box in units of blocks.")
    private val deaultLineWidth = NumberSetting("Default LW",1.0,0.1,10.0,0.1, description = "Default line width of the esp box.")
    private val specialLineWidth = NumberSetting("Special Mob LW",2.0,0.1,10.0,0.1, description = "Line width of the esp box for special mobs like Fel and Withermancer.")
    private val miniLineWidth = NumberSetting("Mini Boss LW",3.0,0.1,10.0,0.1, description = "Line width of the esp box for Mini Bosses.")
    private val opacity = NumberSetting("Opacity", 1.0, 0.05, 1.0, 0.05, description = "Opacity of the ESP box")

    private val showStarMobs = BooleanSetting("Star Mobs", true, description = "Render star mob ESP.")
    private val showFelHead = BooleanSetting("Fel Head", true, description = "Render a box around Fel heads. This box can not be seen through walls.")
    private val showBat = BooleanSetting("Bat ESP", true, description = "Render the bat ESP")
    private val colorShadowAssassin = ColorSetting("SA Color", Color(255, 0, 255), false, description = "ESP color for Shadow Assassins.")
    private val colorMini = ColorSetting("Mini Boss Color", Color(255, 255, 0), false, description = "ESP color for all Mini Bosses except Shadow Assassins.")
    private val colorStar = ColorSetting("Star Mob Color", Color(255, 0, 0), false, description = "ESP color for star mobs.")
    private val colorFel = ColorSetting("Fel Color", Color(0, 255, 255), false, description = "ESP color for star Fel.")
    private val colorFelHead = ColorSetting("Fel Head Color", Color(0, 0, 255), false, description = "ESP color for Fel heads on the floor.")
    private val colorWithermancer = ColorSetting("Withermancer Color", Color(255, 255, 0), false, description = "ESP color for star Withermancer.")
    private val colorBat = ColorSetting("Bat Color", Color(0, 255, 0), false, description = "ESP color for bats.")
    private val colorKey = ColorSetting("Key Color", Color(0, 0, 0), false, description = "ESP color for wither and blood key.")

    init {
        this.addSettings(
            style,
            opacity,
            boxWidth,
            deaultLineWidth,
            specialLineWidth,
            miniLineWidth,
            showStarMobs,
            showFelHead,
            showBat,
            colorShadowAssassin,
            colorMini,
            colorStar,
            colorFel,
            colorFelHead,
            colorWithermancer,
            colorBat,
            colorKey,
        )
    }

    @SubscribeEvent
    fun onRenderWorld(event: RenderWorldLastEvent) {
        if (!this. enabled || !FloppaClient.inDungeons) return
        mc.theWorld.loadedEntityList.stream()
            .forEach { entity ->
                val name = StringUtils.stripControlCodes(entity.customNameTag ?: return@forEach)
                when (entity) {
                    is EntityArmorStand -> {
                        if(showStarMobs.enabled && entity.customNameTag.contains("✯")
                            && !entity.customNameTag.contains("Angry Archeologist")
                            && !entity.customNameTag.contains("Frozen Adventurer")
                            && !entity.customNameTag.contains("Lost Adventurer")
                        ) { // Starred mobs
                            val mob = getCorrespondingMob(entity) ?: return@forEach

                            if (entity.customNameTag.contains("Fel")) drawBoxByEntity( // Fel
                                mob, colorFel.value, boxWidth.value,3.0, event.partialTicks,
                                this.style.index == 1, true, thickness = specialLineWidth.value.toFloat(), opacity = opacity.value.toFloat()
                                )

                            else if (entity.customNameTag.contains("Withermancer")) drawBoxByEntity( // Withermancer
                                     mob, colorWithermancer.value, boxWidth.value,2.4, event.partialTicks,
                                this.style.index == 1, true, thickness = specialLineWidth.value.toFloat(), opacity = opacity.value.toFloat()
                                )

                            else drawBoxByEntity( // Rest of the Starred Mobs
                                mob, colorStar.value, boxWidth.value,2.0,event.partialTicks,
                                this.style.index == 1, true, thickness = deaultLineWidth.value.toFloat(), opacity = opacity.value.toFloat()
                                )
                        }
                        else if (name.equals("Wither Key") || name.equals("Blood Key")) drawBoxByEntity( // Door keys
                            entity,colorKey.value, boxWidth.value,1.0,event.partialTicks,
                            this.style.index == 1, true, yOffset = 1.0, thickness = miniLineWidth.value.toFloat(), opacity = opacity.value.toFloat()
                        )
                    }
                    is EntityEnderman -> {
                        if (showFelHead.enabled && entity.customNameTag == "Dinnerbone") drawBoxByEntity( // Fel Head
                            entity ,colorFelHead.value, boxWidth.value,1.0,event.partialTicks,
                            this.style.index == 1, true, thickness = specialLineWidth.value.toFloat(), opacity = opacity.value.toFloat()
                        )

                    }
                    is EntityOtherPlayerMP -> {
                        if (entity.name.contains("Shadow Assassin")) drawBoxByEntity( // Shadow Assassin
                            entity,colorShadowAssassin.value, boxWidth.value,2.0,event.partialTicks,
                            this.style.index == 1, true, thickness = miniLineWidth.value.toFloat(), opacity = opacity.value.toFloat()
                        )

                        if (entity.name == "Diamond Guy" || entity.name == "Lost Adventurer") drawBoxByEntity( // Lost Adventurer and Angry Archeologist
                            entity,colorMini.value, boxWidth.value,2.0, event.partialTicks,
                            this.style.index == 1, true, thickness = miniLineWidth.value.toFloat(), opacity = opacity.value.toFloat()
                        )

                    }
                    is EntityBat -> {
                        if (showBat.enabled && !entity.isInvisible) drawBoxByEntity( // Secret bat
                            entity, colorBat.value, entity.width, entity.height, event.partialTicks,
                            this.style.index == 1, true, thickness = deaultLineWidth.value.toFloat(), opacity = opacity.value.toFloat()
                        )
                    }
                }
        }
    }

    private fun getCorrespondingMob(entity: Entity): Entity? {
        val possibleEntities = entity.entityWorld.getEntitiesInAABBexcluding(
            entity, entity.entityBoundingBox.offset(0.0, -1.0, 0.0)
        ) { it !is EntityArmorStand }

        return possibleEntities.find {
            when (it) {
                is EntityPlayer -> !it.isInvisible() && it.getUniqueID()
                    .version() == 2 && it != mc.thePlayer
                is EntityWither -> false
                else -> true
            }
        }
    }
}