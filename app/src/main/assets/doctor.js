(()=>{
"use strict";
const el=id=>document.getElementById(id);
const token=new URLSearchParams(location.search).get("key");
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
 ["Résolution native de la dalle","Non déterminable via l'API publique"],
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
  const d=await res.json();latest=d;
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
  set("display",d.displayWidth+" × "+d.displayHeight);
  set("displayNote","Surface de rendu Android · "+d.refreshRateHz+" Hz");
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
el("refresh").addEventListener("click",()=>{refresh();applications()});
el("export").addEventListener("click",downloadReport);
el("cleanOwnCache").addEventListener("click",clean);
el("appSearch").addEventListener("input",renderApps);
refresh();applications();
})();