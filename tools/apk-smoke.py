import pathlib,subprocess
r=pathlib.Path(__file__).resolve().parents[1];cp=':'.join(map(str,[r/'build/test',*(r/'.deps').glob('*.jar')]))
subprocess.run(['java','com.sun.tools.javac.Main','-cp',cp,'-d',str(r/'build/test'),str(r/'tests/ApkSmoke.java')],check=True)
subprocess.run(['java','-cp',cp,'com.gr4nt.filexplorer.ApkSmoke',str(r/'build/File-Xplorer-v0.7.3.apk'),str(r/'build/development.keystore')],check=True)
