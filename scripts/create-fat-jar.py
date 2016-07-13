import zipfile

print "Create fat-jar from treeton's libs"

prosody_jar = "../third-party/treeton/prosody.jar"

zip_file = zipfile.ZipFile(prosody_jar, 'r')
mf_contents = zip_file.read("META-INF/MANIFEST.MF")
zip_file.close()
mf_rows = mf_contents.split("\n")
mf_rows.map()

for r in mf_rows: print r


# zip_file.extract("META-INF/MANIFEST.MF")

