import pathlib,urllib.request,concurrent.futures,hashlib,json,io,zipfile
ROOT=pathlib.Path(__file__).resolve().parents[1];OUT=ROOT/'.deps';OUT.mkdir(exist_ok=True)
items=[
 ('org/apache/commons/commons-compress/1.27.1/commons-compress-1.27.1.jar','maven'),
 ('commons-io/commons-io/2.21.0/commons-io-2.21.0.jar','maven'),
 ('commons-net/commons-net/3.13.0/commons-net-3.13.0.jar','maven'),
 ('commons-codec/commons-codec/1.17.1/commons-codec-1.17.1.jar','maven'),
 ('org/apache/commons/commons-lang3/3.16.0/commons-lang3-3.16.0.jar','maven'),
 ('com/google/re2j/re2j/1.8/re2j-1.8.jar','maven'),
 ('com/google/guava/failureaccess/1.0.1/failureaccess-1.0.1.jar','maven'),
 ('org/tukaani/xz/1.10/xz-1.10.jar','maven'),
 ('net/lingala/zip4j/zip4j/2.11.5/zip4j-2.11.5.jar','maven'),
 ('com/github/junrar/junrar/7.5.5/junrar-7.5.5.jar','maven'),
 ('org/slf4j/slf4j-api/1.7.36/slf4j-api-1.7.36.jar','maven'),
 ('org/smali/dexlib2/2.5.2/dexlib2-2.5.2.jar','maven'),
 ('org/smali/baksmali/2.5.2/baksmali-2.5.2.jar','maven'),
 ('org/smali/smali/2.5.2/smali-2.5.2.jar','maven'),
 ('org/smali/util/2.5.2/util-2.5.2.jar','maven'),
 ('com/google/guava/guava/27.1-android/guava-27.1-android.jar','maven'),
 ('com/beust/jcommander/1.64/jcommander-1.64.jar','maven'),
 ('org/antlr/antlr-runtime/3.5.2/antlr-runtime-3.5.2.jar','maven'),
 ('com/android/tools/build/apksig/8.7.3/apksig-8.7.3.jar','google'),
 ('dev/rikka/shizuku/api/13.1.5/api-13.1.5.aar','maven'),
 ('dev/rikka/shizuku/provider/13.1.5/provider-13.1.5.aar','maven'),
 ('dev/rikka/shizuku/aidl/13.1.5/aidl-13.1.5.aar','maven'),
 ('dev/rikka/shizuku/shared/13.1.5/shared-13.1.5.aar','maven'),
 ('androidx/annotation/annotation/1.3.0/annotation-1.3.0.jar','google')]
expected={}
lock=ROOT/'dependency-lock.json'
if lock.exists():expected={r['name']:r['sha256'] for r in json.loads(lock.read_text())}
def fetch(item):
 path,repo=item;name=path.rsplit('/',1)[-1];url=('https://repo.maven.apache.org/maven2/' if repo=='maven' else 'https://dl.google.com/dl/android/maven2/')+path;f=OUT/name
 if not f.exists():f.write_bytes(urllib.request.urlopen(url,timeout=90).read())
 sha=hashlib.sha256(f.read_bytes()).hexdigest()
 if name in expected and expected[name]!=sha:raise RuntimeError('Dependency hash mismatch: '+name)
 if name.endswith('.aar'):
  with zipfile.ZipFile(io.BytesIO(f.read_bytes())) as archive:
   if 'classes.jar' not in archive.namelist():raise RuntimeError('AAR has no classes.jar: '+name)
   (OUT/(name[:-4]+'-classes.jar')).write_bytes(archive.read('classes.jar'))
 return dict(name=name,url=url,sha256=sha)
with concurrent.futures.ThreadPoolExecutor(max_workers=6) as p:results=list(p.map(fetch,items))
if not lock.exists() or set(expected)!={r["name"] for r in results}:lock.write_text(json.dumps(results,indent=2))
print('Resolved',len(results),'locked libraries.')
