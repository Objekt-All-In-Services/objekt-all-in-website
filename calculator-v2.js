(()=>{'use strict';
  const root=document.getElementById('rechner');
  if(!root)return;
  const q=s=>root.querySelector(s);
  const filling=q('#filling'), slider=q('#fill-slider'), floor=q('#floor'), ceiling=q('#ceiling');
  const board=q('#volume-preview'), tag=q('#volume-stage-label'), pct=q('#volume-stage-percent'), amount=q('#volume-stage-m3'), bar=q('#volume-stage-meter');
  if(!filling||!board)return;
  const levels=[
    {value:'wenig',short:'Wenig',text:'Leicht gefüllt',ratio:.12},
    {value:'normal',short:'Normal',text:'Normal eingerichtet',ratio:.28},
    {value:'voll',short:'Voll',text:'Stark gefüllt',ratio:.52},
    {value:'sehrvoll',short:'Sehr voll',text:'Sehr vollgestellt',ratio:.72}
  ];
  function refresh(){
    let idx=levels.findIndex(x=>x.value===filling.value);
    if(idx<0)idx=1;
    const item=levels[idx],level=idx+1;
    if(slider){slider.value=String(level);slider.setAttribute('aria-valuetext',item.text);}
    board.dataset.level=String(level);
    board.querySelectorAll('[data-min-level]').forEach(el=>{
      el.classList.toggle('is-visible',Number(el.dataset.minLevel)<=level);
    });
    if(tag)tag.textContent=item.text;
    if(pct)pct.textContent=Math.round(item.ratio*100)+' % Füllgrad';
    if(bar)bar.style.width=(item.ratio*100)+'%';
    const m=Math.max(0,Math.min(2000,Number(floor?.value)||0));
    const h=Math.max(1,Math.min(5,Number(ceiling?.value)||2.5));
    const v=m*h*item.ratio;
    if(amount)amount.textContent=v.toLocaleString('de-DE',{maximumFractionDigits:1,minimumFractionDigits:0})+' m³';
  }
  if(slider)slider.addEventListener('input',()=>{
    const n=Math.max(1,Math.min(4,Math.round(Number(slider.value)||2)));
    filling.value=levels[n-1].value;
    filling.dispatchEvent(new Event('input',{bubbles:true}));
    refresh();
  });
  filling.addEventListener('change',refresh);
  filling.addEventListener('input',refresh);
  floor?.addEventListener('input',refresh);
  ceiling?.addEventListener('input',refresh);
  refresh();
})();