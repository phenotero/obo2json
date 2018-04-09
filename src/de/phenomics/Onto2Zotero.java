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

import ontologizer.go.Ontology;
import ontologizer.go.Term;
import util.OntologyUtil;

/**
 * @author Sebastian Köhler (dr.sebastian.koehler@gmail.com)
 *
 */
public class Onto2Zotero {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		// get the obo files form cmd-line
		int c = 0;
		for (String oboFile : args) {
			// init json document
			JSONArray arr = new JSONArray();
			System.out.println("parse ontology: " + oboFile);
			Ontology ontology = OntologyUtil.parseOntology(oboFile);
			for (Term t : ontology) {
				JSONObject termAsZotero = new JSONObject();
				termAsZotero.put("id", "http://purl.obolibrary.org/obo/" + t.getIDAsString().replaceAll(":", "_"));
				termAsZotero.put("type", "entry-dictionary");
				termAsZotero.put("title", t.getIDAsString() + " : " + t.getName());
				termAsZotero.put("container-title", t.getIDAsString());
				JSONArray authorArray = new JSONArray();
				boolean hasDefOrSyn = false;

				if (t.getSynonyms() != null && t.getSynonyms().length > 0) {
					hasDefOrSyn = true;

					HashSet<String> allSyns = new HashSet<>();
					for (String syn : t.getSynonyms()) {
						allSyns.add(syn.toLowerCase());
					}
					allSyns = removeContained(allSyns);

					for (String syn : allSyns) {
						JSONObject authorObj = new JSONObject();
						authorObj.put("family", syn);
						authorArray.add(authorObj);
					}
				}
				if (t.getDefinition() != null) {
					hasDefOrSyn = true;
					JSONObject authorObj = new JSONObject();
					authorObj.put("family", t.getDefinition());
					authorArray.add(authorObj);
				}

				// put at least one author
				if (!hasDefOrSyn) {
					JSONObject authorObj = new JSONObject();
					authorObj.put("family", t.getName());
					authorArray.add(authorObj);
				}

				termAsZotero.put("author", authorArray);
				arr.add(termAsZotero);
			}
			String json = arr.toJSONString();
			String jsonWithNl = json.replaceAll(",", ",\n");
			try {

				BufferedWriter out = new BufferedWriter(new FileWriter(oboFile + ".json"));
				out.write(json);
				out.close();

				BufferedWriter out2 = new BufferedWriter(new FileWriter(oboFile + "2.json"));
				out2.write(jsonWithNl);
				out2.close();

			} catch (IOException e) {
				e.printStackTrace();
			}
			System.out.println("done");
		}

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
