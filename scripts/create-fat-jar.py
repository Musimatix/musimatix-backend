import zipfile

print "Create fat-jar from treeton's libs"

prosody_jar = "../third-party/treeton/prosody.jar"

zip_file = zipfile.ZipFile(prosody_jar, 'r')
zip_file.extract("META-INF/MANIFEST.MF")
zip_file.close()

