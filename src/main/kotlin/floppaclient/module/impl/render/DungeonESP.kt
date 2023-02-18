package floppaclient.module.impl.render

import floppaclient.FloppaClient
import floppaclient.FloppaClient.Companion.mc
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.settings.impl.BooleanSetting
import floppaclient.module.settings.impl.ColorSetting
import floppaclient.module.settings.impl.NumberSetting
import floppaclient.utils.WorldRenderUtils.drawBoxByEntity
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
    private val boxWidth = NumberSetting("Box Width",0.9,0.1,2.0,0.05, description = "Width of the esp box in units of blocks.")
    private val deaultLineWidth = NumberSetting("Default LW",1.0,0.1,10.0,0.1, description = "Default line width of the esp box.")
    private val specialLineWidth = NumberSetting("Special Mob LW",2.0,0.1,10.0,0.1, description = "Line width of the esp box for special mobs like Fel and Withermancer.")
    private val miniLineWidth = NumberSetting("Mini Boss LW",3.0,0.1,10.0,0.1, description = "Line width of the esp box for Mini Bosses.")
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
                when(entity) {
                    is EntityArmorStand -> {
                        if(showStarMobs.enabled && entity.customNameTag.contains("✯")
                            && !entity.customNameTag.contains("Angry Archeologist")
                            && !entity.customNameTag.contains("Frozen Adventurer")
                            && !entity.customNameTag.contains("Lost Adventurer")
                        ){ // starred mobs
                            val mob = getCorrespondingMob(entity) ?: return@forEach
                            if(entity.customNameTag.contains("Fel")){ // Fel
                                drawBoxByEntity(mob,colorFel.value, boxWidth.value,3.0,event.partialTicks,
                                    specialLineWidth.value, true)
                            }else if(entity.customNameTag.contains("Withermancer")){ // Withermancer
                                drawBoxByEntity(mob,colorWithermancer.value, boxWidth.value,2.4,event.partialTicks,
                                    specialLineWidth.value,true)
                            }else {
                                drawBoxByEntity(mob,colorStar.value, boxWidth.value,2.0,event.partialTicks,
                                    deaultLineWidth.value,true)
                            }
                        }
                        else if (name.equals("Wither Key") || name.equals("Blood Key")){
                            drawBoxByEntity(entity,colorKey.value, boxWidth.value,1.0,event.partialTicks,
                                miniLineWidth.value,true,0.0,1.0,0.0)
                        }
                    }
                    is EntityEnderman -> {
                        if(showFelHead.enabled && entity.customNameTag == "Dinnerbone"){
                            drawBoxByEntity(entity,colorFelHead.value, boxWidth.value,1.0,event.partialTicks,
                                specialLineWidth.value,false)
                        }
                    }
                    is EntityOtherPlayerMP -> {
                        if(entity.name.contains("Shadow Assassin")){ // shadow assassin
                            drawBoxByEntity(entity,colorShadowAssassin.value,boxWidth.value,2.0,event.partialTicks,
                                miniLineWidth.value,true)
                        }
                        if(entity.name == "Diamond Guy" || entity.name == "Lost Adventurer"){ // miniBoss
                            drawBoxByEntity(entity,colorMini.value, boxWidth.value,2.0, event.partialTicks,
                                miniLineWidth.value,true)
                        }
                    }
                    is EntityBat -> {
                        if (showBat.enabled && !entity.isInvisible) {
                            drawBoxByEntity(entity, colorBat.value, entity.width, entity.height, event.partialTicks,
                            deaultLineWidth.value.toFloat(), true)
                        }
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