package com.pingmaster.pubg
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import android.view.View
class TrendView(c:Context):View(c){private val p=Paint(3);var values:List<Long> = emptyList();set(v){field=v.takeLast(60);invalidate()};override fun onDraw(c:Canvas){super.onDraw(c);val w=width.toFloat();val h=height.toFloat();p.color=Color.rgb(48,60,80);p.strokeWidth=1f;for(i in 1..3)c.drawLine(0f,h*i/4,w,h*i/4,p);if(values.size<2)return;val m=(values.maxOrNull()?:100L).coerceAtLeast(100).toFloat();p.color=Color.rgb(0,220,160);p.strokeWidth=4f;val step=w/(values.size-1);for(i in 1 until values.size){val y1=h-values[i-1]/m*(h-12)-6;val y2=h-values[i]/m*(h-12)-6;c.drawLine((i-1)*step,y1,i*step,y2,p)}}}
