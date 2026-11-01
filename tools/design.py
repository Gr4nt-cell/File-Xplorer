"""Create editable vector design references and a native Figma-layer importer."""
import pathlib,json,html
ROOT=pathlib.Path(__file__).resolve().parents[1];OUT=ROOT/'design';OUT.mkdir(exist_ok=True)
C={'ink':'#172842','muted':'#718198','accent':'#2478f4','line':'#e0e9f6'}
def text(s,x,y,size=16,color='ink',weight=400):return f'<text x="{x}" y="{y}" font-family="Roboto,DejaVu Sans,sans-serif" font-size="{size}" font-weight="{weight}" fill="{C.get(color,color)}">{html.escape(s)}</text>'
def box(x,y,w,h,r=24,fill='url(#glass)',stroke='#ffffff'):return f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="{r}" fill="{fill}" stroke="{stroke}" stroke-width="1.2"/>'
ICONS={'folder':'M3 7V19Q3 21 5 21H20Q22 21 22 19V8H12L9 4H5Q3 4 3 7Z','search':'M16 16L21 21M17 10.5A6.5 6.5 0 1 1 4 10.5A6.5 6.5 0 1 1 17 10.5','image':'M4 3H20Q21 3 21 5V19Q21 21 19 21H5Q3 21 3 19V5Q3 3 4 3ZM4 18L10 12 14 16 17 12 21 16M9 8A1 1 0 1 1 7 8A1 1 0 1 1 9 8','document':'M5 2H16L20 6V22H5ZM9 10H16M9 14H16M9 18H14','archive':'M5 3H20V21H5ZM11 4H13V7H11ZM11 10H13V13H11ZM10 16H14V19H10Z','apps':'M3 3H10V10H3ZM14 3H21V10H14ZM3 14H10V21H3ZM14 14H21V21H14Z','video':'M3 4H21V20H3ZM10 8L16 12 10 16Z','audio':'M9 17V5L20 3V15M9 18A3 2 0 1 1 3 18A3 2 0 1 1 9 18M20 16A3 2 0 1 1 14 16A3 2 0 1 1 20 16','tools':'M4 20L13 11Q9 2 20 4L16 8 19 11 22 7Q23 16 15 14L7 22Z','settings':'M20 12A8 8 0 1 1 4 12A8 8 0 1 1 20 12M16 12A4 4 0 1 1 8 12A4 4 0 1 1 16 12','more':'M12 4V5M12 11V12M12 18V19','root':'M12 2L21 6 20 15Q17 21 12 22Q6 20 4 15L3 6ZM8 12L11 15 17 8','drive':'M3 6H21V19H3ZM3 14H21M17 17H18','plus':'M12 4V20M4 12H20','up':'M5 14L12 7 19 14'}
def icon(name,x,y,size=24,color='#2478f4'):return f'<g transform="translate({x},{y}) scale({size/24})"><path d="{ICONS.get(name,ICONS["document"])}" fill="none" stroke="{color}" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"/></g>'
def begin(w,h):return f'''<svg xmlns="http://www.w3.org/2000/svg" width="{w}" height="{h}" viewBox="0 0 {w} {h}"><defs><linearGradient id="bg" x2="1" y2="1"><stop stop-color="#f5f8ff"/><stop offset=".45" stop-color="#e3efff"/><stop offset="1" stop-color="#eef8f4"/></linearGradient><linearGradient id="glass" x2="1" y2="1"><stop stop-color="#ffffff" stop-opacity=".94"/><stop offset="1" stop-color="#edf5ff" stop-opacity=".66"/></linearGradient><radialGradient id="glow"><stop stop-color="#8dc6ff" stop-opacity=".55"/><stop offset="1" stop-color="#8dc6ff" stop-opacity="0"/></radialGradient></defs><rect width="100%" height="100%" fill="url(#bg)"/><ellipse cx="40" cy="250" rx="620" ry="520" fill="url(#glow)"/>'''
def row(name,meta,glyph,x,y,w=380,selected=False):return box(x,y,w,74,18,'#ecf4ff' if selected else 'url(#glass)','#94bdf7' if selected else '#ffffff')+box(x+12,y+13,46,46,14)+icon(glyph,x+23,y+24,23)+text(name,x+72,y+31,15,'ink',500)+text(meta,x+72,y+53,11,'muted')+icon('more',x+w-35,y+24,21,'#718198')
b=begin(448,1000)+text('9:41',28,34,14,'ink',500)+text('●  ▰',375,34,13)+text('file xplorer',26,89,21,'ink',600)+box(338,57,42,42,15)+icon('search',348,67,22,'#172842')+box(388,57,36,42,15)+icon('more',395,67,21,'#172842')
b+=text('A LITTLE ORDER. A LOT OF POSSIBILITY.',26,135,10,'muted',500)+text('Your files.',24,179,35,'ink',600)+text('Beautifully in focus.',24,219,35,'ink',600)
b+=box(24,245,400,57,21)+icon('search',42,262,22,'#718198')+text('Search files, folders, or inside text…',78,279,14,'muted')
b+=box(24,321,400,152,26)+text('On-device storage',45,357,19,'ink',500)+text('128 GB used of 256 GB',45,384,13,'muted')+box(45,405,202,43,15)+text('Explore storage  ›',64,432,14,'accent',500)
b+='<circle cx="353" cy="397" r="42" fill="none" stroke="#d8e7fc" stroke-width="9"/><path d="M353 355A42 42 0 0 1 353 439" fill="none" stroke="#2478f4" stroke-width="9" stroke-linecap="round"/>'+text('50%',334,404,19,'ink',500)
b+=text('Find your flow',26,517,21,'ink',500)
for i,(name,glyph,col) in enumerate([('Images','image','#508bec'),('Videos','video','#9a72e8'),('Audio','audio','#e59655'),('Documents','document','#5cae99'),('Archives','archive','#7683d7'),('Apps','apps','#6582aa')]):
 x=24+i%3*138;y=539+i//3*109;b+=box(x,y,124,96,22)+icon(glyph,x+18,y+18,29,col)+text(name,x+17,y+77,13,'ink',500)
b+=text('Pick up where you left off',26,795,20,'ink',500)+row('Project notes.md','14 KB  ·  Today, 10:24','document',24,814,400)
b+=box(34,911,380,71,32)
for i,(n,g) in enumerate([('Overview','apps'),('Browse','folder'),('Tools','tools'),('Settings','settings')]):b+=icon(g,69+i*92,926,22,'#2478f4' if i==0 else '#718198')+text(n,53+i*92,964,10,'accent' if i==0 else 'muted')
b+='</svg>';(OUT/'File-Xplorer-phone.svg').write_text(b)
b=begin(1440,940)+box(18,18,228,904,30)+text('file xplorer',41,64,23,'ink',600)+text('Your space.',42,135,29,'ink',500)+text('Everything, in its place.',42,161,12,'muted')
for i,(name,glyph) in enumerate([('Overview','apps'),('Browse files','folder'),('Workbench','tools'),('Settings','settings')]):
 y=205+i*54;b+=(box(32,y-8,200,46,16,'#ddebff') if i==1 else '')+icon(glyph,45,y+4,21)+text(name,80,y+22,14)
b+=text('LOCATIONS',43,465,11,'muted',500)
for i,(name,glyph) in enumerate([('On device','drive'),('Workspace','folder'),('SD card','drive'),('Root filesystem','root')]):b+=icon(glyph,45,490+i*51,21)+text(name,80,507+i*51,14)
b+=text('FAVORITES',43,741,11,'muted',500)+icon('folder',45,769,21)+text('Downloads',80,786,14)+icon('document',45,821,21)+text('Documents',80,838,14)
b+=text('Two panes. One workspace.',282,81,32,'ink',500)+text('Move effortlessly between where files are and where they belong.',283,113,13,'muted')+box(1118,40,264,52,19)+icon('search',1138,55,23,'#718198')+text('Find anything…   Ctrl K',1174,72,13,'muted')
for i,label in enumerate(['Copy','Move','Paste','Create archive','More']):b+=box(282+i*135,148,122,43,16)+text(label,306+i*135,175,13,'accent',500)
for pi,(title,path) in enumerate([('On-device storage','/storage/emulated/0'),('Downloads','/storage/emulated/0/Download')]):
 x=282+pi*567;b+=box(x,212,542,680,28,stroke='#92b9f2' if pi==0 else '#ffffff')+icon('up',x+20,235,24)+text(title,x+60,254,17,'ink',500)+icon('plus',x+487,235,24)+text(path,x+24,287,12,'muted')+text('NAME',x+24,326,10,'muted',500)+text('SIZE / MODIFIED',x+358,326,10,'muted',500)
 rows=[('Download','Folder  ·  1 Nov 2026','folder'),('Documents','Folder  ·  31 Oct 2026','folder'),('Pictures','Folder  ·  31 Oct 2026','folder'),('Movies','Folder  ·  30 Oct 2026','folder'),('Project backup.7z','486 MB  ·  29 Oct 2026','archive'),('Field notes.md','14 KB  ·  28 Oct 2026','document')] if pi==0 else [('Design assets.zip','24.6 MB  ·  Today','archive'),('Trip photos','Folder  ·  Yesterday','folder'),('Annual report.pdf','2.4 MB  ·  30 Oct 2026','document'),('Release.apk','8.2 MB  ·  29 Oct 2026','apps'),('Archive.iso','1.8 GB  ·  27 Oct 2026','disc')]
 for k,(name,meta,glyph) in enumerate(rows):b+=row(name,meta,glyph,x+14,347+k*84,514,k==0 and pi==0)
 b+=text(f'{len(rows)} items  ·  No items selected',x+24,869,12,'muted')
b+='</svg>';(OUT/'File-Xplorer-desktop.svg').write_text(b)
(OUT/'icons.json').write_text(json.dumps(ICONS))
print('Authored phone and desktop vector design references.')
