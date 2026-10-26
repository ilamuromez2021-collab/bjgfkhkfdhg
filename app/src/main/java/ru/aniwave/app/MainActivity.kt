package ru.aniwave.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

private val Bg = Color(0xFF0B0B12)
private val Panel = Color(0xFF171722)
private val Purple = Color(0xFFB59AFF)
private val Muted = Color(0xFFA2A2BA)

data class Anime(val id:Int,val title:String,val image:String,val score:String,val type:String,val year:String,val synopsis:String,val genres:List<String>,val pageUrl:String)

class MainActivity:ComponentActivity(){
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContent{AniWaveApp()}}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AniWaveApp(){
 val context= LocalContext.current
 val prefs= remember{context.getSharedPreferences("aniwave",0)}
 var favorites by remember{mutableStateOf(prefs.getStringSet("favorites", emptySet())?.toSet()?:emptySet())}
 var tab by remember{mutableStateOf("popular")}
 var searchText by remember{mutableStateOf("")}
 var activeQuery by remember{mutableStateOf("")}
 var genre by remember{mutableStateOf("Все жанры")}
 var anime by remember{mutableStateOf<List<Anime>>(emptyList())}
 var selected by remember{mutableStateOf<Anime?>(null)}
 var loading by remember{mutableStateOf(false)}
 var error by remember{mutableStateOf("")}
 var page by remember{mutableIntStateOf(1)}
 var reload by remember{mutableIntStateOf(0)}

 LaunchedEffect(tab,activeQuery,page,reload){
  if(tab=="favorites" && activeQuery.isBlank()){loading=false;error="";return@LaunchedEffect}
  loading=true;error=""
  val endpoint=when{
   activeQuery.isNotBlank()->"https://api.jikan.moe/v4/anime?q="+URLEncoder.encode(activeQuery,"UTF-8")+"&page=$page&limit=24"
   tab=="top"->"https://api.jikan.moe/v4/top/anime?page=$page&limit=24"
   tab=="season"->"https://api.jikan.moe/v4/seasons/now?page=$page&limit=24"
   else->"https://api.jikan.moe/v4/top/anime?filter=bypopularity&page=$page&limit=24"
  }
  try{
   val result=withContext(Dispatchers.IO){
    val conn=URL(endpoint).openConnection() as HttpURLConnection
    conn.connectTimeout=15000;conn.readTimeout=15000
    try{
     if(conn.responseCode !in 200..299) throw Exception("Сервис временно недоступен: "+conn.responseCode)
     val root=JSONObject(conn.inputStream.bufferedReader().use{it.readText()})
     val arr=root.optJSONArray("data")?:org.json.JSONArray()
     (0 until arr.length()).mapNotNull{i->
      val a=arr.optJSONObject(i)?:return@mapNotNull null
      val images=a.optJSONObject("images")
      val image=images?.optJSONObject("webp")?.optString("large_image_url").orEmpty().ifBlank{images?.optJSONObject("jpg")?.optString("image_url").orEmpty()}
      val ga=a.optJSONArray("genres")
      val names=(0 until (ga?.length()?:0)).mapNotNull{j->ga?.optJSONObject(j)?.optString("name")}
      val year=a.optJSONObject("aired")?.optJSONObject("from")?.optInt("year")?.takeIf{it>0}?.toString().orEmpty()
      Anime(a.optInt("mal_id"),a.optString("title","Без названия"),image,a.opt("score")?.toString()?.takeIf{it!="null"}?:"—",a.optString("type","Аниме"),year,a.optString("synopsis","Описание отсутствует."),names,a.optString("url","https://myanimelist.net"))
     }
    }finally{conn.disconnect()}
   }
   anime=result
  }catch(e:Exception){error=e.message?:"Не удалось загрузить каталог"}
  finally{loading=false}
 }

 fun toggleFavorite(a:Anime){favorites=if(favorites.contains(a.id.toString()))favorites-a.id.toString() else favorites+a.id.toString();prefs.edit().putStringSet("favorites",favorites).apply()}
 val display=if(tab=="favorites"&&activeQuery.isBlank()) anime.filter{favorites.contains(it.id.toString())} else anime.filter{genre=="Все жанры"||it.genres.contains(genre)}
 MaterialTheme(colorScheme=darkColorScheme(primary=Purple,background=Bg,surface=Panel,onBackground=Color.White,onSurface=Color.White,secondary=Purple)){
  Scaffold(containerColor=Bg){pad->
   Column(Modifier.fillMaxSize().padding(pad).padding(horizontal=16.dp)){
    Row(Modifier.fillMaxWidth().padding(top=16.dp,bottom=12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){
     Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){
      Box(Modifier.size(42.dp).background(Purple,RoundedCornerShape(13.dp)),contentAlignment=Alignment.Center){Text("A",color=Bg,fontSize=23.sp,fontWeight=FontWeight.Black)}
      Column{Text("AniWave.",fontSize=22.sp,fontWeight=FontWeight.ExtraBold);Text("Твоя аниме-вселенная",color=Muted,fontSize=11.sp)}
     }
     TextButton(onClick={tab="favorites";activeQuery="";searchText="";page=1}){Icon(Icons.Default.Favorite,null,tint=Purple);Text(" Избранное")}
    }
    Column(Modifier.fillMaxWidth().background(Panel,RoundedCornerShape(20.dp)).padding(18.dp)){
     Text("ТВОЁ СЛЕДУЮЩЕЕ АНИМЕ",color=Purple,fontSize=11.sp,fontWeight=FontWeight.Bold,letterSpacing=1.5.sp)
     Spacer(Modifier.height(8.dp));Text("Мир аниме\nначинается здесь.",fontSize=30.sp,fontWeight=FontWeight.Black,lineHeight=34.sp)
     Text("Ищи любимые тайтлы и открывай новые истории.",color=Muted,fontSize=13.sp)
     Spacer(Modifier.height(12.dp))
     OutlinedTextField(value=searchText,onValueChange={searchText=it},modifier=Modifier.fillMaxWidth(),placeholder={Text("Название аниме…")},singleLine=true,keyboardOptions=KeyboardOptions(imeAction=ImeAction.Search),keyboardActions=KeyboardActions(onSearch={activeQuery=searchText.trim();tab="popular";page=1}),trailingIcon={IconButton(onClick={activeQuery=searchText.trim();tab="popular";page=1}){Icon(Icons.Default.Search,"Найти")}},colors=OutlinedTextFieldDefaults.colors(focusedBorderColor=Purple,unfocusedBorderColor=Color(0xFF39374E)))
    }
    Spacer(Modifier.height(12.dp))
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)){
     listOf("popular" to "🔥 Топ","top" to "⭐ Лучшее","season" to "🆕 Новинки","favorites" to "♡").forEach{(key,label)->FilterChip(selected=tab==key,onClick={tab=key;activeQuery="";searchText="";page=1},label={Text(label,fontSize=10.sp)},modifier=Modifier.weight(1f),colors=FilterChipDefaults.filterChipColors(selectedContainerColor=Purple,selectedLabelColor=Bg))}
    }
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
     Text(if(activeQuery.isNotBlank())"Результаты" else when(tab){"top"->"Лучшие аниме";"season"->"Новинки сезона";"favorites"->"Избранное";else->"Популярное"},fontSize=18.sp,fontWeight=FontWeight.Bold)
     var expanded by remember{mutableStateOf(false)}
     Box{TextButton(onClick={expanded=true}){Text(genre,fontSize=10.sp);Text("⌄")};DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}){
      listOf("Все жанры","Action","Adventure","Comedy","Drama","Fantasy","Romance","Sci-Fi","Horror","Mystery").forEach{g->
       val label=mapOf("Action" to "Экшен","Adventure" to "Приключения","Comedy" to "Комедия","Drama" to "Драма","Fantasy" to "Фэнтези","Romance" to "Романтика","Sci-Fi" to "Фантастика","Horror" to "Ужасы","Mystery" to "Мистика")[g]?:g
       DropdownMenuItem(text={Text(label)},onClick={genre=label;expanded=false})
      }
     }}
    }
    when{
     loading->Box(Modifier.fillMaxWidth().weight(1f),contentAlignment=Alignment.Center){CircularProgressIndicator(color=Purple)}
     error.isNotBlank()->Column(Modifier.fillMaxWidth().weight(1f),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){Text(error,color=Muted);Button(onClick={reload++}){Text("Повторить")}}
     display.isEmpty()->Box(Modifier.fillMaxWidth().weight(1f),contentAlignment=Alignment.Center){Text(if(tab=="favorites")"Добавляй аниме в избранное" else "Ничего не найдено",color=Muted)}
     else->LazyVerticalGrid(columns=GridCells.Fixed(2),modifier=Modifier.weight(1f),contentPadding=PaddingValues(top=8.dp,bottom=12.dp),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
      items(display,key={it.id}){a->
       Column(Modifier.clickable{selected=a}){
        Box{AsyncImage(model=a.image,contentDescription=a.title,modifier=Modifier.fillMaxWidth().height(220.dp).background(Panel,RoundedCornerShape(12.dp)))
         IconButton(onClick={toggleFavorite(a)},modifier=Modifier.align(Alignment.TopEnd).padding(3.dp).background(Bg.copy(alpha=.8f),RoundedCornerShape(50))){Icon(if(favorites.contains(a.id.toString()))Icons.Default.Favorite else Icons.Default.FavoriteBorder,"Избранное",tint=if(favorites.contains(a.id.toString()))Purple else Color.White)}
        }
        Spacer(Modifier.height(6.dp));Text(a.title,fontWeight=FontWeight.SemiBold,fontSize=13.sp,maxLines=2,lineHeight=17.sp);Text("⭐ ${a.score} · ${a.type}",color=Muted,fontSize=11.sp)
       }
      }
     }
    }
    if(tab!="favorites"&&activeQuery.isBlank()){Row(Modifier.fillMaxWidth().padding(bottom=5.dp),horizontalArrangement=Arrangement.Center,verticalAlignment=Alignment.CenterVertically){TextButton(enabled=page>1,onClick={page--}){Text("← Назад")};Text("Стр. $page",color=Muted,fontSize=12.sp);TextButton(enabled=anime.isNotEmpty(),onClick={page++}){Text("Далее →")}}}
   }
  }
  selected?.let{a->Dialog(onDismissRequest={selected=null}){Column(Modifier.fillMaxWidth().heightIn(max=650.dp).background(Panel,RoundedCornerShape(20.dp)).verticalScroll(rememberScrollState()).padding(18.dp)){
   Row(horizontalArrangement=Arrangement.spacedBy(13.dp),verticalAlignment=Alignment.Top){AsyncImage(model=a.image,contentDescription=a.title,modifier=Modifier.width(110.dp).height(160.dp).background(Bg,RoundedCornerShape(10.dp)));Column(Modifier.weight(1f)){Text(a.title,fontSize=19.sp,fontWeight=FontWeight.Bold);Text("⭐ ${a.score} · ${a.type} ${a.year}",color=Muted,fontSize=12.sp);Spacer(Modifier.height(8.dp));Text(a.genres.joinToString(" · "),color=Purple,fontSize=12.sp)}}
   Spacer(Modifier.height(14.dp));Text("Описание",fontWeight=FontWeight.Bold);Text(a.synopsis,color=Muted,fontSize=13.sp);Spacer(Modifier.height(12.dp))
   Button(onClick={toggleFavorite(a)},modifier=Modifier.fillMaxWidth()){Text(if(favorites.contains(a.id.toString()))"♥ В избранном" else "♡ В избранное")}
   OutlinedButton(onClick={context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://www.crunchyroll.com/search?q="+URLEncoder.encode(a.title,"UTF-8"))))},modifier=Modifier.fillMaxWidth()){Text("Найти на Crunchyroll ↗")}
   OutlinedButton(onClick={selected=null},modifier=Modifier.fillMaxWidth()){Text("Закрыть")}
  }}}
 }
}
