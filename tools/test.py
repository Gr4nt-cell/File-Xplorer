import pathlib,subprocess
r=pathlib.Path(__file__).resolve().parents[1];cp=':'.join(str(f) for f in (r/'.deps').glob('*.jar'));out=r/'build/test';out.mkdir(parents=True,exist_ok=True)
files=[r/'app/src/main/java/com/gr4nt/filexplorer'/(n+'.java') for n in ['FileSafety','SearchEngine','Archives','IsoImage','ApkWorkshop','BinaryTools']]
subprocess.run(['java','com.sun.tools.javac.Main','-encoding','UTF-8','-cp',cp,'-d',str(out),*[str(f) for f in files],str(r/'tests/CoreTest.java')],check=True)
subprocess.run(['java','-cp',str(out)+':'+cp,'com.gr4nt.filexplorer.CoreTest'],check=True)
