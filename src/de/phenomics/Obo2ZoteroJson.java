/**
 * 
 */
package de.phenomics;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import ontologizer.go.OBOParser;
import ontologizer.go.OBOParserException;
import ontologizer.go.Ontology;
import ontologizer.go.Term;
import ontologizer.go.TermContainer;
import sonumina.math.graph.SlimDirectedGraphView;

/**
 * @author Sebastian Köhler (dr.sebastian.koehler@gmail.com)
 *
 */
public class Obo2ZoteroJson {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		// process ontologies from commandline
		for (String oboFile : args) {

			System.out.println("processing ontology: " + oboFile);

			boolean addDefinition = true;
			if (oboFile.toLowerCase().contains("mondo")) {
				addDefinition = false;
			}

			// init json document
			JSONArray jsonArrayTerms = new JSONArray();
			Ontology ontology = parseOntology(oboFile);
			SlimDirectedGraphView<Term> ontologySlim = ontology.getSlimGraphView();
			HashSet<Term> rootChildren = new HashSet<Term>(ontologySlim.getChildren(ontology.getRootTerm()));

			for (Term term : ontology) {

				// ignore root and direct subclasses of root
				if (ontology.isRootTerm(term.getID()))
					continue;

				if (rootChildren.contains(term))
					continue;

				JSONObject termAsZotero = new JSONObject();
				termAsZotero.put("id", "http://purl.obolibrary.org/obo/" + term.getIDAsString().replaceAll(":", "_"));
				termAsZotero.put("type", "entry-dictionary");
				termAsZotero.put("title", term.getName());
				termAsZotero.put("container-title", term.getIDAsString());

				JSONArray authorArray = new JSONArray();
				boolean hasDefOrSyn = false;

				if (term.getSynonyms() != null && term.getSynonyms().length > 0) {
					hasDefOrSyn = true;

					HashSet<String> allSyns = new HashSet<>();
					for (String syn : term.getSynonyms()) {
						allSyns.add(syn.toLowerCase());
					}
					allSyns = removeContained(allSyns);

					for (String syn : allSyns) {
						JSONObject authorObj = new JSONObject();
						authorObj.put("family", syn);
						authorArray.add(authorObj);
					}
				}
				if (addDefinition) {
					if (term.getDefinition() != null) {
						hasDefOrSyn = true;
						JSONObject authorObj = new JSONObject();
						authorObj.put("family", term.getDefinition());
						authorArray.add(authorObj);
					}
				}
				// put at least one author
				if (!hasDefOrSyn) {
					JSONObject authorObj = new JSONObject();
					authorObj.put("family", term.getName());
					authorArray.add(authorObj);
				}

				termAsZotero.put("author", authorArray);
				jsonArrayTerms.add(termAsZotero);
			}

			String jsonFileContent = jsonArrayTerms.toJSONString();
			try {

				BufferedWriter out = new BufferedWriter(new FileWriter(oboFile + ".json"));
				out.write(jsonFileContent);
				out.close();

			} catch (IOException e) {
				e.printStackTrace();
			}

			System.out.println("... finished");
		}

	}

	/**
	 * @param oboFile
	 * @return
	 */
	private static Ontology parseOntology(String oboFile) {
		OBOParser oboParser = null;
		oboParser = new OBOParser(oboFile, OBOParser.PARSE_DEFINITIONS | OBOParser.PARSE_XREFS);

		try {
			String parseInfo = oboParser.doParse();
			System.out.println(parseInfo);

		} catch (IOException | OBOParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// get the complete hpo
		TermContainer termContainer = new TermContainer(oboParser.getTermMap(), oboParser.getFormatVersion(),
				oboParser.getDataVersion());
		return Ontology.create(termContainer);
	}

	/**
	 * @param allSyns
	 * @return
	 */
	private static HashSet<String> removeContained(HashSet<String> allSyns) {
		HashSet<String> remove = new HashSet<>();
		for (String s : allSyns) {
			for (String os : allSyns) {
				if (os.length() > s.length())
					if (os.contains(s))
						remove.add(s);
			}
		}
		if (remove.size() > 0) {
			allSyns.removeAll(remove);
		}
		return allSyns;
	}

}
