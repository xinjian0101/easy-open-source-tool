package com.liming.livestage;
import android.os.Handler;import android.os.Looper;import java.util.Random;
public final class LocalSimulator{
 public interface Listener{void stats(int viewers,long likes);void message(String user,String text);void gift(String user,String gift,int count);}
 private final Handler h=new Handler(Looper.getMainLooper());private final Random r=new Random();private final Listener l;private boolean run;private int viewers=128;private long likes;
 private final String[] names={"Mia_27","Leo","小雨","阿泽","Emma","Jason","橘子汽水","Nova","Kai","Luna"};
 private final String[] texts={"刚进来，主播在做什么？","Where are you tonight?","背景音乐不错","Show us around","这个镜头挺稳","What are you drinking?","今天人好多","刚从推荐页进来","灯光有氛围","主播能看到评论吗？"};
 private final String[] gifts={"Rose","Heart","Coffee","Star","Crown"};
 public LocalSimulator(Listener x){l=x;}
 public void start(){if(run)return;run=true;h.post(tick);}public void stop(){run=false;h.removeCallbacks(tick);}public void comment(){l.message(p(names),p(texts));}public void like(){likes+=10+r.nextInt(30);l.stats(viewers,likes);}public void gift(){l.gift(p(names),p(gifts),r.nextInt(4)==0?3:1);viewers+=2+r.nextInt(8);l.stats(viewers,likes);}
 private String p(String[] a){return a[r.nextInt(a.length)];}
 private final Runnable tick=new Runnable(){public void run(){if(!run)return;viewers=Math.max(1,viewers+r.nextInt(9)-3);likes+=r.nextInt(7);l.stats(viewers,likes);int x=r.nextInt(100);if(x<55)comment();else if(x<65)gift();h.postDelayed(this,1800+r.nextInt(1800));}};
}
