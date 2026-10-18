package com.sonora.music;

import android.app.*;
import android.content.*;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.media.MediaMetadataCompat;
import androidx.core.app.NotificationCompat;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MediaPlaybackService extends Service {
    public static final String ACTION_PLAY="com.sonora.music.PLAY", ACTION_TOGGLE="com.sonora.music.TOGGLE", ACTION_NEXT="com.sonora.music.NEXT", ACTION_PREV="com.sonora.music.PREV", ACTION_STOP="com.sonora.music.STOP";
    public static final String BROADCAST="com.sonora.music.PLAYBACK_STATE";
    private static final String CHANNEL="sonora_playback";
    private static final int NOTIFICATION_ID=42;
    private static final String PREFS="sonora_playback_state", QUEUE_KEY="queue", INDEX_KEY="index";
    private MediaPlayer player;
    private MediaSessionCompat session;
    private final ArrayList<TrackData> queue=new ArrayList<>();
    private final ExecutorService artworkExecutor=Executors.newSingleThreadExecutor();
    private int index=0, failedSkips=0;
    private boolean prepared=false, foreground=false, userStopped=false;
    private volatile boolean destroyed=false;
    private final Handler mainHandler=new Handler(Looper.getMainLooper());
    private String error="";

    static class TrackData {
        String title,artist,uri,url,source,art;
        TrackData(String t,String a,String u,String url,String s,String art){title=t;artist=a;uri=u;this.url=url;source=s;this.art=art;}
        String key(){return !TextUtils.isEmpty(uri)?uri:url;}
    }
    @Override public void onCreate(){super.onCreate();createChannel();session=new MediaSessionCompat(this,"SonoraSession");session.setCallback(new MediaSessionCompat.Callback(){
        @Override public void onPlay(){resume();}
        @Override public void onPause(){pause();}
        @Override public void onSkipToNext(){skip(1);}
        @Override public void onSkipToPrevious(){skip(-1);}
        @Override public void onStop(){stopPlayback();}
        @Override public void onSeekTo(long position){try{if(player!=null&&prepared){player.seekTo((int)Math.max(0,position));updateSession(safePlaying()?PlaybackStateCompat.STATE_PLAYING:PlaybackStateCompat.STATE_PAUSED);}}catch(Exception e){fail("No se pudo cambiar la posición de reproducción.",false);}}
    });session.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS|MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);session.setActive(true);}

    @Override public int onStartCommand(Intent intent,int flags,int startId){
        if(intent==null){restoreQueue();if(!userStopped&&!queue.isEmpty()&&player==null)playCurrent();return START_NOT_STICKY;}
        String action=intent.getAction();
        if(ACTION_PLAY.equals(action)){readQueue(intent.getStringExtra("queue"));index=intent.getIntExtra("index",0);userStopped=false;persistQueue();playCurrent();}
        else if(ACTION_TOGGLE.equals(action)){if(player!=null&&prepared&&safePlaying())pause();else resume();}
        else if(ACTION_NEXT.equals(action))skip(1);
        else if(ACTION_PREV.equals(action))skip(-1);
        else if(ACTION_STOP.equals(action))stopPlayback();
        return START_NOT_STICKY;
    }
    private void readQueue(String raw){queue.clear();try{org.json.JSONArray a=new org.json.JSONArray(raw==null?"[]":raw);for(int i=0;i<a.length();i++){org.json.JSONObject o=a.getJSONObject(i);String uri=o.optString("uri",""),url=o.optString("url","");if(uri.isEmpty()&&!validHttps(url))continue;queue.add(new TrackData(safeText(o.optString("title","Audio"),160),safeText(o.optString("artist","Desconocido"),160),uri,url,safeText(o.optString("source","LOCAL"),32),validHttps(o.optString("art",""))?o.optString("art",""):""));}}catch(Exception ignored){}if(!queue.isEmpty())index=Math.max(0,Math.min(index,queue.size()-1));}
    private String safeText(String value,int max){if(value==null)return "";value=value.replaceAll("[\\p{Cntrl}]", " ").trim();return value.length()>max?value.substring(0,max):value;}
    private boolean validHttps(String value){try{return value!=null&&"https".equalsIgnoreCase(new URL(value).getProtocol())&&new URL(value).getHost()!=null;}catch(Exception e){return false;}}
    private void playCurrent(){
        if(queue.isEmpty()){stopPlayback();return;}
        index=Math.max(0,Math.min(index,queue.size()-1));TrackData t=queue.get(index);releasePlayer();prepared=false;error="";
        try{ensureForeground(notification(t,false,null));publish("loading");player=new MediaPlayer();player.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());player.setWakeMode(this,PowerManager.PARTIAL_WAKE_LOCK);
            player.setOnPreparedListener(mp->{prepared=true;try{mp.start();updateSession(PlaybackStateCompat.STATE_PLAYING);updateNotificationWithArtwork(t);publish("playing");}catch(Exception e){fail("No se pudo iniciar la reproducción.",true);}});
            player.setOnCompletionListener(mp->{if(index+1<queue.size())skip(1);else{pause();publish("completed");}});
            player.setOnErrorListener((mp,what,extra)->{fail("El audio no está disponible o el formato no es compatible.",true);return true;});
            if(!TextUtils.isEmpty(t.uri)){Uri uri=Uri.parse(t.uri);if(!"content".equals(uri.getScheme())&&!"file".equals(uri.getScheme()))throw new IllegalArgumentException("URI local no válida");player.setDataSource(this,uri);}else if(validHttps(t.url))player.setDataSource(t.url);else throw new IllegalArgumentException("Fuente de audio no válida");player.prepareAsync();
        }catch(Exception e){fail("No se pudo abrir la pista. Comprueba el permiso del archivo o el enlace.",true);}
    }
    private boolean safePlaying(){try{return player!=null&&player.isPlaying();}catch(IllegalStateException e){return false;}}
    private void resume(){try{userStopped=false;if(player!=null&&prepared){if(!safePlaying())player.start();updateSession(PlaybackStateCompat.STATE_PLAYING);updateNotification();publish("playing");}else if(!queue.isEmpty())playCurrent();else{restoreQueue();if(!queue.isEmpty())playCurrent();else publish("stopped");}}catch(Exception e){fail("No se pudo reanudar la reproducción.",true);}}
    private void pause(){try{if(player!=null&&prepared&&safePlaying())player.pause();updateSession(PlaybackStateCompat.STATE_PAUSED);if(!queue.isEmpty())updateNotification();publish("paused");}catch(Exception e){fail("No se pudo pausar el audio.",false);}}
    private void skip(int delta){if(queue.isEmpty())return;index=(index+delta+queue.size())%queue.size();failedSkips=0;persistQueue();playCurrent();}
    private void fail(String msg,boolean tryNext){error=msg;releasePlayer();prepared=false;updateSession(PlaybackStateCompat.STATE_ERROR);publish("error");if(tryNext&&queue.size()>1&&failedSkips<queue.size()-1){failedSkips++;index=(index+1)%queue.size();persistQueue();publish("recovering");playCurrent();}else if(!queue.isEmpty()){updateNotification();}}
    private void stopPlayback(){userStopped=true;releasePlayer();prepared=false;updateSession(PlaybackStateCompat.STATE_STOPPED);publish("stopped");getSharedPreferences(PREFS,MODE_PRIVATE).edit().putBoolean("user_stopped",true).apply();if(foreground){stopForeground(true);foreground=false;}stopSelf();}
    private void releasePlayer(){if(player!=null){try{player.setOnPreparedListener(null);player.setOnCompletionListener(null);player.setOnErrorListener(null);player.reset();}catch(Exception ignored){}try{player.release();}catch(Exception ignored){}player=null;}prepared=false;}
    private void updateSession(int state){if(session==null)return;TrackData t=queue.isEmpty()?null:queue.get(Math.max(0,Math.min(index,queue.size()-1)));if(t!=null){MediaMetadataCompat.Builder mb=new MediaMetadataCompat.Builder().putString(MediaMetadataCompat.METADATA_KEY_TITLE,t.title).putString(MediaMetadataCompat.METADATA_KEY_ARTIST,t.artist).putString(MediaMetadataCompat.METADATA_KEY_ALBUM,t.source);try{if(player!=null&&prepared)mb.putLong(MediaMetadataCompat.METADATA_KEY_DURATION,player.getDuration());}catch(Exception ignored){}session.setMetadata(mb.build());}long actions=PlaybackStateCompat.ACTION_PLAY|PlaybackStateCompat.ACTION_PAUSE|PlaybackStateCompat.ACTION_PLAY_PAUSE|PlaybackStateCompat.ACTION_SKIP_TO_NEXT|PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS|PlaybackStateCompat.ACTION_STOP|PlaybackStateCompat.ACTION_SEEK_TO;long pos=0;try{if(player!=null&&prepared)pos=player.getCurrentPosition();}catch(Exception ignored){}session.setPlaybackState(new PlaybackStateCompat.Builder().setActions(actions).setState(state,pos,state==PlaybackStateCompat.STATE_PLAYING?1f:0f).build());}
    private Notification notification(TrackData t,boolean playing,Bitmap art){Intent open=new Intent(this,MainActivity.class);PendingIntent content=PendingIntent.getActivity(this,1,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);PendingIntent prev=actionIntent(ACTION_PREV,2),toggle=actionIntent(ACTION_TOGGLE,3),next=actionIntent(ACTION_NEXT,4),stop=actionIntent(ACTION_STOP,5);return new NotificationCompat.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.ic_media_play).setContentTitle(t==null?"Sonora":t.title).setContentText(t==null?"Reproductor":t.artist+" • "+t.source).setLargeIcon(art==null?makeArtwork():art).setContentIntent(content).setOnlyAlertOnce(true).setOngoing(playing).setVisibility(NotificationCompat.VISIBILITY_PUBLIC).addAction(android.R.drawable.ic_media_previous,"Anterior",prev).addAction(playing?android.R.drawable.ic_media_pause:android.R.drawable.ic_media_play,playing?"Pausar":"Reproducir",toggle).addAction(android.R.drawable.ic_media_next,"Siguiente",next).setStyle(new androidx.media.app.NotificationCompat.MediaStyle().setMediaSession(session.getSessionToken()).setShowActionsInCompactView(0,1,2).setShowCancelButton(true).setCancelButtonIntent(stop)).build();}
    private PendingIntent actionIntent(String action,int code){Intent i=new Intent(this,MediaPlaybackService.class);i.setAction(action);return PendingIntent.getService(this,code,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
    private void ensureForeground(Notification n){if(Build.VERSION.SDK_INT>=29)startForeground(NOTIFICATION_ID,n,android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);else startForeground(NOTIFICATION_ID,n);foreground=true;}
    private void updateNotification(){if(!queue.isEmpty()){TrackData t=queue.get(index);Notification n=notification(t,player!=null&&prepared&&safePlaying(),null);if(foreground){((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(NOTIFICATION_ID,n);}else ensureForeground(n);}}
    private void updateNotificationWithArtwork(TrackData t){updateNotification();if(!validHttps(t.art))return;final String url=t.art;artworkExecutor.execute(()->{Bitmap bitmap=downloadArtwork(url);if(bitmap!=null&&!destroyed){mainHandler.post(()->{if(destroyed||queue.isEmpty()||index<0||index>=queue.size()||!queue.get(index).key().equals(t.key()))return;Notification n=notification(t,player!=null&&prepared&&safePlaying(),bitmap);((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(NOTIFICATION_ID,n);});}});}
    private Bitmap downloadArtwork(String url){HttpURLConnection c=null;try{URL u=new URL(url);c=(HttpURLConnection)u.openConnection();c.setConnectTimeout(4000);c.setReadTimeout(4000);c.setInstanceFollowRedirects(false);c.setRequestProperty("User-Agent","SonoraMusic/1.4");if(c.getResponseCode()!=200)return null;long len=c.getContentLengthLong();if(len>2*1024*1024)return null;try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buf=new byte[4096];int n,total=0;while((n=in.read(buf))!=-1){total+=n;if(total>2*1024*1024)return null;out.write(buf,0,n);}byte[] bytes=out.toByteArray();BitmapFactory.Options opts=new BitmapFactory.Options();opts.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(bytes,0,bytes.length,opts);if(opts.outWidth<=0||opts.outHeight<=0||opts.outWidth>6000||opts.outHeight>6000)return null;opts.inJustDecodeBounds=false;opts.inSampleSize=Math.max(1,Math.max(opts.outWidth,opts.outHeight)/512);return BitmapFactory.decodeByteArray(bytes,0,bytes.length,opts);}}catch(Exception ignored){return null;}finally{if(c!=null)c.disconnect();}}
    private void createChannel(){if(Build.VERSION.SDK_INT>=26){NotificationChannel c=new NotificationChannel(CHANNEL,"Reproducción de Sonora",NotificationManager.IMPORTANCE_LOW);c.setDescription("Controles de reproducción musical");c.setShowBadge(false);((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(c);}}
    private Bitmap makeArtwork(){Bitmap b=Bitmap.createBitmap(192,192,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);c.drawColor(Color.rgb(43,31,89));Paint p=new Paint(3);p.setColor(Color.rgb(141,91,255));c.drawCircle(96,96,64,p);p.setColor(Color.rgb(89,241,204));p.setTextSize(66);p.setTextAlign(Paint.Align.CENTER);c.drawText("♫",96,119,p);return b;}
    private void publish(String state){Intent i=new Intent(BROADCAST);i.setPackage(getPackageName());i.putExtra("state",state);i.putExtra("index",index);if(!queue.isEmpty()){TrackData t=queue.get(index);i.putExtra("title",t.title);i.putExtra("artist",t.artist);i.putExtra("source",t.source);}i.putExtra("error",error);sendBroadcast(i);}
    private void persistQueue(){try{org.json.JSONArray a=new org.json.JSONArray();for(TrackData t:queue){org.json.JSONObject o=new org.json.JSONObject();o.put("title",t.title);o.put("artist",t.artist);o.put("uri",t.uri);o.put("url",t.url);o.put("source",t.source);o.put("art",t.art);a.put(o);}getSharedPreferences(PREFS,MODE_PRIVATE).edit().putString(QUEUE_KEY,a.toString()).putInt(INDEX_KEY,index).putBoolean("user_stopped",userStopped).apply();}catch(Exception ignored){}}
    private void restoreQueue(){try{android.content.SharedPreferences p=getSharedPreferences(PREFS,MODE_PRIVATE);userStopped=p.getBoolean("user_stopped",false);if(queue.isEmpty()){readQueue(p.getString(QUEUE_KEY,"[]"));index=queue.isEmpty()?0:Math.max(0,Math.min(p.getInt(INDEX_KEY,0),queue.size()-1));}}catch(Exception ignored){}}
    @Override public void onTaskRemoved(Intent rootIntent){if(userStopped||queue.isEmpty())stopPlayback();else persistQueue();}
    @Override public void onDestroy(){destroyed=true;mainHandler.removeCallbacksAndMessages(null);releasePlayer();artworkExecutor.shutdownNow();if(session!=null){session.setActive(false);session.release();}super.onDestroy();}
    @Override public IBinder onBind(Intent intent){return null;}
}
