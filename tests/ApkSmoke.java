package com.gr4nt.filexplorer;
import java.io.*;import java.security.*;import java.security.cert.X509Certificate;import java.util.zip.*;
public final class ApkSmoke {
 public static void main(String[] args)throws Exception{
  File apk=new File(args[0]);
  try(ZipFile z=new ZipFile(apk);InputStream in=z.getInputStream(z.getEntry("AndroidManifest.xml"))){String xml=BinaryTools.binaryXml(in.readAllBytes());if(!xml.contains("com.gr4nt.filexplorer")||!xml.contains("MainActivity"))throw new AssertionError("Manifest decode");}
  KeyStore ks=KeyStore.getInstance("JKS");try(InputStream in=new FileInputStream(args[1])){ks.load(in,"android".toCharArray());}
  File signed=new File(apk.getParentFile(),"smoke-resigned.apk");signed.delete();ApkWorkshop.sign(apk,signed,(PrivateKey)ks.getKey("androiddebugkey","android".toCharArray()),(X509Certificate)ks.getCertificate("androiddebugkey"));String result=ApkWorkshop.verify(signed);if(!result.startsWith("Verified: true"))throw new AssertionError(result);signed.delete();System.out.println("PASS real APK binary manifest decode and APK workshop v2/v3 signing/verification");
 }
}
