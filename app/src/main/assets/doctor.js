(()=>{
"use strict";
const el=id=>document.getElementById(id);
const token=new URLSearchParams(location.search).get("key");
const translations={"fr":{"eyebrow":"CENTRE DE DIAGNOSTIC · ANDROID TV","headline":"Vue d'ensemble","headline2":"de votre TV.","summary":"Diagnostic et entretien accessibles sur votre réseau local. Aucune donnée transmise au cloud.","refresh":"↻ Actualiser","export":"↓ Exporter JSON","device":"APPAREIL","memory":"MÉMOIRE RAM","storage":"STOCKAGE INTERNE","display":"AFFICHAGE","network":"CONNEXION","codec":"DÉCODAGE VIDÉO","technical":"Informations techniques","applications":"Applications détectées","maintenance":"Maintenance et outils avancés","search":"Rechercher une application","cleanup":"NETTOYAGE STANDARD","doctorCache":"Cache de TV Doctor","cleanButton":"Nettoyer le cache","advanced":"Maintenance avancée","root":"ROOT REQUIS","files":"Fichiers du dossier autorisé","filesNote":"Inventaire en lecture seule depuis le PC. Gère les suppressions sur la TV."},"en":{"eyebrow":"DIAGNOSTICS · ANDROID TV","headline":"Overview","headline2":"of your TV.","summary":"Diagnostics and maintenance on your local network. No cloud upload.","refresh":"↻ Refresh","export":"↓ Export JSON","device":"DEVICE","memory":"RAM MEMORY","storage":"INTERNAL STORAGE","display":"DISPLAY","network":"NETWORK","codec":"VIDEO DECODING","technical":"Technical details","applications":"Detected apps","maintenance":"Maintenance and advanced tools","search":"Find an application","cleanup":"STANDARD CLEANUP","doctorCache":"TV Doctor cache","cleanButton":"Clear cache","advanced":"Advanced maintenance","root":"ROOT REQUIRED","files":"Files in authorized folder","filesNote":"Read-only inventory on PC. Delete files directly on the TV."},"es":{"eyebrow":"DIAGNÓSTICO · ANDROID TV","headline":"Resumen","headline2":"de tu TV.","summary":"Diagnóstico y mantenimiento en la red local. Sin enviar datos a la nube.","refresh":"↻ Actualizar","export":"↓ Exportar JSON","device":"DISPOSITIVO","memory":"MEMORIA RAM","storage":"ALMACENAMIENTO","display":"PANTALLA","network":"RED","codec":"DECODIFICACIÓN DE VÍDEO","technical":"Detalles técnicos","applications":"Aplicaciones detectadas","maintenance":"Mantenimiento y herramientas","search":"Buscar una aplicación","cleanup":"LIMPIEZA ESTÁNDAR","doctorCache":"Caché de TV Doctor","cleanButton":"Limpiar caché","advanced":"Mantenimiento avanzado","root":"REQUIERE ROOT","files":"Archivos de carpeta autorizada","filesNote":"Inventario de solo lectura en el PC. Elimina archivos en la TV."},"ru":{"eyebrow":"ДИАГНОСТИКА · ANDROID TV","headline":"Обзор","headline2":"телевизора.","summary":"Диагностика и обслуживание в локальной сети. Без отправки данных в облако.","refresh":"↻ Обновить","export":"↓ Экспорт JSON","device":"УСТРОЙСТВО","memory":"ОПЕРАТИВНАЯ ПАМЯТЬ","storage":"ХРАНИЛИЩЕ","display":"ЭКРАН","network":"СЕТЬ","codec":"ДЕКОДИРОВАНИЕ ВИДЕО","technical":"Технические данные","applications":"Приложения","maintenance":"Обслуживание и инструменты","search":"Найти приложение","cleanup":"СТАНДАРТНАЯ ОЧИСТКА","doctorCache":"Кэш TV Doctor","cleanButton":"Очистить кэш","advanced":"Расширенные функции","root":"ТРЕБУЕТСЯ ROOT","files":"Файлы разрешённой папки","filesNote":"Просмотр файлов на ПК. Удаление выполняется на телевизоре."}};
let language=null;
let manualLanguage=false;
try{const saved=localStorage.getItem("atomic.doctor.lang");if(saved&&translations[saved]){language=saved;manualLanguage=true;}}catch(e){}
function changeLanguage(next,save){
 language=translations[next]?next:"fr";
 document.documentElement.lang=language;
 el("language").value=language;
 for(const node of document.querySelectorAll("[data-i18n]")){
  const id=node.getAttribute("data-i18n");
  node.textContent=translations[language][id]||translations.fr[id]||node.textContent;
 }
 if(save){manualLanguage=true;try{localStorage.setItem("atomic.doctor.lang",language);}catch(e){}}
}
el("language").addEventListener("change",event=>changeLanguage(event.target.value,true));
changeLanguage(language||"fr",false);

let latest=null,apps=[];
const set=(id,value)=>{el(id).textContent=String(value??"—")};
const bytes=value=>{
 const x=Number(value);
 if(!Number.isFinite(x)||x<0)return "—";
 const units=["o","Ko","Mo","Go","To"];let n=x,k=0;
 while(n>=1024&&k<units.length-1){n/=1024;k++}
 return new Intl.NumberFormat("fr-FR",{minimumFractionDigits:1,maximumFractionDigits:1}).format(n)+" "+units[k];
};
const percent=(used,total)=>Math.max(0,Math.min(100,Math.round(100*used/Math.max(1,total))));
const yes=v=>v?"Oui":"Non";
function drawDonut(which,pct){el(which).style.setProperty("--pct",pct+"%");}
function fields(data){
 const rows=[
 ["Marque",data.brand],["Modèle",data.model],["Version Android",data.android+" · API "+data.sdk],
 ["Correctif sécurité",data.securityPatch],["Carte / SoC",data.board+" / "+data.soc],
 ["Architecture CPU",Array.isArray(data.abis)?data.abis.join(" • "):data.abis],
 ["RAM totale",bytes(data.ramTotal)],["RAM libre",bytes(data.ramAvailable)],
 ["Seuil mémoire faible",bytes(data.ramThreshold)],["Stockage utilisé",bytes(data.storageUsed)],
 ["Stockage disponible",bytes(data.storageAvailable)],["Cache TV Doctor",bytes(data.ownCacheBytes)],
 ["Interface Android",data.displayWidth+" × "+data.displayHeight+" px"],
 ["Mode Android actif",data.activeModeWidth+" × "+data.activeModeHeight+" px"],
 ["Résolution native de la dalle",data.panelResolutionSource==="user"?data.panelWidth+" × "+data.panelHeight+" (déclarée sur la TV)":"Non déterminée par l’API Android"],
 ["Mode max signalé",data.maxExposedModeWidth+" × "+data.maxExposedModeHeight+" px"],
 ["Fréquence affichage",data.refreshRateHz+" Hz"],
 ["HDR déclaré",yes(data.displayHdrSupported)],
 ["Sorties audio",Array.isArray(data.audioOutputs)?data.audioOutputs.join(" ; "):data.audioOutputCount],
 ["Type réseau",data.network],["IP locale",data.ipAddress],["Interface réseau",data.interfaceName],
 ["Internet validé",yes(data.internetValidated)],["Débit liaison estimé ↓",data.downKbps+" kbit/s (pas un speedtest)"],
 ["Débit liaison estimé ↑",data.upKbps+" kbit/s (pas un speedtest)"],
 ["Décodeurs annoncés",data.decoderCount],["Version application",data.appVersion]
 ];
 const panel=el("techDetails");panel.replaceChildren();
 for(const [title,value] of rows){
   if(value===undefined||value===null||String(value).includes("undefined"))continue;
   const row=document.createElement("div");row.className="tech-row";
   const label=document.createElement("span");label.textContent=title;
   const val=document.createElement("strong");val.textContent=String(value);
   row.append(label,val);panel.append(row);
 }
}
async function refresh(){
 if(!token){set("state","Lien privé manquant");return}
 set("state","Lecture des données…");
 try{
  const res=await fetch("/api/status?key="+encodeURIComponent(token),{cache:"no-store",credentials:"omit"});
  if(!res.ok)throw Error("HTTP "+res.status);
  const d=await res.json();latest=d;if(!manualLanguage&&d.uiLanguage)changeLanguage(d.uiLanguage,false);
  set("state","● TV connectée · "+new Date(d.collectedAt).toLocaleTimeString("fr-FR"));
  set("device",(d.manufacturer||"")+" "+(d.model||""));
  set("android","Android "+d.android+" · "+d.cpuCores+" cœurs · API "+d.sdk);
  set("cpu",d.cpuCores+" cœurs");
  set("hardware",d.soc && d.soc!=="Non exposé"?d.soc:d.hardware);
  set("uptime",Math.floor(d.uptimeSeconds/3600)+" h");
  set("ram",bytes(d.ramAvailable)+" libres");
  set("ramTotal",bytes(d.ramTotal)+" au total");
  const ram=percent(d.ramTotal-d.ramAvailable,d.ramTotal);
  drawDonut("ramDonut",ram);set("ramPercent",ram+" %");
  set("storage",bytes(d.storageAvailable)+" libres");
  set("storageTotal",bytes(d.storageTotal)+" au total");
  const disk=percent(d.storageTotal-d.storageAvailable,d.storageTotal);
  drawDonut("diskDonut",disk);set("diskPercent",disk+" %");
  if(d.panelResolutionSource==="user"&&d.panelWidth>0){
   set("display",d.panelWidth>=3840?"4K UHD":d.panelWidth+" × "+d.panelHeight);
   set("displayNote","Dalle renseignée : "+d.panelWidth+" × "+d.panelHeight+" · Interface Android : "+d.displayWidth+" × "+d.displayHeight);
  }else{
   set("display",d.displayWidth+" × "+d.displayHeight);
   set("displayNote","Interface Android · "+d.refreshRateHz+" Hz · dalle native non déterminée");
  }
  set("displayModes",d.exposed4kMode?"Mode 4K déclaré parmi les modes Android":"Dalle native 4K : non vérifiable via Android");
  set("network",d.network);set("connectivity",d.internetValidated?"Internet validé":"Internet non confirmé");
  set("ip","IP locale : "+(d.ipAddress||"—"));
  for(const [id,k] of [["h264","h264Decoder"],["hevc","hevcDecoder"],["vp9","vp9Decoder"],["av1","av1Decoder"]])set(id,yes(d[k]));
  set("decoderCount",d.decoderCount+" décodeurs multimédia déclarés");
  set("version","v"+d.appVersion);
  set("cacheSize","Cache actuel : "+bytes(d.ownCacheBytes));
  fields(d);
 }catch(e){set("state","Connexion impossible : "+e.message)}
}
async function applications(){
 if(!token)return;
 try{
  const r=await fetch("/api/apps?key="+encodeURIComponent(token),{cache:"no-store",credentials:"omit"});
  if(!r.ok)throw Error("HTTP "+r.status);
  apps=await r.json();renderApps();
 }catch(e){set("appCount","Inventaire indisponible");set("appList","Erreur : "+e.message)}
}
function renderApps(){
 const needle=el("appSearch").value.trim().toLocaleLowerCase("fr");
 const filtered=apps.filter(a=>(a.label+" "+a.packageName).toLocaleLowerCase("fr").includes(needle));
 set("appCount",filtered.length+" application(s) visible(s)");
 const parent=el("appList");parent.replaceChildren();
 for(const a of filtered.slice(0,160)){
  const box=document.createElement("div");box.className="app-row";
  const h=document.createElement("strong");h.textContent=a.label+(a.system?"  ·  Système":"");
  if(a.system)h.className="system";
  const id=document.createElement("small");id.textContent=a.packageName;
  box.append(h,id);parent.append(box);
 }
 if(filtered.length===0)set("appList","Aucune application correspondante.");
}
async function clean(){
 if(!token||!confirm("Supprimer uniquement les fichiers temporaires d'ATOMIC TV Doctor ?"))return;
 const btn=el("cleanOwnCache");btn.disabled=true;set("cleanResult","Nettoyage en cours…");
 try{
  const r=await fetch("/api/clean-cache?key="+encodeURIComponent(token),{
   method:"POST",credentials:"omit",headers:{"X-Atomic-Action":"clean-own-cache"}
  });
  if(!r.ok)throw Error("HTTP "+r.status);
  const result=await r.json();set("cleanResult",bytes(result.cleanedBytes)+" nettoyés.");
  await refresh();
 }catch(e){set("cleanResult","Échec : "+e.message)}finally{btn.disabled=false}
}
function downloadReport(){
 if(!latest)return;
 const blob=new Blob([JSON.stringify(latest,null,2)],{type:"application/json"});
 const url=URL.createObjectURL(blob),a=document.createElement("a");
 a.href=url;a.download="ATOMIC-TV-Doctor-diagnostic.json";a.click();
 setTimeout(()=>URL.revokeObjectURL(url),1000);
}
async function loadFiles(){
 if(!token)return;
 const list=el("fileList");list.replaceChildren();set("fileCount","Lecture des fichiers…");
 try{
  const r=await fetch("/api/files?key="+encodeURIComponent(token),{cache:"no-store",credentials:"omit"});
  if(!r.ok)throw Error("HTTP "+r.status);
  const data=await r.json();
  if(!data.authorized){set("fileCount","Dossier non autorisé sur la TV");return}
  const entries=Array.isArray(data.items)?data.items:[];
  set("fileCount",entries.length+" éléments · dossier choisi sur la TV");
  for(const entry of entries.slice(0,160)){
   const box=document.createElement("div");box.className="app-row";
   const label=document.createElement("strong");
   label.textContent=(entry.directory?"▣ ":"◻ ")+entry.name;
   const size=document.createElement("small");
   size.textContent=entry.directory?"Dossier":bytes(entry.size);
   box.append(label,size);list.append(box);
  }
 }catch(e){set("fileCount","Lecture impossible : "+e.message)}
}
el("refresh").addEventListener("click",()=>{refresh();applications();loadFiles();loadFiles()});
el("export").addEventListener("click",downloadReport);
el("cleanOwnCache").addEventListener("click",clean);
el("appSearch").addEventListener("input",renderApps);
refresh();applications();
})();