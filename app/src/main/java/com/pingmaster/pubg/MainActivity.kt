package com.pingmaster.pubg

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.*
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import org.json.JSONArray
import kotlin.math.abs

class MainActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var network: TextView
    private lateinit var status: TextView
    private lateinit var details: TextView
    private lateinit var history: TextView
    private lateinit var chart: TrendView
    private lateinit var start: Button
    private lateinit var stop: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 31)
        updateNetwork()
    }
    override fun onResume() { super.onResume(); handler.post(refresh) }
    override fun onPause() { handler.removeCallbacks(refresh); super.onPause() }
    private val refresh = object : Runnable { override fun run() { refreshData(); handler.postDelayed(this, 1500) } }

    private fun buildUi() {
        val root = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(16,18,16,16); setBackgroundColor(Color.rgb(10,15,28)) }
        val scroll = ScrollView(this)
        val body = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
        scroll.addView(body)
        body.addView(label("PUBG PING MASTER",25,Color.WHITE,true))
        body.addView(label("NETWORK DIAGNOSTICS • VERSION 3.1",12,0xFF00DCAA.toInt(),true))
        network=label("Checking network…",14,Color.LTGRAY); body.addView(network)
        body.addView(label("GAME MODE • LIVE NETWORK MONITOR",16,Color.WHITE,true).apply { setPadding(0,18,0,8) })
        status=label("Monitoring stopped",14,Color.LTGRAY); body.addView(status)
        chart=TrendView(this).apply { setBackgroundColor(Color.rgb(22,31,49)) }
        body.addView(chart,LinearLayout.LayoutParams(-1,150).apply { setMargins(0,8,0,8) })
        details=label("No samples yet",13,Color.LTGRAY); body.addView(details)
        start=Button(this).apply { text="START GAME MODE"; setOnClickListener { startMonitor() } }; body.addView(start)
        stop=Button(this).apply { text="STOP MONITORING"; isEnabled=false; setOnClickListener { stopMonitor() } }; body.addView(stop)
        body.addView(label("RECENT SAMPLES",15,Color.WHITE,true).apply { setPadding(0,18,0,8) })
        history=label("No samples",12,Color.LTGRAY); body.addView(history)
        body.addView(label("TCP measures connection time to port 443. UDP measures DNS request/response time to port 53. These are not ICMP ping or PUBG game-server ping. Game Mode runs a foreground service so you can switch to PUBG while monitoring. It cannot read PUBG internal ping, change game traffic, or reduce latency. Repeated tests use battery and a small amount of data.",12,0xFFFFC35F.toInt()).apply { setPadding(0,18,0,10) })
        root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f)); setContentView(root)
    }
    private fun label(s:String,size:Int,color:Int,bold:Boolean=false)=TextView(this).apply { text=s;textSize=size.toFloat();setTextColor(color);gravity=Gravity.CENTER;if(bold)typeface=Typeface.DEFAULT_BOLD }
    private fun startMonitor() {
        val i=Intent(this,PingMonitorService::class.java).setAction(PingMonitorService.START)
        if(Build.VERSION.SDK_INT>=26) startForegroundService(i) else startService(i)
    }
    private fun stopMonitor() { startService(Intent(this,PingMonitorService::class.java).setAction(PingMonitorService.STOP)) }
    private fun updateNetwork() {
        val cm=getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val c=cm.getNetworkCapabilities(cm.activeNetwork)
        network.text=when { c==null->"No network";c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)->"Connected via Wi-Fi";c.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)->"Connected via mobile data";else->"Connected to network" }
    }
    private fun refreshData() {
        updateNetwork()
        val p=getSharedPreferences(PingMonitorService.PREFS,MODE_PRIVATE)
        val running=p.getBoolean("running",false);start.isEnabled=!running;stop.isEnabled=running
        status.text=if(running) "Monitoring active • switch to PUBG now" else "Monitoring stopped"
        val a=try{JSONArray(p.getString("samples","[]"))}catch(_:Exception){JSONArray()}
        val ct=mutableListOf<Long>();val gt=mutableListOf<Long>();val cu=mutableListOf<Long>();val gu=mutableListOf<Long>();val trend=mutableListOf<Long>()
        for(i in 0 until a.length()) { val o=a.optJSONObject(i)?:continue;val n=o.optString("name");if(!o.isNull("tcp")){val v=o.optLong("tcp");if(n=="Cloudflare")ct.add(v) else gt.add(v);trend.add(v)};if(!o.isNull("udp")){val v=o.optLong("udp");if(n=="Cloudflare")cu.add(v) else gu.add(v)} }
        chart.values=trend.takeLast(60)
        details.text="Cloudflare TCP: ${stats(ct)}\nCloudflare UDP DNS: ${stats(cu)}\n\nGoogle TCP: ${stats(gt)}\nGoogle UDP DNS: ${stats(gu)}"
        history.text=(0 until a.length()).mapNotNull { i->a.optJSONObject(i)?.let { "${it.optString("name")}  TCP ${it.opt("tcp")} ms • UDP DNS ${it.opt("udp")} ms" } }.takeLast(10).asReversed().joinToString("\n").ifBlank{"No samples yet"}
    }
    private fun stats(v:List<Long>):String { if(v.isEmpty())return "no replies";val s=v.sorted();val med=if(s.size%2==0)(s[s.size/2-1]+s[s.size/2])/2.0 else s[s.size/2].toDouble();val jit=if(v.size<2)0.0 else v.zipWithNext{a,b->abs(b-a).toDouble()}.average();return "avg ${"%.1f".format(v.average())} ms | median ${"%.1f".format(med)} | min ${s.first()} | max ${s.last()} | jitter ${"%.1f".format(jit)} ms" }
    override fun onDestroy(){handler.removeCallbacks(refresh);super.onDestroy()}
}