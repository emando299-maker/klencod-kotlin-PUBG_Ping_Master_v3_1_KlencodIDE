package com.pingmaster.pubg
import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
class PingMonitorService:Service(){private val executor=Executors.newSingleThreadScheduledExecutor();@Volatile private var running=false
 override fun onCreate(){super.onCreate();if(Build.VERSION.SDK_INT>=26){val ch=NotificationChannel(CHANNEL,"Ping monitoring",NotificationManager.IMPORTANCE_LOW);(getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch)}}
 override fun onStartCommand(i:Intent?,flags:Int,id:Int):Int{if(i?.action==STOP){running=false;getSharedPreferences(PREFS,0).edit().putBoolean("running",false).apply();executor.shutdownNow();stopForeground(true);stopSelf();return START_NOT_STICKY};if(!running){running=true;startForeground(31,notification());getSharedPreferences(PREFS,0).edit().putBoolean("running",true).apply();executor.scheduleWithFixedDelay({sample()},0,2,TimeUnit.SECONDS)};return START_STICKY}
 private fun notification():Notification{val b=if(Build.VERSION.SDK_INT>=26)Notification.Builder(this,CHANNEL) else Notification.Builder(this);return b.setContentTitle("PUBG Ping Master").setContentText("Network monitoring active").setSmallIcon(android.R.drawable.stat_notify_sync).setOngoing(true).build()}
 private fun sample(){if(!running)return;for((name,host) in listOf("Cloudflare" to "1.1.1.1","Google" to "8.8.8.8")){if(!running)return;val tcp=PingTester.measureTcp(host);val udp=PingTester.measureUdpDns(host);synchronized(this){val p=getSharedPreferences(PREFS,0);val a=try{JSONArray(p.getString("samples","[]"))}catch(_:Exception){JSONArray()};a.put(JSONObject().put("name",name).put("tcp",tcp?:JSONObject.NULL).put("udp",udp?:JSONObject.NULL).put("time",System.currentTimeMillis()));while(a.length()>400)a.remove(0);p.edit().putString("samples",a.toString()).apply()}}}
 override fun onDestroy(){running=false;executor.shutdownNow();getSharedPreferences(PREFS,0).edit().putBoolean("running",false).apply();super.onDestroy()};override fun onBind(i:Intent?):IBinder?=null
 companion object{const val START="ping.start";const val STOP="ping.stop";const val PREFS="ping_master_v31";const val CHANNEL="ping_monitor"}}
