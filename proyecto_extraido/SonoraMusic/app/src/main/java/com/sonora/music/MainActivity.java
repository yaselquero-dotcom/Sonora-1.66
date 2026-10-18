package com.sonora.music;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap;
import android.util.LruCache;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.os.Build;
import android.net.Uri;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.*;
import org.json.*;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    LinearLayout root, content, tracksList, playerPanel, tabs;
    final ArrayList<Button> navButtons = new ArrayList<>();
    final ArrayList<String> navKeys = new ArrayList<>();
    int activePlaylist = -1;
    TextView nowTitle, nowSub, playButton, status;
    EditText searchBox;
    MediaPlayer player;
    ArrayList<Track> library = new ArrayList<>(), onlineResults = new ArrayList<>();
    ArrayList<Playlist> playlists = new ArrayList<>();
    int current = -1;
    boolean playing = false;
    String page = "home";
    final ExecutorService network = Executors.newSingleThreadExecutor();
    final int bg=Color.rgb(11,14,31), panel=Color.rgb(27,32,59), purple=Color.rgb(141,91,255), mint=Color.rgb(89,241,204), muted=0xffaab2d2;
    static final String JAMENDO_CLIENT_ID = "YOUR_JAMENDO_CLIENT_ID"; // Set this in MainActivity.java to enable licensed catalog search.
    private static final LruCache<String, Bitmap> COVER_CACHE = new LruCache<String, Bitmap>(6 * 1024 * 1024) { @Override protected int sizeOf(String key, Bitmap value) { return value.getByteCount(); } };
    static class Playlist { String name; ArrayList<String> trackKeys = new ArrayList<>(); Playlist(String n){name=n;} }
    static class Track {
        String title, artist, url, source, art; Uri uri; boolean favorite;
        Track(String t,String a,Uri u,String s,String src){this(t,a,u,s,src,"");}
        Track(String t,String a,Uri u,String s,String src,String art){title=t;artist=a;uri=u;url=s;source=src;this.art=art==null?"":art;}
    }
    private final BroadcastReceiver playbackReceiver=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){String state=i.getStringExtra("state");String title=i.getStringExtra("title");String artist=i.getStringExtra("artist");String source=i.getStringExtra("source");if(title!=null&&nowTitle!=null){nowTitle.setText(title);nowSub.setText((artist==null?"":artist)+"  •  "+(source==null?"SONORA":source));}current=i.getIntExtra("index",current);playing="playing".equals(state);if(playButton!=null)playButton.setText(playing?"Ⅱ":"loading".equals(state)?"…":"▶");if("error".equals(state)){String err=i.getStringExtra("error");Toast.makeText(MainActivity.this,err==null?"Error de reproducción":err,Toast.LENGTH_LONG).show();if(playButton!=null)playButton.setText("▶");}else if("recovering".equals(state)){Toast.makeText(MainActivity.this,"La pista falló; intentando la siguiente…",Toast.LENGTH_SHORT).show();}}};
    @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(bg);getWindow().setNavigationBarColor(bg);loadLibrary();loadPlaylists();showApp();IntentFilter f=new IntentFilter(MediaPlaybackService.BROADCAST);if(Build.VERSION.SDK_INT>=33)registerReceiver(playbackReceiver,f,Context.RECEIVER_NOT_EXPORTED);else registerReceiver(playbackReceiver,f);if(Build.VERSION.SDK_INT>=33&&checkSelfPermission("android.permission.POST_NOTIFICATIONS")!=android.content.pm.PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"},31);} }
    GradientDrawable shape(int c,int r){GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(r);return d;}
    TextView txt(String s,int z,int c,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(c);if(bold)t.setTypeface(null,Typeface.BOLD);return t;}
    LinearLayout.LayoutParams lp(int w,int h,int top,int bottom){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w,h);p.setMargins(0,top,0,bottom);return p;}
    Button btn(String s,int c,int tc){Button b=new Button(this);b.setText(s);b.setTextColor(tc);b.setTextSize(14);b.setAllCaps(false);b.setTypeface(null,Typeface.BOLD);b.setBackground(shape(c,18));return b;}
    void showApp(){
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setClipToPadding(false);
        root=new LinearLayout(this);root.setOrientation(1);root.setPadding(18,16,18,24);
        root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xff050b1c,0xff101332,0xff090d20}));scroll.addView(root);setContentView(scroll);
        LinearLayout brandRow=new LinearLayout(this);brandRow.setGravity(Gravity.CENTER_VERTICAL);root.addView(brandRow,lp(-1,-2,0,8));
        LinearLayout brandText=new LinearLayout(this);brandText.setOrientation(1);brandRow.addView(brandText,new LinearLayout.LayoutParams(0,-2,1));
        brandText.addView(txt("S O N O R A",22,Color.WHITE,true));
        brandText.addView(txt("TU MÚSICA, EN TODAS PARTES",10,0xff9ba8d1,true),lp(-1,-2,2,0));
        TextView statusIcon=txt("♫",25,mint,true);statusIcon.setGravity(Gravity.CENTER);statusIcon.setBackground(shape(0xff151e3e,18));brandRow.addView(statusIcon,new LinearLayout.LayoutParams(46,46));
        LinearLayout hero=new LinearLayout(this);hero.setOrientation(1);hero.setPadding(20,20,20,20);
        GradientDrawable heroBg=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xff35126f,0xff142a65,0xff073c54});heroBg.setCornerRadius(26);heroBg.setStroke(1,0xff286ce0);hero.setBackground(heroBg);root.addView(hero,lp(-1,-2,12,18));
        hero.addView(txt("TU ESPACIO MUSICAL",11,mint,true));
        TextView headline=txt("La música que\nva contigo.",30,Color.WHITE,true);headline.setPadding(0,8,0,5);hero.addView(headline);
        hero.addView(txt("Tus canciones, tus listas y tus emisoras en un solo lugar.",13,0xffd1d9f6,false));
        LinearLayout heroActions=new LinearLayout(this);heroActions.setGravity(Gravity.CENTER_VERTICAL);hero.addView(heroActions,lp(-1,-2,15,0));
        Button localBtn=btn("♫  Mi música",0xff6a35ee,Color.WHITE);heroActions.addView(localBtn,new LinearLayout.LayoutParams(0,44,1));localBtn.setOnClickListener(v->{page="library";renderPage();});
        Space gap=new Space(this);heroActions.addView(gap,new LinearLayout.LayoutParams(10,1));
        Button listBtn=btn("☷  Mis listas",0xff12294b,Color.WHITE);listBtn.setBackground(strokeShape(0xff12294b,16,0xff168be7,1));heroActions.addView(listBtn,new LinearLayout.LayoutParams(0,44,1));listBtn.setOnClickListener(v->{page="playlists";renderPage();});
        HorizontalScrollView navScroll=new HorizontalScrollView(this);navScroll.setHorizontalScrollBarEnabled(false);navScroll.setFillViewport(false);root.addView(navScroll,lp(-1,48,0,16));
        tabs=new LinearLayout(this);tabs.setOrientation(0);tabs.setGravity(Gravity.CENTER_VERTICAL);tabs.setPadding(2,2,2,2);navScroll.addView(tabs);
        navButtons.clear();navKeys.clear();
        addTab("⌂  Inicio","home");addTab("♫  Local","library");addTab("☷  Listas","playlists");addTab("◉  Radio","radio");addTab("⌕  Explorar","catalog");
        content=new LinearLayout(this);content.setOrientation(1);root.addView(content);
        playerPanel=new LinearLayout(this);playerPanel.setOrientation(1);playerPanel.setPadding(14,13,14,12);playerPanel.setBackground(strokeShape(0xff0d1732,22,0xff2151a0,1));root.addView(playerPanel,lp(-1,-2,20,0));
        LinearLayout playingRow=new LinearLayout(this);playingRow.setGravity(Gravity.CENTER_VERTICAL);playerPanel.addView(playingRow);
        FrameLayout miniCover=new FrameLayout(this);miniCover.setBackground(shape(0xff30206a,13));playingRow.addView(miniCover,new LinearLayout.LayoutParams(46,46));TextView coverGlyph=txt("♫",23,mint,true);coverGlyph.setGravity(Gravity.CENTER);miniCover.addView(coverGlyph,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout playingWords=new LinearLayout(this);playingWords.setOrientation(1);playingWords.setPadding(10,0,4,0);playingRow.addView(playingWords,new LinearLayout.LayoutParams(0,-2,1));
        playingWords.addView(txt("REPRODUCIENDO AHORA",9,mint,true));
        nowTitle=txt("Elige una canción",14,Color.WHITE,true);nowTitle.setMaxLines(1);nowTitle.setEllipsize(android.text.TextUtils.TruncateAt.END);playingWords.addView(nowTitle,lp(-1,-2,2,0));
        nowSub=txt("SONORA PLAYER",10,muted,false);nowSub.setMaxLines(1);nowSub.setEllipsize(android.text.TextUtils.TruncateAt.END);playingWords.addView(nowSub);
        playButton=txt("▶",23,Color.WHITE,true);playButton.setGravity(Gravity.CENTER);playButton.setBackground(shape(0xff6337e9,18));playingRow.addView(playButton,new LinearLayout.LayoutParams(46,46));playButton.setOnClickListener(v->toggle());
        LinearLayout controls=new LinearLayout(this);controls.setGravity(Gravity.CENTER);playerPanel.addView(controls,lp(-1,38,7,0));
        TextView prev=txt("⏮",18,0xffb9c8ef,true);prev.setGravity(Gravity.CENTER);controls.addView(prev,new LinearLayout.LayoutParams(54,38));prev.setOnClickListener(v->serviceAction(MediaPlaybackService.ACTION_PREV));
        TextView next=txt("⏭",18,0xffb9c8ef,true);next.setGravity(Gravity.CENTER);controls.addView(next,new LinearLayout.LayoutParams(54,38));next.setOnClickListener(v->serviceAction(MediaPlaybackService.ACTION_NEXT));
        Button queueButton=btn("Ver cola de reproducción  ›",0xff111e3b,Color.WHITE);queueButton.setTextSize(11);controls.addView(queueButton,new LinearLayout.LayoutParams(0,34,1));queueButton.setOnClickListener(v->showQueueDialog());
        renderPage();
    }
    GradientDrawable strokeShape(int color,int radius,int stroke,int width){GradientDrawable d=shape(color,radius);d.setStroke(width,stroke);return d;}
    void addTab(String label,String key){
        Button b=btn(label,0xff0c1530,0xffb9c6e9);b.setTextSize(11);b.setPadding(14,0,14,0);b.setMinWidth(0);b.setMinimumWidth(0);b.setBackground(shape(0xff0c1530,16));
        tabs.addView(b,new LinearLayout.LayoutParams(-2,-1));navButtons.add(b);navKeys.add(key);
        b.setOnClickListener(v->{page=key;activePlaylist=-1;renderPage();});
    }
    void updateNavigation(){
        for(int i=0;i<navButtons.size();i++){String key=navKeys.get(i);boolean selected=page.equals(key)||(page.equals("playlistDetail")&&key.equals("playlists"));Button b=navButtons.get(i);b.setBackground(selected?strokeShape(0xff073d55,16,0xff00d9e8,1):shape(0xff0c1530,16));b.setTextColor(selected?mint:0xffb9c6e9);}
    }
    void renderPage(){
        updateNavigation();
        content.removeAllViews();
        if(page.equals("home")){
            content.addView(txt("Escucha a tu manera",21,Color.WHITE,true));
            content.addView(txt("Elige por dónde empezar.",14,muted,false),lp(-1,-2,4,16));
            addAction("♫","Tu música","Reproduce archivos guardados en el teléfono",()->pickLocal());
            addAction("◉","Radio en vivo","Explora emisoras por género o país",()->{page="radio";renderPage();});
            addAction("⌕","Catálogo musical","Busca música con licencia en Jamendo",()->{page="catalog";renderPage();});
            content.addView(txt("Tus favoritos",17,Color.WHITE,true),lp(-1,-2,18,8));
            boolean anyFav=false; for(int i=0;i<library.size();i++) if(library.get(i).favorite){addTrackRow(library.get(i),i,false);anyFav=true;}
            if(!anyFav)content.addView(txt("Marca canciones con ☆ para tenerlas a mano.",12,muted,false),lp(-1,-2,2,8));
            content.addView(txt("Añadido recientemente",17,Color.WHITE,true),lp(-1,-2,14,8));
            if(library.isEmpty())content.addView(txt("Tu biblioteca está esperando su primera canción.",13,muted,false));
            else for(int i=Math.max(0,library.size()-3);i<library.size();i++)addTrackRow(library.get(i),i,false);
            addAction("⚙","Privacidad y ajustes","Gestiona tus datos guardados en este dispositivo",()->showSettings());
        } else if(page.equals("library")){
            content.addView(txt("Música local",25,Color.WHITE,true));
            content.addView(txt("Tu colección guardada en este dispositivo.",13,muted,false),lp(-1,-2,4,14));
            LinearLayout actions=new LinearLayout(this);actions.setGravity(Gravity.CENTER_VERTICAL);content.addView(actions,lp(-1,48,0,12));
            Button add=btn("＋  Importar canciones",purple,Color.WHITE);actions.addView(add,new LinearLayout.LayoutParams(0,-1,1));add.setOnClickListener(v->pickLocal());
            Button playlistsBtn=btn("☷  Listas",0xff102746,Color.WHITE);playlistsBtn.setBackground(strokeShape(0xff102746,16,0xff1a9bd7,1));playlistsBtn.setTextSize(12);actions.addView(playlistsBtn,lp(94,-1,0,0));playlistsBtn.setOnClickListener(v->{page="playlists";renderPage();});
            if(library.isEmpty()){
                LinearLayout empty=new LinearLayout(this);empty.setOrientation(1);empty.setGravity(Gravity.CENTER);empty.setPadding(22,28,22,28);empty.setBackground(strokeShape(0xff0c1730,22,0xff183c70,1));content.addView(empty,lp(-1,-2,0,0));
                empty.addView(txt("♫",40,mint,true));empty.addView(txt("Tu biblioteca empieza aquí",18,Color.WHITE,true),lp(-1,-2,10,6));empty.addView(txt("Importa archivos de audio desde tu teléfono para escucharlos y crear listas propias.",13,muted,false));Button importEmpty=btn("Seleccionar música",purple,Color.WHITE);empty.addView(importEmpty,lp(-1,48,18,0));importEmpty.setOnClickListener(v->pickLocal());
            } else {
                content.addView(txt(library.size()+" canciones · "+countFavorites()+" favoritas",11,mint,true),lp(-1,-2,0,10));
                for(int i=0;i<library.size();i++)addTrackRow(library.get(i),i,false);
            }
        } else if(page.equals("playlists")) renderPlaylists();
        else if(page.equals("playlistDetail")) renderPlaylistDetail();
        else if(page.equals("radio")) renderRadio();
        else if(page.equals("settings")) showSettingsContent();
        else renderCatalog();
    }
    int countFavorites(){int n=0;for(Track t:library)if(t.favorite)n++;return n;}
    String trackKey(Track t){return t.uri!=null?t.uri.toString():(t.url==null?"":t.url);}
    Track findTrack(String key){for(Track t:library)if(trackKey(t).equals(key))return t;return null;}
    ArrayList<Track> localTracks(){ArrayList<Track> out=new ArrayList<>();for(Track t:library)if("LOCAL".equals(t.source)&&t.uri!=null)out.add(t);return out;}
    void savePlaylists(){
        try{JSONArray all=new JSONArray();for(Playlist p:playlists){JSONObject o=new JSONObject();o.put("name",p.name);JSONArray tracks=new JSONArray();for(String key:p.trackKeys)tracks.put(key);o.put("tracks",tracks);all.put(o);}getSharedPreferences("sonora_playlists",MODE_PRIVATE).edit().putString("playlists",all.toString()).apply();}catch(Exception ignored){}
    }
    void loadPlaylists(){
        try{JSONArray all=new JSONArray(getSharedPreferences("sonora_playlists",MODE_PRIVATE).getString("playlists","[]"));for(int i=0;i<all.length();i++){JSONObject o=all.getJSONObject(i);Playlist p=new Playlist(o.optString("name","Mi lista"));JSONArray a=o.optJSONArray("tracks");if(a!=null)for(int j=0;j<a.length();j++){String key=a.optString(j,"");if(!key.isEmpty())p.trackKeys.add(key);}playlists.add(p);}}catch(Exception ignored){}
    }
    void renderPlaylists(){
        content.addView(txt("Listas de reproducción",24,Color.WHITE,true));
        content.addView(txt("Organiza tu música local a tu manera.",13,muted,false),lp(-1,-2,4,14));
        Button create=btn("＋  Crear lista nueva",purple,Color.WHITE);content.addView(create,lp(-1,50,0,14));create.setOnClickListener(v->createPlaylistDialog());
        ArrayList<Track> locals=localTracks();
        if(playlists.isEmpty()){
            LinearLayout empty=new LinearLayout(this);empty.setOrientation(1);empty.setGravity(Gravity.CENTER);empty.setPadding(20,26,20,26);empty.setBackground(strokeShape(0xff0b1730,22,0xff1b3a70,1));content.addView(empty,lp(-1,-2,0,0));
            empty.addView(txt("☷",38,mint,true));empty.addView(txt("Tus listas empiezan aquí",18,Color.WHITE,true),lp(-1,-2,8,5));empty.addView(txt("Crea listas para entrenar, viajar, relajarte o guardar tus favoritas.",13,muted,false));
        }else{
            for(int i=0;i<playlists.size();i++){
                final int index=i;Playlist p=playlists.get(i);int valid=0;for(String key:p.trackKeys)if(findTrack(key)!=null)valid++;
                LinearLayout card=new LinearLayout(this);card.setGravity(Gravity.CENTER_VERTICAL);card.setPadding(13,12,12,12);card.setBackground(strokeShape(0xff0b1630,19,0xff203e78,1));content.addView(card,lp(-1,-2,0,9));
                TextView art=txt("♫",25,mint,true);art.setGravity(Gravity.CENTER);art.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xff47209a,0xff07546b}));card.addView(art,new LinearLayout.LayoutParams(54,54));
                LinearLayout words=new LinearLayout(this);words.setOrientation(1);words.setPadding(11,0,5,0);card.addView(words,new LinearLayout.LayoutParams(0,-2,1));words.addView(txt(p.name,15,Color.WHITE,true));words.addView(txt(valid+" canciones",11,muted,false),lp(-1,-2,4,0));
                Button play=btn("▶",0xff133d5c,Color.WHITE);play.setBackground(strokeShape(0xff12334f,15,0xff00c9d7,1));card.addView(play,new LinearLayout.LayoutParams(42,42));play.setOnClickListener(v->playPlaylist(index));
                card.setOnClickListener(v->{activePlaylist=index;page="playlistDetail";renderPage();});
            }
        }
        content.addView(txt("Consejo",14,mint,true),lp(-1,-2,18,5));
        content.addView(txt(locals.isEmpty()?"Importa primero canciones locales desde la pestaña Local.":"Puedes añadir hasta todas tus canciones locales a cada lista.",12,muted,false));
        if(locals.isEmpty()){Button importBtn=btn("Importar música local",0xff102746,Color.WHITE);content.addView(importBtn,lp(-1,46,10,0));importBtn.setOnClickListener(v->{page="library";renderPage();pickLocal();});}
    }
    void createPlaylistDialog(){
        EditText input=new EditText(this);input.setSingleLine(true);input.setHint("Nombre de la lista");input.setTextColor(Color.WHITE);input.setHintTextColor(muted);input.setPadding(16,12,16,12);input.setBackground(strokeShape(0xff101b37,14,0xff2366aa,1));
        LinearLayout wrap=new LinearLayout(this);wrap.setPadding(20,8,20,0);wrap.addView(input,new LinearLayout.LayoutParams(-1,52));
        new AlertDialog.Builder(this).setTitle("Crear lista").setView(wrap).setNegativeButton("Cancelar",null).setPositiveButton("Crear",(d,w)->{String name=input.getText().toString().trim();if(name.isEmpty()){Toast.makeText(this,"Escribe un nombre para la lista.",Toast.LENGTH_SHORT).show();return;}Playlist p=new Playlist(name);playlists.add(p);savePlaylists();activePlaylist=playlists.size()-1;page="playlistDetail";renderPage();}).show();
    }
    void renderPlaylistDetail(){
        if(activePlaylist<0||activePlaylist>=playlists.size()){page="playlists";renderPage();return;}
        Playlist p=playlists.get(activePlaylist);
        LinearLayout heading=new LinearLayout(this);heading.setGravity(Gravity.CENTER_VERTICAL);content.addView(heading,lp(-1,-2,0,12));
        Button back=btn("‹",0xff101e3c,Color.WHITE);back.setTextSize(24);heading.addView(back,new LinearLayout.LayoutParams(46,46));back.setOnClickListener(v->{page="playlists";renderPage();});
        LinearLayout names=new LinearLayout(this);names.setOrientation(1);names.setPadding(11,0,0,0);heading.addView(names,new LinearLayout.LayoutParams(0,-2,1));names.addView(txt("LISTA DE REPRODUCCIÓN",10,mint,true));names.addView(txt(p.name,22,Color.WHITE,true));
        Button more=btn("⋮",0xff101e3c,Color.WHITE);more.setTextSize(20);heading.addView(more,new LinearLayout.LayoutParams(42,44));more.setOnClickListener(v->new AlertDialog.Builder(this).setTitle(p.name).setItems(new String[]{"Cambiar nombre","Eliminar lista"},(d,which)->{if(which==0)renamePlaylistDialog(activePlaylist);else confirmDeletePlaylist(activePlaylist);}).show());
        int count=0;for(String key:p.trackKeys)if(findTrack(key)!=null)count++;
        content.addView(txt(count+" canciones · guardada en este dispositivo",12,muted,false),lp(-1,-2,0,12));
        LinearLayout buttons=new LinearLayout(this);content.addView(buttons,lp(-1,48,0,14));
        Button playAll=btn("▶  Reproducir",purple,Color.WHITE);buttons.addView(playAll,new LinearLayout.LayoutParams(0,-1,1));playAll.setOnClickListener(v->playPlaylist(activePlaylist));
        Button add=btn("＋  Añadir",0xff102746,Color.WHITE);add.setBackground(strokeShape(0xff102746,16,0xff158fca,1));buttons.addView(add,lp(105,-1,8,0));add.setOnClickListener(v->chooseTracksForPlaylist(activePlaylist));
        boolean any=false;for(int i=0;i<p.trackKeys.size();i++){Track t=findTrack(p.trackKeys.get(i));if(t==null)continue;any=true;final String key=p.trackKeys.get(i);LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(10,8,8,8);row.setBackground(strokeShape(0xff0a142b,15,0xff1a315c,1));content.addView(row,lp(-1,-2,0,7));
            TextView glyph=txt("♫",21,mint,true);glyph.setGravity(Gravity.CENTER);glyph.setBackground(shape(0xff192b52,12));row.addView(glyph,new LinearLayout.LayoutParams(44,44));
            LinearLayout text=new LinearLayout(this);text.setOrientation(1);text.setPadding(10,0,4,0);row.addView(text,new LinearLayout.LayoutParams(0,-2,1));text.addView(txt(t.title,13,Color.WHITE,true));text.addView(txt(t.artist,11,muted,false));
            TextView remove=txt("−",24,0xffff8eae,true);remove.setGravity(Gravity.CENTER);row.addView(remove,new LinearLayout.LayoutParams(36,42));remove.setContentDescription("Quitar de la lista");remove.setOnClickListener(v->{p.trackKeys.remove(key);savePlaylists();renderPage();});
            row.setOnClickListener(v->playPlaylistTrack(activePlaylist,key));
        }
        if(!any){LinearLayout empty=new LinearLayout(this);empty.setOrientation(1);empty.setGravity(Gravity.CENTER);empty.setPadding(20,25,20,25);empty.setBackground(strokeShape(0xff0b1730,20,0xff1a3d72,1));content.addView(empty,lp(-1,-2,0,0));empty.addView(txt("Esta lista está vacía",17,Color.WHITE,true));empty.addView(txt("Añade canciones de tu biblioteca local para empezar.",12,muted,false),lp(-1,-2,6,12));Button addEmpty=btn("＋  Añadir canciones",purple,Color.WHITE);empty.addView(addEmpty,lp(-1,46,0,0));addEmpty.setOnClickListener(v->chooseTracksForPlaylist(activePlaylist));}
    }
    void chooseTracksForPlaylist(int playlistIndex){
        if(playlistIndex<0||playlistIndex>=playlists.size())return;ArrayList<Track> locals=localTracks();
        if(locals.isEmpty()){new AlertDialog.Builder(this).setTitle("No hay música local").setMessage("Importa canciones desde la pestaña Local antes de añadirlas a una lista.").setPositiveButton("Ir a música local",(d,w)->{page="library";renderPage();pickLocal();}).setNegativeButton("Cancelar",null).show();return;}
        Playlist p=playlists.get(playlistIndex);String[] names=new String[locals.size()];boolean[] checked=new boolean[locals.size()];for(int i=0;i<locals.size();i++){Track t=locals.get(i);names[i]=t.title+" — "+t.artist;checked[i]=p.trackKeys.contains(trackKey(t));}
        new AlertDialog.Builder(this).setTitle("Añadir canciones").setMultiChoiceItems(names,checked,(d,which,isChecked)->checked[which]=isChecked).setNegativeButton("Cancelar",null).setPositiveButton("Guardar",(d,w)->{p.trackKeys.clear();for(int i=0;i<locals.size();i++)if(checked[i])p.trackKeys.add(trackKey(locals.get(i)));savePlaylists();renderPage();Toast.makeText(this,"Lista actualizada",Toast.LENGTH_SHORT).show();}).show();
    }
    void playPlaylist(int index){if(index<0||index>=playlists.size())return;Playlist p=playlists.get(index);ArrayList<Track> tracks=new ArrayList<>();for(String key:p.trackKeys){Track t=findTrack(key);if(t!=null)tracks.add(t);}if(tracks.isEmpty()){Toast.makeText(this,"Añade canciones a esta lista primero.",Toast.LENGTH_SHORT).show();return;}playTrackQueue(tracks,0);}
    void playPlaylistTrack(int index,String key){if(index<0||index>=playlists.size())return;Playlist p=playlists.get(index);ArrayList<Track> tracks=new ArrayList<>();int selected=0;for(String k:p.trackKeys){Track t=findTrack(k);if(t!=null){if(k.equals(key))selected=tracks.size();tracks.add(t);}}if(!tracks.isEmpty())playTrackQueue(tracks,selected);}
    void playTrackQueue(ArrayList<Track> tracks,int selected){
        if(tracks==null||tracks.isEmpty())return;
        try{JSONArray arr=new JSONArray();for(Track x:tracks){JSONObject o=new JSONObject();o.put("title",x.title);o.put("artist",x.artist);o.put("uri",x.uri==null?"":x.uri.toString());o.put("url",x.url==null?"":x.url);o.put("source",x.source);o.put("art",x.art==null?"":x.art);arr.put(o);}
            Track t=tracks.get(Math.max(0,Math.min(selected,tracks.size()-1)));current=selected;nowTitle.setText(t.title);nowSub.setText(t.artist+"  •  "+t.source);playButton.setText("…");Intent i=new Intent(this,MediaPlaybackService.class);i.setAction(MediaPlaybackService.ACTION_PLAY);i.putExtra("queue",arr.toString());i.putExtra("index",selected);if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);
        }catch(Exception e){Toast.makeText(this,"No se pudo iniciar la lista.",Toast.LENGTH_LONG).show();}
    }
    void renamePlaylistDialog(int index){if(index<0||index>=playlists.size())return;EditText input=new EditText(this);input.setSingleLine(true);input.setText(playlists.get(index).name);input.setTextColor(Color.WHITE);new AlertDialog.Builder(this).setTitle("Cambiar nombre").setView(input).setNegativeButton("Cancelar",null).setPositiveButton("Guardar",(d,w)->{String n=input.getText().toString().trim();if(!n.isEmpty()){playlists.get(index).name=n;savePlaylists();renderPage();}}).show();}
    void confirmDeletePlaylist(int index){if(index<0||index>=playlists.size())return;new AlertDialog.Builder(this).setTitle("Eliminar lista").setMessage("Se eliminará la lista, pero no los archivos de música originales.").setNegativeButton("Cancelar",null).setPositiveButton("Eliminar",(d,w)->{playlists.remove(index);savePlaylists();activePlaylist=-1;page="playlists";renderPage();}).show();}
    void showSettings(){page="settings";renderPage();}
    void showSettingsContent(){
        content.addView(txt("Privacidad y ajustes",24,Color.WHITE,true));
        content.addView(txt("Sonora no necesita una cuenta. La biblioteca y los favoritos se guardan localmente en este dispositivo. Las búsquedas de radio y catálogo sí requieren conexión a sus servicios.",14,muted,false),lp(-1,-2,8,16));
        LinearLayout card=new LinearLayout(this);card.setOrientation(1);card.setPadding(16,16,16,16);card.setBackground(shape(panel,18));content.addView(card,lp(-1,-2,0,12));
        card.addView(txt("Datos almacenados",16,Color.WHITE,true));card.addView(txt(library.size()+" elementos en tu biblioteca",13,muted,false),lp(-1,-2,6,12));
        Button clear=btn("Borrar biblioteca y favoritos",0xff693548,Color.WHITE);card.addView(clear,lp(-1,48,0,0));clear.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Borrar datos musicales").setMessage("Se eliminarán de Sonora las canciones guardadas y los favoritos. No se borrarán los archivos originales del teléfono.").setNegativeButton("Cancelar",(d,w)->{}).setPositiveButton("Borrar",(d,w)->{library.clear();saveLibrary();release();nowTitle.setText("Elige una canción");nowSub.setText("SONORA PLAYER");playButton.setText("▶");page="home";renderPage();Toast.makeText(this,"Biblioteca borrada",Toast.LENGTH_SHORT).show();}).show());
        Button back=btn("← Volver al inicio",0xff303454,Color.WHITE);content.addView(back,lp(-1,46,4,0));back.setOnClickListener(v->{page="home";renderPage();});
    }
    void addAction(String icon,String title,String desc,Runnable action){LinearLayout card=new LinearLayout(this);card.setGravity(Gravity.CENTER_VERTICAL);card.setPadding(15,13,15,13);card.setBackground(strokeShape(0xff0d1832,19,0xff1a3769,1));content.addView(card,lp(-1,-2,0,10));TextView ico=txt(icon,24,mint,true);ico.setGravity(Gravity.CENTER);card.addView(ico,new LinearLayout.LayoutParams(48,48));LinearLayout words=new LinearLayout(this);words.setOrientation(1);words.setPadding(10,0,0,0);card.addView(words,new LinearLayout.LayoutParams(0,-2,1));words.addView(txt(title,15,Color.WHITE,true));words.addView(txt(desc,12,muted,false));card.setOnClickListener(v->action.run());}
    void addTrackRow(Track t,int idx,boolean addToLibrary){
        LinearLayout item=new LinearLayout(this);item.setGravity(Gravity.CENTER_VERTICAL);item.setPadding(12,10,10,10);item.setBackground(strokeShape(0xff0a142b,17,0xff1a315c,1));content.addView(item,lp(-1,-2,0,8));
        FrameLayout coverFrame=new FrameLayout(this);coverFrame.setBackground(shape(0xff172b55,14));item.addView(coverFrame,new LinearLayout.LayoutParams(48,48));TextView cover=txt(t.source.equals("RADIO")?"◉":"♫",22,t.source.equals("RADIO")?0xff36d8ef:mint,true);cover.setGravity(Gravity.CENTER);coverFrame.addView(cover,new FrameLayout.LayoutParams(-1,-1));if(t.art!=null&&t.art.startsWith("https://")){ImageView artView=new ImageView(this);artView.setScaleType(ImageView.ScaleType.CENTER_CROP);coverFrame.addView(artView,new FrameLayout.LayoutParams(-1,-1));loadCover(t,artView,cover);}
        LinearLayout words=new LinearLayout(this);words.setOrientation(1);words.setPadding(10,0,4,0);item.addView(words,new LinearLayout.LayoutParams(0,-2,1));
        TextView name=txt(t.title,14,Color.WHITE,true);name.setMaxLines(1);name.setEllipsize(android.text.TextUtils.TruncateAt.END);words.addView(name);TextView artist=txt(t.artist,11,muted,false);artist.setMaxLines(1);artist.setEllipsize(android.text.TextUtils.TruncateAt.END);words.addView(artist);
        TextView fav=txt(t.favorite?"★":"☆",21,t.favorite?mint:muted,true);fav.setGravity(Gravity.CENTER);item.addView(fav,new LinearLayout.LayoutParams(34,42));fav.setContentDescription(t.favorite?"Quitar de favoritos":"Añadir a favoritos");
        TextView play=txt("▶",16,mint,true);play.setGravity(Gravity.CENTER);item.addView(play,new LinearLayout.LayoutParams(28,42));
        fav.setOnClickListener(v->{t.favorite=!t.favorite;if(addToLibrary&&!containsTrack(t))library.add(t);saveLibrary();renderPage();});
        item.setOnClickListener(v->{if(addToLibrary&&!containsTrack(t)){library.add(t);saveLibrary();Toast.makeText(this,"Añadido a biblioteca",Toast.LENGTH_SHORT).show();}playTrack(t);});
    }
    void loadCover(Track t,ImageView target,TextView fallback){
        final String imageUrl=t.art;if(imageUrl==null||!imageUrl.startsWith("https://"))return;
        Bitmap cached=COVER_CACHE.get(imageUrl);if(cached!=null){target.setImageBitmap(cached);fallback.setVisibility(View.GONE);return;}
        target.setTag(imageUrl);try{network.execute(()->{Bitmap bitmap=null;HttpURLConnection conn=null;try{URL parsed=new URL(imageUrl);if(!"https".equalsIgnoreCase(parsed.getProtocol()))return;conn=(HttpURLConnection)parsed.openConnection();conn.setConnectTimeout(4000);conn.setReadTimeout(4000);conn.setInstanceFollowRedirects(false);conn.setRequestProperty("User-Agent","SonoraMusic/1.4");if(conn.getResponseCode()==200){long length=conn.getContentLengthLong();if(length<=0||length<=2*1024*1024){try(InputStream in=conn.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[4096];int n,total=0;while((n=in.read(b))!=-1){total+=n;if(total>2*1024*1024)break;out.write(b,0,n);}if(total<=2*1024*1024){byte[] bytes=out.toByteArray();BitmapFactory.Options opts=new BitmapFactory.Options();opts.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(bytes,0,bytes.length,opts);if(opts.outWidth>0&&opts.outHeight>0&&opts.outWidth<=6000&&opts.outHeight<=6000){opts.inJustDecodeBounds=false;opts.inSampleSize=Math.max(1,Math.max(opts.outWidth,opts.outHeight)/512);bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.length,opts);}}}}}}catch(Exception ignored){}finally{if(conn!=null)conn.disconnect();}final Bitmap result=bitmap;if(result!=null){COVER_CACHE.put(imageUrl,result);runOnUiThread(()->{if(!isFinishing()&&imageUrl.equals(target.getTag())){target.setImageBitmap(result);fallback.setVisibility(View.GONE);}});}});}catch(java.util.concurrent.RejectedExecutionException ignored){}
    }
    void showQueueDialog(){if(library.isEmpty()){new AlertDialog.Builder(this).setTitle("Cola de reproducción").setMessage("Añade canciones a tu biblioteca para crear una cola.").setPositiveButton("Aceptar",null).show();return;}String[] items=new String[library.size()];for(int i=0;i<library.size();i++)items[i]=(i==current?"▶  ":"")+library.get(i).title+" — "+library.get(i).artist;new AlertDialog.Builder(this).setTitle("Cola de reproducción · "+library.size()).setItems(items,(dialog,which)->playTrack(library.get(which))).setNegativeButton("Cerrar",null).show();}
    boolean containsTrack(Track t){for(Track x:library){if((t.uri!=null&&t.uri.equals(x.uri))||(t.url!=null&&t.url.equals(x.url)))return true;}return false;}
    void saveLibrary(){try{JSONArray a=new JSONArray();for(Track t:library){JSONObject o=new JSONObject();o.put("title",t.title);o.put("artist",t.artist);o.put("uri",t.uri==null?"":t.uri.toString());o.put("url",t.url==null?"":t.url);o.put("source",t.source);o.put("art",t.art==null?"":t.art);o.put("favorite",t.favorite);a.put(o);}getSharedPreferences("sonora_library",MODE_PRIVATE).edit().putString("tracks",a.toString()).apply();}catch(Exception ignored){}}
    void loadLibrary(){try{String raw=getSharedPreferences("sonora_library",MODE_PRIVATE).getString("tracks","[]");JSONArray a=new JSONArray(raw);for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);String u=o.optString("uri","");String url=o.optString("url","");Track t=new Track(o.optString("title","Audio"),o.optString("artist","Desconocido"),u.isEmpty()?null:Uri.parse(u),url.isEmpty()?null:url,o.optString("source","LOCAL"),o.optString("art",""));t.favorite=o.optBoolean("favorite",false);if(t.uri!=null|| (t.url!=null&&t.url.startsWith("https://")))library.add(t);}}catch(Exception ignored){}}
    void renderRadio(){
        content.addView(txt("Radio en vivo",24,Color.WHITE,true));
        content.addView(txt("Miles de emisoras públicas para descubrir nuevos sonidos.",13,muted,false),lp(-1,-2,4,14));
        searchBox=new EditText(this);searchBox.setSingleLine(true);searchBox.setHint("Buscar emisora, género o país");searchBox.setTextColor(Color.WHITE);searchBox.setHintTextColor(muted);searchBox.setPadding(15,10,15,10);searchBox.setBackground(shape(panel,16));content.addView(searchBox,lp(-1,52,0,8));
        Button search=btn("Buscar emisoras",purple,Color.WHITE);content.addView(search,lp(-1,48,0,10));
        status=txt("Prueba: jazz, rock, chill, noticias…",12,muted,false);content.addView(status,lp(-1,-2,0,8));
        search.setOnClickListener(v->{hideKeyboard();String q=searchBox.getText().toString().trim();fetchRadio(q);});
        addRadioGenre("Chill & Lounge","chill");addRadioGenre("Jazz","jazz");addRadioGenre("Rock","rock");addRadioGenre("Clásica","classical");
    }
    void addRadioGenre(String title,String q){Button b=btn("♫  "+title,0xff303454,Color.WHITE);content.addView(b,lp(-1,46,0,7));b.setOnClickListener(v->{searchBox.setText(q);hideKeyboard();fetchRadio(q);});}
    void fetchRadio(String q){
        if(q==null||q.trim().isEmpty()){status.setText("Escribe una emisora, género o país para buscar.");return;}
        status.setText("Buscando emisoras…");
        network.execute(()->{ArrayList<Track> result=new ArrayList<>();String error=null;
            try{String query="https://all.api.radio-browser.info/json/stations/search?name="+URLEncoder.encode(q,"UTF-8")+"&limit=25&hidebroken=true&order=clickcount&reverse=true";
                HttpURLConnection c=(HttpURLConnection)new URL(query).openConnection();c.setConnectTimeout(10000);c.setReadTimeout(12000);c.setRequestProperty("User-Agent","SonoraMusic/1.0");InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[4096];int n;while((n=in.read(buf))>0)out.write(buf,0,n);JSONArray a=new JSONArray(new String(out.toByteArray(),"UTF-8"));for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);String stream=o.optString("url_resolved",o.optString("url",""));if(stream.startsWith("https://")){String name=o.optString("name","Radio");String country=o.optString("country","");String tags=o.optString("tags","");result.add(new Track(name,country.isEmpty()?tags:country,null,stream,"RADIO"));}}c.disconnect();
            }catch(Exception e){error="No se pudo conectar. Comprueba Internet e inténtalo otra vez.";}
            final String err=error;runOnUiThread(()->{content.removeAllViews();content.addView(txt("Resultados de radio",21,Color.WHITE,true));Button back=btn("← Volver a buscar",0xff303454,Color.WHITE);content.addView(back,lp(-1,44,8,12));back.setOnClickListener(v->{page="radio";renderPage();});if(err!=null){content.addView(txt(err,14,0xffff9eaa,false));return;}if(result.isEmpty()){content.addView(txt("No encontramos emisoras HTTPS para esa búsqueda.",14,muted,false));return;}content.addView(txt(result.size()+" emisoras encontradas",12,mint,true),lp(-1,-2,0,8));for(Track t:result)addTrackRow(t,-1,true);});
        });
    }
    void renderCatalog(){
        content.addView(txt("Explorar música",24,Color.WHITE,true));
        content.addView(txt("Descubre canciones con licencia a través de Jamendo.",13,muted,false),lp(-1,-2,4,14));
        if(JAMENDO_CLIENT_ID.equals("YOUR_JAMENDO_CLIENT_ID")){
            LinearLayout info=new LinearLayout(this);info.setOrientation(1);info.setPadding(17,18,17,18);info.setBackground(shape(panel,20));content.addView(info);
            info.addView(txt("Tu próximo descubrimiento",18,Color.WHITE,true));
            TextView body=txt("El catálogo de Jamendo ofrece música independiente con licencias identificables. Para activar la búsqueda, registra una aplicación en developer.jamendo.com y coloca tu client ID en la constante JAMENDO_CLIENT_ID del código.",14,muted,false);body.setPadding(0,8,0,12);info.addView(body);
            Button guide=btn("Abrir Jamendo Developers",purple,Color.WHITE);info.addView(guide);guide.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_VIEW,Uri.parse("https://developer.jamendo.com/"));startActivity(i);});
        }else{
            searchBox=new EditText(this);searchBox.setSingleLine(true);searchBox.setHint("Artista, canción o género");searchBox.setTextColor(Color.WHITE);searchBox.setHintTextColor(muted);searchBox.setBackground(shape(panel,16));searchBox.setPadding(14,10,14,10);content.addView(searchBox,lp(-1,52,0,8));
            Button b=btn("Buscar catálogo",purple,Color.WHITE);content.addView(b,lp(-1,48,0,10));status=txt("",12,muted,false);content.addView(status);
            b.setOnClickListener(v->{hideKeyboard();fetchJamendo(searchBox.getText().toString().trim());});
        }
    }
    void fetchJamendo(String q){if(q==null||q.trim().isEmpty()){status.setText("Escribe un artista, canción o género.");return;}status.setText("Buscando música…");network.execute(()->{ArrayList<Track> result=new ArrayList<>();String err=null;try{String u="https://api.jamendo.com/v3.0/tracks/?client_id="+URLEncoder.encode(JAMENDO_CLIENT_ID,"UTF-8")+"&format=json&limit=25&audioformat=mp32&search="+URLEncoder.encode(q,"UTF-8");HttpURLConnection c=(HttpURLConnection)new URL(u).openConnection();c.setConnectTimeout(10000);c.setReadTimeout(12000);InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[4096];int n;while((n=in.read(buf))>0)out.write(buf,0,n);JSONArray a=new JSONObject(new String(out.toByteArray(),"UTF-8")).getJSONArray("results");for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);String stream=o.optString("audio","");String art=o.optString("image"," ").trim();if(stream.startsWith("https://"))result.add(new Track(o.optString("name","Track"),o.optString("artist_name","Jamendo"),null,stream,"JAMENDO",art.startsWith("https://")?art:""));}c.disconnect();}catch(Exception e){err="No se pudo cargar el catálogo. Revisa el client ID y tu conexión.";}final String error=err;runOnUiThread(()->{content.removeAllViews();content.addView(txt("Resultados del catálogo",21,Color.WHITE,true));Button back=btn("← Volver a explorar",0xff303454,Color.WHITE);content.addView(back,lp(-1,44,8,12));back.setOnClickListener(v->{page="catalog";renderPage();});if(error!=null){content.addView(txt(error,14,0xffff9eaa,false));return;}for(Track t:result)addTrackRow(t,-1,true);if(result.isEmpty())content.addView(txt("No encontramos canciones para esa búsqueda.",14,muted,false));});});}
    void pickLocal(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("audio/*");i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,10);}
    @Override protected void onActivityResult(int r,int res,Intent d){super.onActivityResult(r,res,d);if(r==10&&res==RESULT_OK&&d!=null){if(d.getClipData()!=null){for(int i=0;i<d.getClipData().getItemCount();i++)addLocal(d.getClipData().getItemAt(i).getUri());}else if(d.getData()!=null)addLocal(d.getData());page="library";renderPage();}}
    void addLocal(Uri u){
        try{getContentResolver().takePersistableUriPermission(u,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}
        if(containsTrack(new Track("","",u,null,"LOCAL")))return;
        String name=null,artist="En tu dispositivo";
        try(android.database.Cursor cursor=getContentResolver().query(u,new String[]{android.provider.OpenableColumns.DISPLAY_NAME},null,null,null)){if(cursor!=null&&cursor.moveToFirst())name=cursor.getString(0);}catch(Exception ignored){}
        if(name==null||name.trim().isEmpty())name=u.getLastPathSegment();if(name==null||name.trim().isEmpty())name="Audio local";
        android.media.MediaMetadataRetriever meta=new android.media.MediaMetadataRetriever();
        try{meta.setDataSource(this,u);String title=meta.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_TITLE);String foundArtist=meta.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ARTIST);if(title!=null&&!title.trim().isEmpty())name=title;if(foundArtist!=null&&!foundArtist.trim().isEmpty())artist=foundArtist;}catch(Exception ignored){}finally{try{meta.release();}catch(Exception ignored){}}
        library.add(new Track(name,artist,u,null,"LOCAL"));saveLibrary();
    }
    void play(int i){if(i>=0&&i<library.size())playTrack(library.get(i));}
    void playTrack(Track t){
        if(t==null)return;
        int found=-1;for(int i=0;i<library.size();i++){Track x=library.get(i);if((t.uri!=null&&t.uri.equals(x.uri))||(t.url!=null&&t.url.equals(x.url))){found=i;break;}}
        if(found<0){library.add(t);found=library.size()-1;saveLibrary();}
        current=found;nowTitle.setText(t.title);nowSub.setText(t.artist+"  •  "+t.source);playButton.setText("…");
        try{org.json.JSONArray arr=new org.json.JSONArray();for(Track x:library){org.json.JSONObject o=new org.json.JSONObject();o.put("title",x.title);o.put("artist",x.artist);o.put("uri",x.uri==null?"":x.uri.toString());o.put("url",x.url==null?"":x.url);o.put("source",x.source);o.put("art",x.art==null?"":x.art);arr.put(o);}Intent i=new Intent(this,MediaPlaybackService.class);i.setAction(MediaPlaybackService.ACTION_PLAY);i.putExtra("queue",arr.toString());i.putExtra("index",found);if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);}catch(Exception e){playButton.setText("▶");Toast.makeText(this,"No se pudo iniciar el reproductor.",Toast.LENGTH_LONG).show();}
    }
    void serviceAction(String action){Intent i=new Intent(this,MediaPlaybackService.class);i.setAction(action);try{startService(i);}catch(Exception e){Toast.makeText(this,"Abre una canción primero.",Toast.LENGTH_SHORT).show();}}
    void toggle(){if(library.isEmpty()){page="library";renderPage();pickLocal();return;}serviceAction(MediaPlaybackService.ACTION_TOGGLE);}
    void hideKeyboard(){try{((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(searchBox.getWindowToken(),0);}catch(Exception ignored){}}
    void release(){serviceAction(MediaPlaybackService.ACTION_STOP);playing=false;}
    @Override protected void onDestroy(){try{unregisterReceiver(playbackReceiver);}catch(Exception ignored){}network.shutdownNow();super.onDestroy();}
}
