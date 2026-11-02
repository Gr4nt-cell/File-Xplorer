"""Editable vector references for the v0.2 app; sample files are illustrative."""
from pathlib import Path
import json,html,xml.etree.ElementTree as ET
OUT=Path(__file__).resolve().parents[1]/'design/v0.2';OUT.mkdir(parents=True,exist_ok=True)
ICONS=json.loads((OUT.parent/'icons.json').read_text());ICONS.update({'play':'M8 4L20 12 8 20Z','close':'M5 5L19 19M19 5L5 19','back':'M16 4L8 12 16 20','copy':'M8 8H21V21H8ZM3 16V3H16','moon':'M18 3A9 9 0 1 0 21 17A9 9 0 0 1 18 3'})
P={}
def theme(dark):
 global P
 P=dict(ink='#edf3ff' if dark else '#172842',muted='#a5b5ce' if dark else '#718198',blue='#8ab8ff' if dark else '#2478f4',bg='#101827' if dark else '#edf4fd',glass1='#28374e' if dark else '#ffffff',glass2='#19263b' if dark else '#edf5ff',edge='#617a9e' if dark else '#ffffff',sel='#29496f' if dark else '#ddebff')
def txt(s,x,y,n=16,c='ink',weight=400):return f'<text x="{x}" y="{y}" font-family="Roboto,sans-serif" font-size="{n}" font-weight="{weight}" fill="{P.get(c,c)}">{html.escape(s)}</text>'
def box(x,y,w,h,r=22,fill='url(#glass)',stroke=None):return f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="{r}" fill="{fill}" stroke="{stroke or P["edge"]}" stroke-opacity=".65"/>'
def icon(n,x,y,size=24,c='blue'):return f'<g transform="translate({x},{y}) scale({size/24})"><path d="{ICONS.get(n,ICONS["document"])}" fill="none" stroke="{P.get(c,c)}" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"/></g>'
def begin(w,h,dark):
 theme(dark)
 return f'''<svg xmlns="http://www.w3.org/2000/svg" width="{w}" height="{h}" viewBox="0 0 {w} {h}"><defs><linearGradient id="bg" x2="1" y2="1"><stop stop-color="{P['bg']}"/><stop offset=".55" stop-color="{'#162840' if dark else '#dcecff'}"/><stop offset="1" stop-color="{'#132c32' if dark else '#eaf8f4'}"/></linearGradient><linearGradient id="glass" x2="1" y2="1"><stop stop-color="{P['glass1']}" stop-opacity=".87"/><stop offset="1" stop-color="{P['glass2']}" stop-opacity=".72"/></linearGradient><radialGradient id="glow"><stop stop-color="{'#467bbb' if dark else '#8dc6ff'}" stop-opacity=".32"/><stop offset="1" stop-color="#8dc6ff" stop-opacity="0"/></radialGradient><filter id="blur"><feGaussianBlur stdDeviation="13"/></filter><clipPath id="media"><rect x="14" y="48" width="420" height="930" rx="30"/></clipPath><linearGradient id="sky" x2="0" y2="1"><stop stop-color="#618dba"/><stop offset=".6" stop-color="#bdd9dc"/><stop offset="1" stop-color="#f0d5ad"/></linearGradient><g id="landscape"><rect x="14" y="48" width="420" height="930" fill="url(#sky)"/><circle cx="337" cy="292" r="58" fill="#fff0c4" opacity=".8"/><path d="M14 655L109 346 219 570 301 432 434 694V978H14Z" fill="#405e78"/><path d="M50 716L182 482 269 675 340 567 434 763V978H14Z" fill="#2c495f"/><path d="M14 785Q156 692 295 787T434 774V978H14Z" fill="#1a364c"/><path d="M14 856Q132 802 255 854T434 833V978H14Z" fill="#254958"/><path d="M146 978Q198 867 273 806Q219 909 219 978" fill="#a9d0cf" opacity=".7"/></g></defs><rect width="100%" height="100%" fill="url(#bg)"/><ellipse cx="40" cy="170" rx="650" ry="560" fill="url(#glow)"/>'''
def dock(active='Browse'):
 b=box(24,908,400,70,30)
 for i,(label,g) in enumerate([('Home','apps'),('Browse','folder'),('Search','search'),('Tools','tools'),('Settings','settings')]):
  x=40+i*78;b+=icon(g,x+13,921,23,'blue' if label==active else 'muted')+txt(label,x+24,961,10,'blue' if label==active else 'muted') .replace('font-weight="400"','text-anchor="middle" font-weight="400"')
 return b
def file(name,meta,g,x,y,w,selected=False):return box(x,y,w,70,19,P['sel'] if selected else 'url(#glass)')+box(x+12,y+13,44,44,14)+icon(g,x+22,y+23,24)+txt(name,x+70,y+29,15,'ink',500)+txt(meta,x+70,y+51,11,'muted')+icon('more',x+w-35,y+24,20,'muted')
def phone(dark,menu=False):
 b=begin(448,1000,dark)+txt('9:41',28,33,14,'ink',500)+txt('●  ▰',375,33,12)+txt('file xplorer',24,87,22,'ink',600)+box(380,55,44,44,16)+icon('more',391,65,22,'ink')+txt('On-device storage',24,151,29,'ink',500)+box(303,169,121,41,16)+txt('Panel A ⇄',322,196,14,'blue',500)
 b+=box(24,226,400,588,28)+icon('up',43,246,23)+txt('/storage/emulated/0',80,263,13)+box(369,238,40,40,14)+icon('plus',378,247,22)+txt('8 items · 1 selected',43,300,11,'muted')
 rows=[('Download','Folder · Today','folder'),('Documents','Folder · Yesterday','folder'),('Android','Folder · 1 Nov 2026','root'),('Pictures','Folder · 31 Oct 2026','image'),('Design assets.zip','24.6 MB · 30 Oct 2026','archive'),('Field notes.md','14 KB · 29 Oct 2026','document')]
 for i,(n,m,g) in enumerate(rows):b+=file(n,m,g,36,316+i*79,376,i==0)
 b+=box(24,834,400,60,24)
 for i,n in enumerate(['Copy','Move','Paste','More']):b+=box(32+i*97,841,90,46,16)+txt(n,55+i*97,870,14,'blue',500)
 b+=dock()
 if menu:
  b=b[:b.index('<text x="28"')]+ '<g filter="url(#blur)">'+b[b.index('<text x="28"'):]+'</g>'
  b+='<rect width="448" height="1000" fill="#07111f" opacity=".28"/>'+box(30,136,388,706,30)+txt('Download',55,186,23,'ink',500)+txt('FILE ACTIONS',55,217,10,'muted',500)
  for i,(n,g) in enumerate([('Open','folder'),('Open containing folder','up'),('Copy','copy'),('Move','tools'),('Rename','document'),('Properties / checksums','document'),('Create archive / ISO','archive'),('Add / remove favorite','root'),('Delete','close')]):
   y=239+i*55;b+=box(44,y,360,48,16)+icon(g,59,y+13,21,'muted')+txt(n,94,y+30,14)
  b+=box(287,758,112,52,17)+txt('Close',325,791,14,'blue',500)
 return b+'</svg>'
def tablet(dark):
 b=begin(1440,940,dark)+box(18,18,228,904,30)+txt('file xplorer',41,64,23,'ink',600)+txt('Your space.',42,128,28,'ink',500)+txt('Everything, in its place.',42,156,12,'muted')
 for i,(n,g) in enumerate([('Overview','apps'),('Browse files','folder'),('Workbench','tools'),('Settings','settings')]):
  y=197+i*54;b+=box(31,y,202,46,16,P['sel'] if i==1 else 'url(#glass)')+icon(g,46,y+13,22)+txt(n,82,y+30,14)
 b+=txt('LOCATIONS',42,460,11,'muted',500)
 for i,(n,g) in enumerate([('On device','drive'),('Workspace','folder'),('Recent','document'),('Favorites','star'),('SD / USB folders','drive')]):b+=icon(g,44,484+i*52,22)+txt(n,80,501+i*52,14)
 b+=box(32,830,200,63,20)+icon('moon',48,849,23)+txt('Dark glass' if dark else 'Light glass',84,867,14)
 b+=txt('Two panes. One workspace.',282,78,32,'ink',500)+txt('A clear view of everything you keep.',283,109,13,'muted')+box(1120,39,264,54,20)+icon('search',1139,55,23,'muted')+txt('Find anything…',1174,72,14,'muted')
 for i,n in enumerate(['Copy','Move','Paste','Create archive','More']):b+=box(282+i*135,145,122,44,16)+txt(n,302+i*135,173,13,'blue',500)
 for pi,(title,path) in enumerate([('On-device storage','/storage/emulated/0'),('Downloads','/storage/emulated/0/Download')]):
  x=282+567*pi;b+=box(x,212,542,680,28)+icon('up',x+20,235,23)+txt(title,x+60,253,18,'ink',500)+icon('plus',x+487,236,23)+txt(path,x+24,286,12,'muted')+txt('NAME',x+24,327,10,'muted',500)+txt('SIZE / MODIFIED',x+358,327,10,'muted',500)
  rows=[('Download','Folder · Today','folder'),('Documents','Folder · Yesterday','folder'),('Android','Folder · Today','root'),('Pictures','Folder · 31 Oct 2026','image'),('Project backup.7z','486 MB · 30 Oct 2026','archive'),('Field notes.md','14 KB · 29 Oct 2026','document')] if pi==0 else [('Design assets.zip','24.6 MB · Today','archive'),('Trip photos','Folder · Yesterday','image'),('Annual report.pdf','2.4 MB · 30 Oct 2026','document'),('Release.apk','8.2 MB · 29 Oct 2026','apps'),('Archive.iso','1.8 GB · 27 Oct 2026','drive')]
  for k,(n,m,g) in enumerate(rows):b+=file(n,m,g,x+14,347+k*81,514,k==0 and pi==0)
  b+=txt(f'{len(rows)} items · '+('1 selected' if pi==0 else 'No items selected'),x+24,868,12,'muted')
 return b+'</svg>'
def media(dark,video=False):
 b=begin(448,1000,dark)+'<g clip-path="url(#media)"><use href="#landscape"/></g>'
 # The repeated scene is deliberately blurred only under the controls; its text stays crisp.
 for x,y,w,h,r in [(24,58,400,64,24),(24,812 if video else 896,400,166 if video else 78,26)]:
  cid=f'clip{y}';b+=f'<defs><clipPath id="{cid}"><rect x="{x}" y="{y}" width="{w}" height="{h}" rx="{r}"/></clipPath></defs><g clip-path="url(#{cid})"><use href="#landscape" filter="url(#blur)"/></g>'+box(x,y,w,h,r)
 b+=txt('Valley film.mp4' if video else 'Valley morning.jpg',42,97,17,'ink',500)+box(340,68,72,44,16)+txt('Close',356,96,14,'blue',500)
 if video:
  b+=txt('0:38 / 2:14',42,842,12,'muted')+'<path d="M44 868H402" stroke="#90aac0" stroke-width="4" stroke-linecap="round"/><path d="M44 868H147" stroke="'+P['blue']+'" stroke-width="4" stroke-linecap="round"/><circle cx="147" cy="868" r="7" fill="'+P['blue']+'"/>'
  for i,n in enumerate(['Pause','−10s','+10s','Sound']):b+=box(32+i*97,887,90,43,16)+txt(n,49+i*97,914,14)
  b+=txt('1×',96,956,14,'blue',500)+txt('Share',298,956,14,'blue',500)
 else:
  b+=txt('Pinch to zoom · drag to pan',126,868,12,'#edf3ff')
  for i,n in enumerate(['Fit','Rotate','Share']):b+=box(32+i*130,906,124,56,18)+txt(n,78+i*130,941,14,'blue',500)
 return b+'</svg>'
screens=[]
for dark in [False,True]:
 mode='dark' if dark else 'light'
 for kind,svg in [('phone',phone(dark)),('menu',phone(dark,True)),('tablet',tablet(dark)),('image-viewer',media(dark)),('video-viewer',media(dark,True))]:
  name=f'{kind}-{mode}';(OUT/(name+'.svg')).write_text(svg);screens.append({'name':name,'svg':svg})
(OUT/'screens.json').write_text(json.dumps(screens,ensure_ascii=False))
print('Created 10 editable v0.2 screen references')
