package app.saltabeat.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import app.saltabeat.engine.Difficulty
import kotlinx.coroutines.flow.*
import java.io.IOException

private val Context.store by preferencesDataStore("saltabeat")
data class Settings(val music:Float=.65f,val effects:Float=.8f,val vibration:Boolean=true,
                    val reduce:Boolean=false,val calibration:Int=0,val tutorial:Boolean=false,
                    val calibrated:Boolean=false,val difficulty:Difficulty=Difficulty.NORMAL,
                    val easy:Int=0,val normal:Int=0,val hard:Int=0,val bestCombo:Int=0) {
    fun record(d:Difficulty)=when(d){Difficulty.EASY->easy;Difficulty.NORMAL->normal;Difficulty.HARD->hard}
}
class Preferences(context:Context) {
    private val store=context.store
    val settings=store.data.catch{if(it is IOException) emit(emptyPreferences()) else throw it}.map{p->
        Settings(p[floatPreferencesKey("music")]?:.65f,p[floatPreferencesKey("effects")]?:.8f,
            p[booleanPreferencesKey("vibration")]?:true,p[booleanPreferencesKey("reduce")]?:false,
            p[intPreferencesKey("calibration")]?:0,p[booleanPreferencesKey("tutorial")]?:false,
            p[booleanPreferencesKey("calibrated")]?:false,
            Difficulty.entries.getOrElse(p[intPreferencesKey("difficulty")]?:1){Difficulty.NORMAL},
            p[intPreferencesKey("record_EASY")]?:0,p[intPreferencesKey("record_NORMAL")]?:0,
            p[intPreferencesKey("record_HARD")]?:0,p[intPreferencesKey("combo")]?:0)
    }
    suspend fun music(value:Float)=store.edit{it[floatPreferencesKey("music")]=value.coerceIn(0f,1f)}
    suspend fun effects(value:Float)=store.edit{it[floatPreferencesKey("effects")]=value.coerceIn(0f,1f)}
    suspend fun toggle(key:String,value:Boolean)=store.edit{it[booleanPreferencesKey(key)]=value}
    suspend fun calibration(ms:Int)=store.edit{it[intPreferencesKey("calibration")]=ms.coerceIn(-300,300);it[booleanPreferencesKey("calibrated")]=true}
    suspend fun difficulty(d:Difficulty)=store.edit{it[intPreferencesKey("difficulty")]=d.ordinal}
    suspend fun record(d:Difficulty,points:Int,combo:Int)=store.edit{
        val k=intPreferencesKey("record_${d.name}");it[k]=maxOf(it[k]?:0,points)
        val c=intPreferencesKey("combo");it[c]=maxOf(it[c]?:0,combo)
    }
    suspend fun clear()=store.edit{p->Difficulty.entries.forEach{p.remove(intPreferencesKey("record_${it.name}"))};p.remove(intPreferencesKey("combo"))}
}
