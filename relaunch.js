(()=>{'use strict';
const $=(s,root=document)=>root.querySelector(s);const $$=(s,root=document)=>Array.from(root.querySelectorAll(s));
const nav=$('#nav-links'),toggle=$('#nav-toggle');if(toggle&&nav){toggle.addEventListener('click',()=>{const show=nav.classList.toggle('open');toggle.setAttribute('aria-expanded',String(show))});$$('a',nav).forEach(a=>a.addEventListener('click',()=>{nav.classList.remove('open');toggle.setAttribute('aria-expanded','false')}))}
const floor=$('#floor'),height=$('#ceiling'),fill=$('#filling'),result=$('#volume-result'),fillface=$('#cube-fill'),fillface2=$('#cube-fill-2');
const factors={wenig:.12,normal:.28,voll:.52,sehrvoll:.72};
function recalc(){if(!floor||!height||!fill||!result)return;const m=Math.max(0,Math.min(2000,Number(floor.value)||0)),h=Math.max(1,Math.min(5,Number(height.value)||2.5)),f=factors[fill.value]||.28;const value=m*h*f;result.textContent=(Math.round(value*10)/10).toLocaleString('de-DE',{maximumFractionDigits:1})+' m³';const pct=Math.round(f*100);if(fillface)fillface.style.height=pct+'%';if(fillface2)fillface2.style.height=pct+'%';const out=$('#volume-detail');if(out)out.textContent='Unverbindliche Schätzung anhand von Fläche, Raumhöhe und Füllgrad. Die tatsächliche Menge hängt von Möbeln, Demontage und Stapelbarkeit ab.';}
[floor,height,fill].filter(Boolean).forEach(el=>el.addEventListener('input',recalc));recalc();
async function weather(){const status=$('#weather-state'),temp=$('#weather-temp'),snow=$('#weather-snow'),precip=$('#weather-precip');if(!status)return;try{const endpoint='https://api.open-meteo.com/v1/forecast?latitude=51.63&longitude=11.63&current=temperature_2m,precipitation,snowfall&daily=temperature_2m_min,snowfall_sum,precipitation_probability_max&timezone=Europe%2FBerlin&forecast_days=2';const res=await fetch(endpoint,{cache:'no-store'});if(!res.ok)throw Error('Wetterdaten nicht erreichbar');const d=await res.json();const t=d.current?.temperature_2m,mm=d.current?.precipitation,s=d.daily?.snowfall_sum?.[0],p=d.daily?.precipitation_probability_max?.[0];if(typeof t!=='number')throw Error('Keine Wetterdaten');temp.textContent=Math.round(t)+' °C';snow.textContent=(Number.isFinite(s)?s.toLocaleString('de-DE',{maximumFractionDigits:1})+' cm':'k. A.');precip.textContent=(Number.isFinite(p)?p+' %':'k. A.');const icy=(Number(d.daily?.temperature_2m_min?.[0])<=1)||t<=1;status.textContent=icy?'Achtung: niedrige Temperaturen möglich. Glätte bitte vor Ort prüfen.':'Aktuelle Wetterübersicht für Gerbstedt';const source=$('#weather-updated');if(source)source.textContent='Aktualisiert: '+new Date().toLocaleTimeString('de-DE',{hour:'2-digit',minute:'2-digit'})+' Uhr · Quelle: Open-Meteo';}catch(e){status.textContent='Live-Wetter gerade nicht verfügbar. Für Winterdienst bitte direkt anfragen.';temp.textContent='–';snow.textContent='–';precip.textContent='–';}}
const wbtn=$('#load-weather');if(wbtn)wbtn.addEventListener('click',()=>{wbtn.disabled=true;wbtn.textContent='Wetter wird geladen …';weather().finally(()=>{wbtn.hidden=true})});
function collect(form){const d=new FormData(form);const lines=[];for(const [k,v] of d.entries()){if(typeof v==='string'&&v.trim())lines.push(k+': '+v.trim())}return lines.join('\n');}
function mailSubmit(formId,subject){const f=$(formId);if(!f)return;f.addEventListener('submit',e=>{e.preventDefault();if(!f.reportValidity())return;const txt=collect(f);const u='mailto:info@objekt-services.com?subject='+encodeURIComponent(subject)+'&body='+encodeURIComponent(txt);window.location.href=u;const note=$('.mail-note',f)||$('#mail-note');if(note){note.hidden=false;note.textContent='Dein E-Mail-Programm wird geöffnet. Bitte die E-Mail darin noch absenden. Falls nichts passiert: info@objekt-services.com';}})}
function createCustomerWizard(){
 const f=$('#customer-form');if(!f)return;const grid=$('.calc-form',f);if(!grid)return;
 const fields=Array.from(grid.children);if(fields.length<5)return;
 const layouts=[[fields[3]],[fields[4]],[fields[0],fields[1],fields[2]]];
 const root=document.createElement('div');const head=document.createElement('div');head.className='wizard-head';head.innerHTML='<div class="wizard-label" id="wizard-label">Schritt 1 von 3 · Leistung wählen</div><div class="wizard-track"><div class="wizard-progress" id="wizard-progress"></div></div>';root.appendChild(head);
 const panels=layouts.map(group=>{const el=document.createElement('div');el.className='calc-form wizard-step';group.forEach(field=>el.appendChild(field));root.appendChild(el);return el;});
 const controls=document.createElement('div');controls.className='wizard-controls';
 const back=document.createElement('button');back.type='button';back.className='btn ghost';back.textContent='Zurück';
 const next=document.createElement('button');next.type='button';next.className='btn dark';next.textContent='Weiter →';
 const submit=$('button[type="submit"]',f);controls.append(back,next);if(submit){submit.removeAttribute('style');controls.appendChild(submit)}
 root.appendChild(controls);grid.replaceWith(root);
 let step=0;const labels=['Leistung wählen','Auftrag beschreiben','Kontaktdaten'];
 function show(n){step=Math.max(0,Math.min(2,n));panels.forEach((p,i)=>p.hidden=i!==step);$('#wizard-label').textContent='Schritt '+(step+1)+' von 3 · '+labels[step];$('#wizard-progress').style.width=((step+1)/3*100)+'%';back.hidden=step===0;next.hidden=step===2;if(submit)submit.hidden=step!==2}
 function advance(){for(const fld of $('input,select,textarea',panels[step])){if(!fld.reportValidity())return}show(step+1)}
 back.addEventListener('click',()=>show(step-1));next.addEventListener('click',advance);
 f.addEventListener('submit',event=>{if(step<2){event.preventDefault();event.stopImmediatePropagation();advance()}},true);
 show(0);
}
createCustomerWizard();
mailSubmit('#customer-form','Neue Kundenanfrage über die Webseite');mailSubmit('#partner-form','Kooperationsanfrage / Auftrag abgeben');
$$('[data-year]').forEach(e=>e.textContent=new Date().getFullYear());
})();