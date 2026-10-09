package com.example.pullthesword

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.*

internal val regionImages=listOf(R.drawable.bg_valley,R.drawable.bg_ice,R.drawable.bg_volcano,R.drawable.bg_forest,R.drawable.bg_kingdom,R.drawable.bg_cosmic)
private val rocks=listOf(R.drawable.rock_valley,R.drawable.rock_ice,R.drawable.rock_volcano,R.drawable.rock_forest,R.drawable.rock_kingdom,R.drawable.rock_cosmic)
private val relicImages=listOf(
    R.drawable.relic_old_sword,R.drawable.relic_iron_sword,R.drawable.relic_valley_spear,R.drawable.relic_royal_blade,
    R.drawable.relic_frozen_dagger,R.drawable.relic_ice_axe,R.drawable.relic_glacier_spear,R.drawable.relic_winter_crown,
    R.drawable.relic_flame_sword,R.drawable.relic_lava_axe,R.drawable.relic_burning_spear,R.drawable.relic_fire_hammer,
    R.drawable.relic_root_blade,R.drawable.relic_forest_axe,R.drawable.relic_emerald_staff,R.drawable.relic_sylvan_crown,
    R.drawable.relic_ancient_sword,R.drawable.relic_kings_hammer,R.drawable.relic_golden_crown,R.drawable.relic_sun_relic,
    R.drawable.relic_star_blade,R.drawable.relic_void_spear,R.drawable.relic_alien_tool,R.drawable.relic_cosmic_relic)
private val iconImages=listOf(R.drawable.ui_sword,R.drawable.ui_upgrade,R.drawable.ui_collection,R.drawable.ui_settings,R.drawable.ui_coin,R.drawable.ui_combo,R.drawable.ui_smith,R.drawable.ui_critical,R.drawable.ui_auto)
private val colors=listOf(Color(0xFF86C9AA),Color(0xFF88DFFA),Color(0xFFFF9967),Color(0xFF9BCD78),Color(0xFFE9C879),Color(0xFFBAA2FF))

@Composable internal fun GameIcon(kind:Int,color:Color) {
    Image(painterResource(iconImages[kind.coerceIn(iconImages.indices)]),contentDescription=null,modifier=Modifier.size(28.dp),alpha=if(color==Color(0xFF8192AA)) .55f else 1f)
}

@Composable fun RelicArt(relic:Relic,progress:Float,impact:Float,reduced:Boolean,modifier:Modifier,silhouette:Boolean=false,onlyRelic:Boolean=false,extraction:Float=0f,variant:Int=0,explosion:Boolean=false) {
    val item=ImageBitmap.imageResource(relicImages[Relics.all.indexOf(relic).coerceAtLeast(0)])
    val background=if(!onlyRelic) ImageBitmap.imageResource(regionImages[relic.world]) else null
    val boulder=if(!onlyRelic) ImageBitmap.imageResource(rocks[relic.world]) else null
    val cracks=if(!onlyRelic) ImageBitmap.imageResource(when {progress>.7f -> R.drawable.cracks_3;progress>.35f -> R.drawable.cracks_2;else -> R.drawable.cracks_1}) else null
    val shard=if(!onlyRelic) ImageBitmap.imageResource(R.drawable.fx_shard) else null
    val spark=if(!silhouette) ImageBitmap.imageResource(R.drawable.fx_spark) else null
    Canvas(modifier) {
        val w=size.width;val h=size.height;val unit=min(w/360f,h/440f)
        val accent=if(variant==0) colors[relic.world] else listOf(Color.White,Color(0xFFFFD981),Color(0xFFBAA2FF),Color(0xFF88DFFA))[variant%4]
        fun sprite(bitmap:ImageBitmap,x:Float,y:Float,width:Float,height:Float,alpha:Float=1f,filter:ColorFilter?=null) {
            drawImage(bitmap,dstOffset=IntOffset(x.roundToInt(),y.roundToInt()),dstSize=IntSize(width.roundToInt().coerceAtLeast(1),height.roundToInt().coerceAtLeast(1)),alpha=alpha.coerceIn(0f,1f),colorFilter=filter,filterQuality=FilterQuality.Medium)
        }
        if(background!=null) {
            val scale=max(w/background.width,h/background.height)
            sprite(background,(w-background.width*scale)/2,(h-background.height*scale)/2,background.width*scale,background.height*scale)
            drawRect(Brush.verticalGradient(listOf(Color(0xFF081222).copy(alpha=.15f),Color(0xFF081222).copy(alpha=.55f))))
        }
        val shake=if(reduced) 0f else sin(impact*25)*impact*7*unit
        translate(w/2+shake,h*.55f) {scale(unit,unit,Offset.Zero) {
            if(!silhouette) {
                drawCircle(Brush.radialGradient(listOf(accent.copy(alpha=.28f),Color.Transparent),center=Offset(0f,-15f),radius=155f),155f,Offset(0f,-15f))
                if(onlyRelic) drawCircle(accent.copy(alpha=.18f),135f,Offset(0f,0f),style=Stroke(1.5f))
            }
            val side=if(onlyRelic) 340f else 290f
            val lift=if(onlyRelic) 0f else progress*45f+if(reduced) 0f else extraction*55f
            val itemFilter=if(silhouette) ColorFilter.tint(Color(0xFF354653)) else if(variant>0) {
                val r=.5f+accent.red*.6f;val g=.5f+accent.green*.6f;val b=.5f+accent.blue*.6f
                ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(r,0f,0f,0f,8f*accent.red,0f,g,0f,0f,8f*accent.green,0f,0f,b,0f,8f*accent.blue,0f,0f,0f,1f,0f)))
            } else null
            rotate(if(reduced) 0f else impact*3f,Offset.Zero) {
                sprite(item,-side/2,-side*.53f-lift,side,side,filter=itemFilter)
            }
            if(variant>0 && !silhouette && spark!=null) for(i in 0..3) {
                val a=i*PI/2;val x=(cos(a)*105).toFloat();val y=(sin(a)*105).toFloat()-lift
                sprite(spark,x-15,y-15,30f,30f,filter=ColorFilter.tint(accent))
            }
            if(boulder!=null && extraction<.98f) {
                val sink=if(reduced) 0f else extraction*70
                sprite(boulder,-190f,-37f+sink,380f,230f,1-extraction)
                if(cracks!=null && progress>.05f) {
                    // Clip the fracture layer to the body of the foreground boulder.
                    val mask=Path().apply {moveTo(-125f,48f+sink);lineTo(-90f,16f+sink);lineTo(85f,16f+sink);lineTo(130f,50f+sink);lineTo(100f,125f+sink);lineTo(-100f,125f+sink);close()}
                    clipPath(mask) {sprite(cracks,-135f,-15f+sink,270f,160f,(progress*1.5f).coerceAtMost(1f)*(1-extraction))}
                }
            }
            if(!reduced && extraction>0f && shard!=null) {
                drawCircle(accent.copy(alpha=(1-extraction)*.6f),40+extraction*155,Offset(0f,40f),style=Stroke(4f))
                for(i in 0..11) {
                    val a=i*PI/6;val distance=35+extraction*145
                    val center=Offset((cos(a)*distance).toFloat(),40+(sin(a)*distance).toFloat()+extraction*extraction*65)
                    rotate(i*31f+extraction*160,center) {sprite(shard,center.x-18,center.y-18,36f,36f,1-extraction)}
                }
            }
            if(!reduced && impact>0f && spark!=null) {
                for(i in 0..7) {
                    val a=i*PI/4;val distance=40+(1-impact)*90
                    val center=Offset((cos(a)*distance).toFloat(),35+(sin(a)*distance*.6).toFloat())
                    sprite(spark,center.x-12,center.y-12,24f,24f,impact,ColorFilter.tint(if(explosion) Color(0xFFFFAA55) else accent))
                }
                if(explosion) drawCircle(Color(0xFFFF9967).copy(alpha=impact*.65f),45+(1-impact)*130,Offset(0f,35f),style=Stroke(8f*impact))
            }
        }}
    }
}
