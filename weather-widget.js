(()=>{'use strict';
const hour=3600000;
const number=v=>typeof v==='number'&&Number.isFinite(v);
const isSnow=r=>['snow','sleet'].includes(r?.condition)||['snow','sleet'].includes(r?.icon);
const known=r=>typeof r?.condition==='string'||typeof r?.icon==='string';
function analyse(current,forecast,now=Date.now()){
 const w=current?.weather,stamp=Date.parse(w?.timestamp),age=now-stamp;
 const fresh=Number.isFinite(stamp)&&age>=-hour/6&&age<=3*hour;
 const rows=(Array.isArray(forecast?.weather)?forecast.weather:[]).filter(r=>{
  const t=Date.parse(r.timestamp);return t>now&&t<=now+24*hour;
 }).sort((a,b)=>Date.parse(a.timestamp)-Date.parse(b.timestamp));
 const unique=[...new Map(rows.map(r=>[r.timestamp,r])).values()];
 const full=unique.length>=24&&Date.parse(unique[0]?.timestamp)<=now+hour&&Date.parse(unique.at(-1)?.timestamp)>=now+23*hour&&unique.every((r,i)=>!i||Date.parse(r.timestamp)-Date.parse(unique[i-1].timestamp)<=hour);
 const temps=unique.filter(r=>number(r.temperature)).map(r=>r.temperature);
 const snow=unique.find(isSnow);
 return {current:fresh?w:null,stale:!!w&&!fresh,rows:unique,full,
  snow:snow?'possible':full&&unique.every(known)?'none':'unknown',snowAt:snow?.timestamp,
  min:temps.length?Math.min(...temps):null,
  frost:temps.some(t=>t<=0)?'possible':full&&temps.length===unique.length?'none':'unknown'};
}
if(typeof module==='object'&&module.exports)module.exports={analyse,isSnow};
if(typeof document==='undefined')return;
const box=document.getElementById('local-weather');if(!box)return;
const $=s=>box.querySelector(s),select=$('#weather-city'),load=$('#weather-load'),locate=$('#weather-locate'),stop=$('#weather-stop'),status=$('#weather-message');
const places={gerbstedt:[51.63,11.63],magdeburg:[52.12,11.63],halle:[51.48,11.97],eisleben:[51.53,11.55],bernburg:[51.79,11.74],aschersleben:[51.76,11.46],koennern:[51.67,11.77],hettstedt:[51.65,11.51],sangerhausen:[51.47,11.30],wernigerode:[51.83,10.79],quedlinburg:[51.79,11.14],erfurt:[50.98,11.03],leipzig:[51.34,12.37]};
let consent=false,position=null,generation=0,controllers=[],timer=null,geoPending=false;
const dt=t=>new Intl.DateTimeFormat('de-DE',{timeZone:'Europe/Berlin',day:'2-digit',month:'2-digit',hour:'2-digit',minute:'2-digit'}).format(new Date(t));
const celsius=n=>new Intl.NumberFormat('de-DE',{maximumFractionDigits:1}).format(n)+' °C';
const states={dry:'Trocken',fog:'Nebel',rain:'Regen',sleet:'Schneeregen',snow:'Schnee',hail:'Hagel',thunderstorm:'Gewitter'};
function clear(){ $('#weather-results').hidden=true;$('#weather-hours').replaceChildren(); }
function cancel(){generation++;controllers.forEach(c=>c.abort());controllers=[];clearInterval(timer);timer=null;}
function busy(on){load.disabled=on;locate.disabled=on;select.disabled=on;box.setAttribute('aria-busy',String(on));}
async function get(path,params){
 const u=new URL('https://api.brightsky.dev/'+path);Object.entries(params).forEach(([k,v])=>u.searchParams.set(k,String(v)));
 const c=new AbortController();controllers.push(c);const timeout=setTimeout(()=>c.abort(),15000);
 try{const r=await fetch(u,{signal:c.signal,credentials:'omit',referrerPolicy:'no-referrer'});if(!r.ok)throw Error('Wetterdienst nicht erreichbar');return await r.json();}finally{clearTimeout(timeout);}
}
function render(a,current,name){
 $('#weather-place').textContent=name+' · Umgebung';
 const w=a.current;$('#weather-temp').textContent=number(w?.temperature)?celsius(w.temperature):'– °C';
 $('#weather-now').textContent=w?(states[w.condition]||'Wetterzustand nicht gemeldet'):(a.stale?'Messwerte veraltet':'Aktuelle Messwerte nicht verfügbar');
 $('#weather-current-snow').textContent=w&&known(w)?(isSnow(w)?'Schnee / Schneeregen gemeldet':'Kein Schneefall gemeldet'):'Schneefall aktuell: keine Angabe';
 $('#weather-current-time').textContent=w?'Messzeit: '+dt(w.timestamp)+' Uhr':'Keine aktuelle Temperatur verfügbar.';
 $('#weather-snow').textContent=a.snow==='possible'?'Schnee / Schneeregen möglich ab '+dt(a.snowAt)+' Uhr':a.snow==='none'?'Kein Schneefall vorhergesagt':'Schneevorhersage unvollständig';
 $('#weather-min').textContent=number(a.min)?celsius(a.min):'Keine Angabe';
 $('#weather-frost').textContent=a.frost==='possible'?'Frost möglich. Glätte vor Ort prüfen.':a.frost==='none'?'Kein Luftfrost vorhergesagt. Bodenglätte bleibt möglich.':'Frostvorhersage unvollständig. Glätte vor Ort prüfen.';
 const risk=a.snow==='possible'||a.frost==='possible';$('#weather-advice').textContent=risk?'Schnee oder Frost möglich? Jetzt Winterdienst abstimmen.':'Winterdienst frühzeitig planen und Einsatzbereitschaft sichern.';
 box.dataset.risk=String(risk);
 $('#weather-coverage').textContent=a.full?'Vorhersage für die nächsten 24 Stunden.':'Nur teilweise verfügbare Vorhersage für die nächsten 24 Stunden.';
 const sources=Array.isArray(current?.sources)?current.sources:[];
 const id=w?.fallback_source_ids?.temperature??w?.source_id;const src=sources.find(s=>s.id===id);
 $('#weather-station').textContent=src?'Temperaturmessung: '+src.station_name+(number(src.distance)?' (ca. '+Math.round(src.distance/1000)+' km entfernt)':'')+'. Werte gelten näherungsweise für die Umgebung.':'DWD-Mess- und Vorhersagewerte für die Umgebung, keine Messung direkt am Grundstück.';
 const hours=$('#weather-hours');hours.replaceChildren();
 a.rows.slice(0,6).forEach(r=>{const li=document.createElement('li'),time=document.createElement('span'),value=document.createElement('strong'),state=document.createElement('span');time.textContent=new Intl.DateTimeFormat('de-DE',{timeZone:'Europe/Berlin',hour:'2-digit',minute:'2-digit'}).format(new Date(r.timestamp));value.textContent=number(r.temperature)?celsius(r.temperature):'–';state.textContent=states[r.condition]||'Keine Angabe';li.append(time,value,state);hours.append(li);});
 $('#weather-results').hidden=false;$('#weather-retrieved').textContent='Abgerufen: '+dt(Date.now())+' Uhr. Bei geöffneter Seite nach 15 Minuten aktualisiert.';
 status.textContent=w||a.rows.length?'Wetter für '+name+' geladen.':'Wetterdaten derzeit nicht verfügbar. Bitte später erneut versuchen.';
}
async function refresh(){
 if(!consent)return;cancel();const id=generation;clear();busy(true);status.textContent='Wetterdaten werden geladen …';
 const coords=position??places[select.value];if(!coords){busy(false);status.textContent='Bitte einen Ort wählen.';return;}
 const label=position?'Ihr Standort':select.selectedOptions[0].textContent;
 const now=Date.now(),params={lat:coords[0].toFixed(2),lon:coords[1].toFixed(2),tz:'Europe/Berlin',max_dist:50000};
 try{
  const r=await Promise.allSettled([get('current_weather',params),get('weather',{...params,date:new Date(now).toISOString(),last_date:new Date(now+24*hour).toISOString()})]);
  if(id!==generation||!consent)return;const current=r[0].status==='fulfilled'?r[0].value:null,forecast=r[1].status==='fulfilled'?r[1].value:null;
  render(analyse(current,forecast,Date.now()),current,label);
 }catch(e){if(id===generation)status.textContent='Wetterdaten derzeit nicht verfügbar. Bitte später erneut versuchen.';}
 finally{if(id===generation){busy(false);stop.hidden=false;load.textContent='Wetter aktualisieren';timer=setInterval(()=>{if(consent&&!document.hidden)refresh();},15*60*1000);}}
}
load.addEventListener('click',()=>{consent=true;stop.hidden=false;refresh();});
select.addEventListener('change',()=>{if(select.value!=='standort')position=null;if(consent)refresh();else clear();});
locate.addEventListener('click',()=>{
 if(!navigator.geolocation){status.textContent='Ihr Browser bietet keinen Standortzugriff. Bitte einen Ort auswählen.';return;}
 cancel();clear();geoPending=true;stop.hidden=false;const token=generation;busy(true);status.textContent='Bitte den Standortzugriff im Browser erlauben. Alternativ können Sie einen Ort auswählen.';
 navigator.geolocation.getCurrentPosition(p=>{
  if(token!==generation||!geoPending)return;geoPending=false;
  const lat=Math.round(p.coords.latitude*100)/100,lon=Math.round(p.coords.longitude*100)/100;
  if(lat<47||lat>55.5||lon<5.5||lon>15.5){busy(false);status.textContent='Der Standort liegt außerhalb der unterstützten Region Deutschland. Bitte einen Ort auswählen.';return;}
  position=[lat,lon];consent=true;if(!select.querySelector('option[value="standort"]')){const option=document.createElement('option');option.value='standort';option.textContent='Mein Standort (Umgebung)';select.append(option);}select.value='standort';refresh();
 },()=>{if(token!==generation)return;geoPending=false;busy(false);status.textContent='Standort nicht verfügbar oder nicht freigegeben. Bitte oben einen Ort auswählen und „Wetter anzeigen“ wählen.';},{enableHighAccuracy:false,timeout:12000,maximumAge:300000});
});
stop.addEventListener('click',()=>{consent=false;geoPending=false;position=null;cancel();clear();busy(false);stop.hidden=true;select.querySelector('option[value="standort"]')?.remove();load.textContent='Wetter anzeigen';$('#weather-place').textContent='Wetter für Ihren Ort';status.textContent='Wetterabruf beendet. Es werden keine weiteren Wetterdaten angefragt. Die Standortfreigabe können Sie zusätzlich in Ihrem Browser widerrufen.';});
})();
