// Copyright (c) 2022-2025  Egon Willighagen <egon.willighagen@gmail.com>
//
// GPL v3

@Grab(group='io.github.egonw.bacting', module='managers-rdf', version='1.0.5')
@Grab(group='io.github.egonw.bacting', module='managers-ui', version='1.0.5')
@Grab(group='io.github.egonw.bacting', module='net.bioclipse.managers.jsoup', version='1.0.5')

bioclipse = new net.bioclipse.managers.BioclipseManager(".");
rdf = new net.bioclipse.managers.RDFManager(".");
jsoup = new net.bioclipse.managers.JSoupManager(".");

sitemap = "https://cloud.vhp4safety.nl/sitemap.xml"
sitemapTxt = bioclipse.download(sitemap)

def urlset = new XmlSlurper().parseText(sitemapTxt)
databases = []
for (url in urlset.children()) databases.add(url.loc.text())

kg = rdf.createInMemoryStore()

for (database in databases) {
    htmlContent = bioclipse.download(database)

    htmlDom = jsoup.parseString(htmlContent)

    // application/ld+json

    bioschemasSections = jsoup.select(htmlDom, "script[type='application/ld+json']");

    for (section in bioschemasSections) {
        bioschemasJSON = section.html()
        rdf.importFromString(kg, bioschemasJSON, "JSON-LD")
    }
}

turtle = rdf.asTurtle(kg);

println "#" + rdf.size(kg) + " triples detected in the JSON-LD"
// println turtle ; System.exit(0)

sparql = """
PREFIX schema: <http://schema.org/>
SELECT ?dataset ?url ?name ?license ?description WHERE {
?dataset a schema:SoftwareApplication ;
    schema:url ?url .
OPTIONAL { ?dataset schema:name ?name }
OPTIONAL { ?dataset schema:license ?license }
OPTIONAL { ?dataset schema:description ?description }
} ORDER BY ASC(?dataset)
"""

results = rdf.sparql(kg, sparql)

println "@prefix dc:    <http://purl.org/dc/elements/1.1/> ."
println "@prefix dct:   <http://purl.org/dc/terms/> ."
println "@prefix foaf:  <http://xmlns.com/foaf/0.1/> ."
println "@prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> ."
println "@prefix sbd:   <https://www.sbd4nano.eu/rdf/#> ."
println "@prefix xsd:   <http://www.w3.org/2001/XMLSchema#> ."
println "@prefix void:  <http://rdfs.org/ns/void#> ."
println ""
println "<https://vhp4safety.github.io/cloud/>"
println " a                    void:DatasetDescription ;"
println " dc:source            <https://cloud.vhp4safety.nl/> ;"
println " dct:title            \"Cloud services provided by VHP4Safety\"@en ;"
println " foaf:img             <https://images.nieuwsbrieven.rivm.nl/101500/0/5763/fe1e7915ce28f7a96ca25ed234631504.png> ;"
println " dct:license          <http://creativecommons.org/publicdomain/zero/1.0/> . # license of this metadata"
println ""

for (i=1;i<=results.rowCount;i++) {
println "<${results.get(i, "dataset")}> a sbd:Resource ;"
println "  dc:source <https://vhp4safety.github.io/cloud/> ;"
if (results.get(i, "name") != null) println "  rdfs:label \"${results.get(i, "name")}\"@en ;"
if (results.get(i, "description") != null) println "  dc:description \"${results.get(i, "description")}\"@en ;"
if (results.get(i, "license") != null) println "  dct:license <${results.get(i, "license")}> ;"
println "  foaf:page <${results.get(i, "url")}> ."
println ""
}
