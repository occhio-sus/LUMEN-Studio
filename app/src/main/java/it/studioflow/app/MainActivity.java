package it.studioflow.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.graphics.Color;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.*;
import java.util.*;
import java.io.*;

public class MainActivity extends Activity {
 LinearLayout messages;
 ScrollView scroll;
 EditText input;
 String topic = "";
 StringBuilder transcript = new StringBuilder();
 TextToSpeech tts;
 boolean ready = false;
 int blue = Color.rgb(37,78,219);
 int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }
 @Override public void onCreate(Bundle state) {
  super.onCreate(state);
  tts = new TextToSpeech(this, status -> {
   if(status == TextToSpeech.SUCCESS) {
    int result = tts.setLanguage(Locale.ITALIAN);
    ready = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED;
   }
  });
  LinearLayout root = new LinearLayout(this);
  root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(16),dp(20),dp(16),dp(12));
  root.setBackgroundColor(Color.rgb(247,248,252));
  TextView title = new TextView(this); title.setText("StudioFlow"); title.setTextSize(28); title.setTextColor(blue); root.addView(title);
  TextView note = new TextView(this); note.setText("Studio essenziale · demo locale, senza AI online"); root.addView(note);
  LinearLayout menu = new LinearLayout(this);
  button(menu,"Copia chat",v -> {
   ((ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("StudioFlow",transcript.toString()));
   Toast.makeText(this,"Chat copiata",Toast.LENGTH_SHORT).show();
  });
  button(menu,"Termini",v -> terms()); root.addView(menu);
  scroll = new ScrollView(this);
  messages = new LinearLayout(this); messages.setOrientation(LinearLayout.VERTICAL); scroll.addView(messages);
  root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
  LinearLayout actions = new LinearLayout(this);
  button(actions,"Flashcard",v -> cards());
  button(actions,"Quiz",v -> quiz());
  button(actions,"Podcast",v -> podcast()); root.addView(actions);
  input = new EditText(this); input.setHint("Incolla il testo da studiare…"); input.setMinLines(2); input.setMaxLines(4); root.addView(input);
  Button send = new Button(this); send.setText("Invia testo"); send.setOnClickListener(v -> {
   String text = input.getText().toString().trim();
   if(text.isEmpty()) return;
   if(text.length()>20000) { Toast.makeText(this,"Limite: 20.000 caratteri",Toast.LENGTH_LONG).show(); return; }
   topic = text; add("Tu",text); input.setText("");
   add("StudioFlow","Testo ricevuto. Vuoi creare flashcard, quiz o ascoltare il podcast? Questa demo usa il testo, non un'intelligenza artificiale.");
  }); root.addView(send); setContentView(root);
  add("StudioFlow","Incolla un testo di studio. Puoi creare carte, un quiz di completamento e ascoltare il testo.");
 }
 void button(LinearLayout row,String label,View.OnClickListener click) {
  Button b = new Button(this); b.setText(label); b.setTextSize(12); b.setOnClickListener(click);
  row.addView(b,new LinearLayout.LayoutParams(0,-2,1));
 }
 void add(String who,String text) {
  TextView t = new TextView(this); t.setText(who+"\n"+text); t.setTextSize(16); t.setTextColor(Color.rgb(30,35,50));
  t.setPadding(dp(12),dp(12),dp(12),dp(12)); t.setTextIsSelectable(true);
  LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1,-2); p.setMargins(0,dp(8),0,0);
  t.setBackgroundColor(Color.WHITE); messages.addView(t,p); transcript.append(who).append(": ").append(text).append("\n\n");
  scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
 }
 boolean hasText() { if(!topic.isEmpty()) return true; Toast.makeText(this,"Prima invia un testo",Toast.LENGTH_SHORT).show(); return false; }
 List<String> sentences() {
  List<String> result = new ArrayList<>();
  for(String s:topic.split("(?<=[.!?])\\s+|\\n+")) if(!s.trim().isEmpty()) result.add(s.trim());
  return result;
 }
 void cards() {
  if(!hasText()) return;
  List<String> lines=sentences();
  for(int i=0;i<Math.min(6,lines.size());i++) {
   final String answer=lines.get(i);
   Button card = new Button(this); card.setText("Carta: ricorda il punto "+(i+1)+" · Tocca per vedere");
   card.setOnClickListener(v -> card.setText(answer)); messages.addView(card);
  }
  add("Flashcard","Le carte seguono le frasi del tuo testo. Tocca ogni carta per scoprire la risposta.");
 }
 void quiz() {
  if(!hasText()) return;
  List<String> lines = sentences(); String sentence = lines.get(new Random().nextInt(lines.size()));
  String key="";
  for(String w:sentence.split("[^\\p{L}\\p{N}]+")) if(w.length()>key.length()) key=w;
  if(key.length()<2) { add("Quiz","Inserisci una frase più completa."); return; }
  final String expected=key;
  EditText answer=new EditText(this); answer.setHint("Parola mancante");
  AlertDialog d=new AlertDialog.Builder(this).setTitle("Completa la frase").setMessage(sentence.replace(expected,"_____"))
   .setView(answer).setPositiveButton("Verifica",(dialog,which) -> add("Quiz", answer.getText().toString().trim().equalsIgnoreCase(expected)?"Risposta corretta!":"La risposta era: "+expected)).setNegativeButton("Annulla",null).create(); d.show();
 }
 void podcast() {
  if(!hasText()) return;
  if(!ready) { add("Audio","Voce italiana non disponibile: controlla il motore di sintesi vocale del telefono."); return; }
  String spoken=topic.substring(0,Math.min(topic.length(),TextToSpeech.getMaxSpeechInputLength()));
  tts.speak(spoken,TextToSpeech.QUEUE_FLUSH,null,"studioflow");
  add("Audio","Lettura del testo avviata. È una lettura vocale, non un podcast AI. Per testi lunghi viene letta solo la prima parte.");
 }
 void terms() {
  try(InputStream in=getAssets().open("terms.txt"); ByteArrayOutputStream out=new ByteArrayOutputStream()) {
   byte[] b=new byte[2048]; int n; while((n=in.read(b))!=-1) out.write(b,0,n);
   TextView text=new TextView(this); text.setPadding(dp(16),dp(16),dp(16),dp(16)); text.setText(out.toString("UTF-8"));
   ScrollView box=new ScrollView(this); box.addView(text);
   new AlertDialog.Builder(this).setTitle("Termini e privacy — bozza").setView(box).setPositiveButton("Chiudi",null).show();
  } catch(IOException e) { Toast.makeText(this,"Impossibile leggere i termini",Toast.LENGTH_SHORT).show(); }
 }
 @Override protected void onDestroy() { if(tts!=null) { tts.stop(); tts.shutdown(); } super.onDestroy(); }
}
