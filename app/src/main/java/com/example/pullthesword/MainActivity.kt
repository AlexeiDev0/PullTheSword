package com.example.pullthesword

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.*
import kotlin.random.Random

private val palettes = listOf(Color(0xFF86C9AA),Color(0xFF88DFFA),Color(0xFFFF9967),Color(0xFF9BCD78),Color(0xFFE9C879),Color(0xFFBAA2FF))
private val worlds = listOf("Forgotten Valley","Frozen Lands","Volcanic Lands","Ancient Forest","Lost Kingdom","Cosmic Ruins")
private val ruWorlds = listOf("Забытая долина","Ледяные земли","Вулканические земли","Древний лес","Затерянное королевство","Космические руины")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("relic", MODE_PRIVATE)
        val game = GameState().apply {
            style = prefs.getInt("style",0); comboLevel = prefs.getInt("comboLevel",0)
            automationLevel = prefs.getInt("automationLevel",0); manualHits = prefs.getInt("manualHits",0)
            coins = prefs.getLong("coins",0); level = prefs.getInt("level",0)
            auto = prefs.getInt("auto",0); critical = prefs.getInt("critical",0)
            index = prefs.getInt("index",0); damage = prefs.getFloat("damage",0f).toDouble()
            claimed = prefs.getStringSet("claimed",emptySet())!!.mapNotNull { it.toIntOrNull() }.toMutableSet()
        }
        setContent {
            var revision by remember { mutableIntStateOf(0) }
            var lang by remember { mutableStateOf(prefs.getString("lang","Русский")!!) }
            var sound by remember { mutableStateOf(prefs.getBoolean("sound",true)) }
            var music by remember { mutableStateOf(prefs.getBoolean("music",false)) }
            var vibration by remember { mutableStateOf(prefs.getBoolean("vibration",true)) }
            var reduced by remember { mutableStateOf(prefs.getBoolean("reduced",false)) }
            fun save() {
                prefs.edit().putInt("style",game.style).putInt("comboLevel",game.comboLevel)
                    .putInt("automationLevel",game.automationLevel).putInt("manualHits",game.manualHits)
                    .putLong("coins",game.coins).putInt("level",game.level).putInt("auto",game.auto)
                    .putInt("critical",game.critical).putInt("index",game.index).putFloat("damage",game.damage.toFloat())
                    .putStringSet("claimed",game.claimed.map { it.toString() }.toSet()).putString("lang",lang)
                    .putBoolean("sound",sound).putBoolean("music",music).putBoolean("vibration",vibration).putBoolean("reduced",reduced).apply()
                revision++
            }
            fun t(ru: String,en: String,es: String) = when(lang) { "Русский" -> ru; "Español" -> es; else -> en }
            val tone = remember { ToneGenerator(AudioManager.STREAM_MUSIC,35) }
            DisposableEffect(Unit) { onDispose { tone.release() } }
            LaunchedEffect(music) {
                while (music) { tone.startTone(ToneGenerator.TONE_PROP_BEEP,80); delay(4000) }
            }
            revision
            val accent = palettes[game.relic.world]
            MaterialTheme(colorScheme = darkColorScheme(primary=accent,onPrimary=Color(0xFF09171D),background=Color(0xFF0B121E),surface=Color(0xFF182436),secondary=Color(0xFFFFD981))) {
                var tab by remember { mutableIntStateOf(0) }
                var reset by remember { mutableStateOf(false) }
                var reward by remember { mutableStateOf(false) }
                val shake = remember { Animatable(0f) }
                val extraction = remember { Animatable(0f) }
                var extracting by remember { mutableStateOf(false) }
                var autoNotice by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    while(true) {
                        delay(1000)
                        if(game.auto > 0 && (!game.ready || game.style==2) && !extracting && !reward) {
                            autoNotice = game.autoTick()
                            save()
                        }
                    }
                }
                val scope = rememberCoroutineScope()
                val haptic = LocalHapticFeedback.current
                var hitText by remember { mutableStateOf("") }
                fun strike() {
                    if(extracting || reward) return
                    autoNotice=false
                    if(game.ready) {
                        extracting=true
                        scope.launch {
                            extraction.snapTo(0f)
                            if(sound) tone.startTone(ToneGenerator.TONE_PROP_ACK,250)
                            if(vibration) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            extraction.animateTo(1f,tween(if(reduced) 150 else 1400))
                            reward=true
                            extracting=false
                        }
                        return
                    }
                    val dealt = game.manualHit(SystemClock.elapsedRealtime(),Random.nextDouble())
                    val crit = game.lastCritical
                    hitText = "+${dealt.toInt()}" + if(game.lastExplosion) " ✹" else if(crit) " ★" else ""
                    if(sound) tone.startTone(if(crit) ToneGenerator.TONE_PROP_ACK else ToneGenerator.TONE_PROP_BEEP,45)
                    if(vibration) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    scope.launch { if(!reduced) { shake.snapTo(1f); shake.animateTo(0f) }; delay(300); hitText="" }
                    save()
                }
                Scaffold(bottomBar={ NavigationBar(containerColor=Color(0xFF111C2B)) {
                    listOf(t("Реликвия","Relic","Reliquia"),t("Улучшения","Upgrades","Mejoras"),t("Коллекция","Collection","Colección"),t("Настройки","Settings","Ajustes")).forEachIndexed { i,label ->
                        NavigationBarItem(selected=tab==i,onClick={if(!extracting) tab=i},icon={GameIcon(i,if(tab==i) accent else Color(0xFF8192AA))},label={Text(label,fontSize=10.sp)})
                    }
                } }) { padding ->
                    Column(Modifier.fillMaxSize().paint(painterResource(regionImages[game.relic.world]),sizeToIntrinsics=false,contentScale=ContentScale.Crop).background(Brush.verticalGradient(listOf(Color(0xFF101C2E).copy(alpha=.88f),Color(0xFF0B121E).copy(alpha=.95f)))).padding(padding).padding(horizontal=20.dp)) {
                        Row(Modifier.fillMaxWidth().padding(top=16.dp,bottom=12.dp),horizontalArrangement=Arrangement.SpaceBetween) {
                            Column { Text("PULL THE RELIC",fontWeight=FontWeight.Black,letterSpacing=2.sp,fontSize=19.sp); Text(t("Экспедиция ${game.index+1}","Expedition ${game.index+1}","Expedición ${game.index+1}"),color=accent,fontSize=12.sp) }
                            Row(Modifier.clip(RoundedCornerShape(18.dp)).background(Color(0xFF302B24)).padding(horizontal=12.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically) { GameIcon(4,Color(0xFFFFD981)); Spacer(Modifier.width(6.dp));Text("${game.coins}",color=Color(0xFFFFD981),fontWeight=FontWeight.Bold,fontSize=18.sp) }
                        }
                        when(tab) {
                            0 -> {
                                Text(if(lang=="Русский") ruWorlds[game.relic.world] else worlds[game.relic.world],color=accent,modifier=Modifier.clip(RoundedCornerShape(12.dp)).background(accent.copy(alpha=.1f)).padding(horizontal=12.dp,vertical=6.dp))
                                Text(relicTitle(game.relic,game.cycle,lang),fontSize=30.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=8.dp))
                                Text(t("Сила","Power","Fuerza")+": ${game.power.toInt()}   •   "+t("Бонус коллекции","Collection bonus","Bonus de colección")+": ${((game.bonus-1)*100).roundToInt()}%",fontSize=12.sp,color=Color(0xFFABBBC7))
                                Box(Modifier.weight(1f).fillMaxWidth().padding(vertical=12.dp).clip(RoundedCornerShape(28.dp)).background(Brush.verticalGradient(listOf(accent.copy(alpha=.12f),Color(0xFF101A29)))).clickable { strike() },contentAlignment=Alignment.Center) {
                                    RelicArt(game.relic,game.progress,shake.value,reduced,Modifier.fillMaxSize(),extraction=extraction.value,variant=game.variant,explosion=game.lastExplosion)
                                    if(hitText.isNotEmpty()) Text(hitText,color=accent,fontSize=32.sp,fontWeight=FontWeight.Black,modifier=Modifier.align(Alignment.TopCenter).padding(top=48.dp))
                                }
                                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                                    Text(t("Освобождение","Extraction","Extracción"),color=Color(0xFFABBBC7)); Text("${(game.progress*100).toInt()}%",color=accent)
                                }
                                LinearProgressIndicator(progress={game.progress},modifier=Modifier.fillMaxWidth().padding(vertical=12.dp).height(7.dp),color=accent)
                                Button(onClick={strike()},enabled=!extracting,modifier=Modifier.fillMaxWidth().height(58.dp),shape=RoundedCornerShape(18.dp)) {
                                    GameIcon(0,accent);Spacer(Modifier.width(12.dp))
                                    Text(if(game.ready) t("ВЫТАЩИТЬ!","EXTRACT!","¡EXTRAER!") else t("УДАРИТЬ","STRIKE","GOLPEAR"),fontWeight=FontWeight.Black,fontSize=18.sp)
                                }
                                Text(if(autoNotice) t("Кузнец извлёк реликвию!","Smith extracted a relic!","¡El herrero extrajo una reliquia!") else if(game.style==1) t("Серия ${game.combo}/10 • интервал до 0,9 сек","Combo ${game.combo}/10 • within 0.9 sec","Combo ${game.combo}/10 • hasta 0,9 s") else t("Круг ${game.cycle+1} • постоянная сила +${game.cycle*15}%","Cycle ${game.cycle+1} • permanent power +${game.cycle*15}%","Ciclo ${game.cycle+1} • fuerza +${game.cycle*15}%"),modifier=Modifier.align(Alignment.CenterHorizontally).padding(vertical=12.dp),fontSize=11.sp,color=Color(0xFFABBBC7))
                            }
                            1 -> Column(Modifier.verticalScroll(rememberScrollState())) {
                                Text(t("Стань сильнее","Grow stronger","Hazte más fuerte"),fontSize=28.sp,fontWeight=FontWeight.Bold)
                                Text(t("Стиль можно менять бесплатно","Switch styles freely","Cambia de estilo gratis"),modifier=Modifier.padding(top=12.dp))
                                listOf(t("Мощь: +25% ручного урона","Power: +25% manual damage","Fuerza: +25% daño manual"),t("Серия: быстрые удары усиливают урон","Combo: quick strikes build damage","Combo: golpes rápidos aumentan daño"),t("Кузнец: +50% автоурона и автосбор","Smith: +50% auto damage and auto claim","Herrero: +50% daño automático y recogida")).forEachIndexed { i,label ->
                                    FilterChip(selected=game.style==i,onClick={game.style=i;game.combo=0;save()},label={Text(label)})
                                }
                                Upgrade(t("Сила +3","Power +3","Fuerza +3"),"${game.level}",game.powerCost,game.coins,iconKind=0) { game.buyUpgrade(0);save() }
                                Upgrade(t("Дух кузнеца","Smith's spirit","Espíritu del herrero"),t("${game.auto} силы удара в секунду","${game.auto} strike power/sec","${game.auto} fuerza/seg"),game.autoCost,game.coins,iconKind=8) {game.buyUpgrade(1);save()}
                                Upgrade(t("Критический удар","Critical strike","Golpe crítico"),"${game.critical*3}% • ×3",game.critCost,game.coins,game.critical<10,iconKind=7) {game.buyUpgrade(2);save()}
                                Upgrade(t("Мастер серии","Combo mastery","Maestro de combo"),t("+1% за шаг серии • уровень ${game.comboLevel}","+1% per combo step • level ${game.comboLevel}","+1% por paso • nivel ${game.comboLevel}"),game.comboCost,game.coins,iconKind=5) {game.buyUpgrade(3);save()}
                                Upgrade(t("Мастер кузнеца","Smith mastery","Maestro herrero"),t("+15% автоурона • уровень ${game.automationLevel}","+15% auto damage • level ${game.automationLevel}","+15% daño automático • nivel ${game.automationLevel}"),game.automationCost,game.coins,iconKind=6) {game.buyUpgrade(4);save()}
                            }
                            2 -> {
                                Text(t("Коллекция","Collection","Colección")+" ${game.claimed.size}/${Relics.all.size}",fontSize=26.sp,fontWeight=FontWeight.Bold)
                                Text(t("5 находок: +2% • 10 находок: +5%","5 discoveries: +2% • 10 discoveries: +5%","5 hallazgos: +2% • 10 hallazgos: +5%"),fontSize=12.sp,color=accent,modifier=Modifier.padding(vertical=8.dp))
                                val fireComplete = (8..11).all { it in game.claimed }
                                Text(if(fireComplete) t("Огненный набор собран!","Fire set complete!","¡Set de fuego completo!") else t("Огненный набор","Fire set","Set de fuego")+" ${(8..11).count { it in game.claimed }}/4",color=Color(0xFFFF9967))
                                Text(t("Огонь: каждый 10-й ручной удар ×4","Fire: every 10th manual strike ×4","Fuego: cada décimo golpe manual ×4"),fontSize=12.sp)
                                Text(t("Лёд: +4% за шаг серии","Ice: +4% per combo step","Hielo: +4% por paso")+" • ${(4..7).count { it in game.claimed }}/4",fontSize=12.sp,color=palettes[1])
                                Text(t("Каждые 24 находки: новый облик и +15% постоянной силы. Круг ${game.cycle+1}","Every 24 relics: new look and +15% permanent power. Cycle ${game.cycle+1}","Cada 24 reliquias: nuevo aspecto y +15% fuerza. Ciclo ${game.cycle+1}"),fontSize=12.sp,modifier=Modifier.padding(top=8.dp))
                                LazyVerticalGrid(columns=GridCells.Fixed(2),verticalArrangement=Arrangement.spacedBy(10.dp),horizontalArrangement=Arrangement.spacedBy(10.dp),modifier=Modifier.padding(top=12.dp)) {
                                    itemsIndexed(Relics.all) { i,r -> val found=i in game.claimed
                                        Card(shape=RoundedCornerShape(22.dp),colors=CardDefaults.cardColors(containerColor=Color(0xFF1A293B))) { Column(Modifier.padding(10.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                                            RelicArt(r,1f,0f,true,Modifier.fillMaxWidth().height(125.dp),!found,true,variant=game.variant)
                                            Text(if(found) relicTitle(r,game.cycle,lang) else "???",fontWeight=FontWeight.Bold,fontSize=13.sp)
                                            Text(if(found) listOf("COMMON","RARE","EPIC","LEGENDARY","MYTHIC")[r.rarity] else t("Не найдено","Undiscovered","Sin descubrir"),fontSize=10.sp,color=if(found) palettes[r.world] else Color.Gray)
                                        } }
                                    }
                                }
                            }
                            3 -> Column(Modifier.verticalScroll(rememberScrollState())) {
                                Text(t("Настройки","Settings","Ajustes"),fontSize=28.sp,fontWeight=FontWeight.Bold)
                                Row(Modifier.padding(vertical=16.dp),horizontalArrangement=Arrangement.spacedBy(5.dp)) { listOf("Русский","English","Español").forEach { language -> FilterChip(selected=lang==language,onClick={lang=language;save()},label={Text(language,fontSize=12.sp)}) } }
                                Toggle(t("Звук","Sound","Sonido"),sound){sound=it;save()}
                                Toggle(t("Звуковой фон","Ambient tones","Tonos ambientales"),music){music=it;save()}
                                Toggle(t("Вибрация","Vibration","Vibración"),vibration){vibration=it;save()}
                                Toggle(t("Меньше движения","Reduced motion","Movimiento reducido"),reduced){reduced=it;save()}
                                OutlinedButton(onClick={reset=true},modifier=Modifier.padding(top=24.dp)) {Text(t("Сбросить прогресс","Reset progress","Reiniciar progreso"))}
                            }
                        }
                    }
                }
                if(reward) Dialog(onDismissRequest={reward=false;scope.launch { extraction.snapTo(0f) }}) {
                    Column(Modifier.clip(RoundedCornerShape(32.dp)).background(Brush.verticalGradient(listOf(accent.copy(alpha=.35f),Color(0xFF142134)))).padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                        Text(t("РЕЛИКВИЯ ИЗВЛЕЧЕНА!","RELIC EXTRACTED!","¡RELIQUIA EXTRAÍDA!"),color=Color(0xFFFFD981),fontWeight=FontWeight.Black,letterSpacing=1.sp)
                        RelicArt(game.relic,1f,0f,reduced,Modifier.fillMaxWidth().height(240.dp),onlyRelic=true,variant=game.variant)
                        Text(relicTitle(game.relic,game.cycle,lang),fontSize=24.sp,fontWeight=FontWeight.Bold)
                        Text(if(game.index%24 in game.claimed) t("Усиленная находка","Empowered discovery","Hallazgo potenciado") else t("Новая находка в коллекции","A new discovery for your collection","Un nuevo hallazgo para tu colección"),color=accent,modifier=Modifier.padding(vertical=12.dp))
                        Text("+${(game.required/3).toLong()} ◈",color=Color(0xFFFFD981),fontSize=22.sp,fontWeight=FontWeight.Bold)
                        if(game.index%24==23) Text(t("Новый круг: +15% силы и новый облик!","New cycle: +15% power and a new look!","Nuevo ciclo: +15% fuerza y nuevo aspecto!"),color=accent,modifier=Modifier.padding(top=12.dp))
                        Button(onClick={game.claim();reward=false;scope.launch { extraction.snapTo(0f) };save()},modifier=Modifier.fillMaxWidth().padding(top=20.dp).height(52.dp)) {Text(t("ЗАБРАТЬ","CLAIM","RECOGER"),fontWeight=FontWeight.Bold)}
                    }
                }
                if(reset) AlertDialog(onDismissRequest={reset=false},title={Text(t("Сбросить всё?","Reset everything?","¿Reiniciar todo?"))},text={Text(t("Монеты, улучшения и коллекция будут удалены.","Coins, upgrades and collection will be erased.","Se borrarán monedas, mejoras y colección."))},confirmButton={TextButton(onClick={game.coins=0;game.level=0;game.auto=0;game.critical=0;game.index=0;game.damage=0.0;game.claimed.clear();game.style=0;game.comboLevel=0;game.automationLevel=0;game.manualHits=0;game.combo=0;game.lastManualHit=0L;autoNotice=false;reset=false;save()}){Text(t("Сбросить","Reset","Reiniciar"))}},dismissButton={TextButton(onClick={reset=false}){Text(t("Отмена","Cancel","Cancelar"))}})
            }
        }
    }
}

@Composable private fun Toggle(label:String,value:Boolean,onChange:(Boolean)->Unit) { Row(Modifier.fillMaxWidth().padding(vertical=5.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){Text(label);Switch(checked=value,onCheckedChange=onChange)} }
@Composable private fun Upgrade(name:String,detail:String,cost:Long,coins:Long,available:Boolean=true,iconKind:Int=1,buy:()->Unit) {
    Card(Modifier.fillMaxWidth().padding(top=14.dp),shape=RoundedCornerShape(22.dp),colors=CardDefaults.cardColors(containerColor=Color(0xFF1A293B))) { Column(Modifier.padding(18.dp)) {
        Row(verticalAlignment=Alignment.CenterVertically) { Box(Modifier.clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha=.15f)).padding(10.dp)) {GameIcon(iconKind,MaterialTheme.colorScheme.primary)};Spacer(Modifier.width(12.dp));Text(name,fontSize=20.sp,fontWeight=FontWeight.Bold) }
        Text(detail,color=Color(0xFFABBBC7),modifier=Modifier.padding(top=10.dp))
        Button(onClick=buy,enabled=coins>=cost && available,modifier=Modifier.fillMaxWidth().padding(top=12.dp).height(46.dp)){Text(if(available) "◈ $cost" else "MAX",fontWeight=FontWeight.Bold)}
    } }
}

private fun relicTitle(relic:Relic,cycle:Int,lang:String):String {
    if(cycle==0) return relic.name
    val names=when(lang) {"Русский" -> listOf("Пробуждённая","Золотая","Теневая","Астральная");"Español" -> listOf("Despertada","Dorada","Sombría","Astral");else -> listOf("Awakened","Golden","Shadow","Astral")}
    return "${names[cycle%4]} • ${relic.name} +$cycle"
}
