package cmucoref.document.clean;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class CoNLLDocumentCleaner {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(args[0]), "UTF8"));
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(args[1]), "UTF8"));
		
		String title = reader.readLine();
		while(title != null && title.length() != 0) {
			System.err.println(title);
			List<List<String[]>> sents = getNextDoc(reader);
			
			validatePunct(sents);
			
			//remove HYPH "-", such as "large-scale
			//removeHYPHDashes(sents);
			
			//remove UH token such as "yeah"
			removeUHs(sents);
			removeUHsTokens(sents);
			
			//remove sentence of length 0
			removeZeroSent(sents);
			
			writeDoc(writer, title, sents);
			title = reader.readLine();
		}
		
		reader.close();
		writer.close();
	}
	
	protected static void validatePunct(List<List<String[]>> sents) {
		Set<String> normalPuncts = new HashSet<String>(Arrays.asList(".", "?", "!"));
		Set<String> slaspPuncts = new HashSet<String>(Arrays.asList("/.", "/?", "/!"));
		
		for(List<String[]> sent : sents) {
			for(String[] token : sent) {
				if(token[4].equals(".")) {
					if(normalPuncts.contains(token[3])) {
						continue;
					}
					else if(slaspPuncts.contains(token[3])) {
						token[3] = token[3].substring(1);
						assert normalPuncts.contains(token[3]);
					}
					else {
						token[3] = ".";
					}
				}
			}
		}
	}
	
	/**
	 * remove tokens with UH pos tags
	 * @param sents
	 */
	protected static void removeUHs(List<List<String[]>> sents) {
		removeOnlyUHPuncSent(sents);
		removeUHsTokens(sents);
	}
	
	/**
	 * remove sentences that only contain UH or punctuations
	 * @param sents
	 */
	private static void removeOnlyUHPuncSent(List<List<String[]>> sents) {
		Set<List<String[]>> remove = new HashSet<List<String[]>>();
		for(List<String[]> sent : sents) {
			boolean removed = true;
			for(String[] token : sent) {
				if(!token[4].equals("UH") && !token[4].equals("CC") 
						&& !token[4].equals(".") && !token[4].equals(",") && !token[4].equals(":")) {
					removed = false;
					break;
				}
			}
			if(removed) {
				remove.add(sent);
			}
		}
		sents.removeAll(remove);
	}
	
	private static void removeUHsTokens(List<List<String[]>> sents) {
		for(List<String[]> sent : sents) {
			Set<String[]> remove = new HashSet<String[]>();
			for(int i = 0; i < sent.size(); ++i) {
				String[] token = sent.get(i);
				if(remove.contains(token)) {
					continue;
				}
				
				if(token[4].equals("UH") && token[token.length - 1].equals("-")) {
					remove.add(token);
					if(token[5].equals("*") || token[5].equals("(INTJ*)")) {
						continue;
					}
					else if(token[5].endsWith("(INTJ*)")) {
						String prefix = token[5].substring(0, token[5].length() - "(INTJ*)".length());
						for(int j = i + 1; j < sent.size(); ++j) {
							String[] next = sent.get(j);
							if((next[4].equals(",") || next[4].equals(".") || next[4].equals(":"))
								&& (next[5].equals("*"))) {
								remove.add(next);
								continue;
							}
							next[5] = prefix + next[5];
							break;
						}
					}
					else if(token[5].endsWith("(INTJ*")) {
						String prefix = token[5].substring(0, token[5].length() - "(INTJ*".length());
						for(int j = i + 1; j < sent.size(); ++j) {
							String[] next = sent.get(j);
							if(next[5].equals("*")) {
								remove.add(next);
							}
							else if(next[5].equals("*)")) {
								remove.add(next);
								String[] nn = sent.get(j + 1);
								nn[5]  = prefix + nn[5];
								break;
							}
							else if(next[5].equals("*))")) {
								remove.add(next);
								if(prefix.indexOf('(') == -1) {
									for(int k = i - 1; k >= 0; --k) {
										String[] prev = sent.get(k);
										if(remove.contains(prev)) {
											continue;
										}
										prev[5] = prev[5] + ")";
										break;
									}
								}
								break;
							}
							else if(next[5].equals("*)))")) {
								remove.add(next);
								if(prefix.indexOf('(') == -1) {
									for(int k = i - 1; k >= 0; --k) {
										String[] prev = sent.get(k);
										if(remove.contains(prev)) {
											continue;
										}
										prev[5] = prev[5] + "))";
										break;
									}
								}
								break;
							}
							else if(next[5].charAt(0) == '(' && next[5].charAt(1) != '(' 
									&& next[5].charAt(next[5].length() - 1) == ')'
									&& next[5].charAt(next[5].length() - 2) != ')') {
								remove.add(next);
							}
							else {
								System.err.println("UH error1: " + token[3] + " " + token[5] + "|" + next[3] + " " + next[5]);
								System.exit(0);
							}
						}
					}
					else if(token[5].startsWith("(INTJ*)")) {
						String suffix = token[5].substring("(INTJ*)".length());
						for(int j = i - 1; j >= 0; --j) {
							String[] prev = sent.get(j);
							if(remove.contains(prev)) {
								continue;
							}
							prev[5] = prev[5] + suffix;
							break;
						}
					}
					else if(token[5].endsWith("*")) {
						String[] next = sent.get(i + 1);
						next[5] = token[5].substring(0, token[5].length() - "*".length()) + next[5];
					}
					else if(token[5].startsWith("*")) {
						String suffix = token[5].substring("*".length());
						for(int j = i - 1; j >= 0; --j) {
							String[] prev = sent.get(j);
							if(remove.contains(prev)) {
								continue;
							}
							prev[5] = prev[5] + suffix;
							break;
						}
					}
					else if(token[5].equals("(NP*)")) {
						continue;
					}
					else {
						System.err.println("UH error2: " + token[3] + " " + token[5]);
					}
				}
			}
			sent.removeAll(remove);
		}
	}
	
	/**
	 * remove sentences of length 0
	 * @param sents
	 */
	protected static void removeZeroSent(List<List<String[]>> sents) {
		Set<List<String[]>> remove = new HashSet<List<String[]>>();
		for(List<String[]> sent : sents) {
			if(sent.size() == 0) {
				remove.add(sent);
			}
		}
		sents.removeAll(remove);
	}
	
	protected static void removeHYPHDashes(List<List<String[]>> sents) {
		for(List<String[]> sent : sents) {
			boolean hasHYPH = false;
			do {
				Set<String[]> remove = new HashSet<String[]>();
				for(int i = 0; i < sent.size(); ++i) {
					String[] token = sent.get(i);
					if(token[3].equals("-") && token[4].equals("HYPH")) {
						String[] prev = sent.get(i - 1);
						String[] next = sent.get(i + 1);
						if(prev[5].endsWith("*") && token[5].equals("*") && next[5].startsWith("*")) {
							prev[3] = prev[3] + token[3] + next[3];
							prev[5] = prev[5] + next[5].substring(1);
							remove.add(token);
							remove.add(next);
							break;
						}
						else {
							System.err.println("HYPH error: " + prev[3] + " " + token[3] + " " + next[3]);
						}
					}
				}
				if(remove.isEmpty()) {
					hasHYPH = false;
				}
				else{
					hasHYPH = true;
					sent.removeAll(remove);
				}
			} while(hasHYPH);
		}
	}
	
	private static List<List<String[]>> getNextDoc(BufferedReader inputReader) throws IOException {
		List<List<String[]>> sents = new LinkedList<List<String[]>>();
		
		String line = inputReader.readLine();
		if(line == null || line.length() == 0) {
			return null;
		}
		
		while(!line.equals("#end document")) {
			List<String[]> sent = new LinkedList<String[]>();
			while (line != null && line.length() != 0) {
				sent.add(line.split(" +"));
				line = inputReader.readLine();
			}
			sents.add(sent);
			line = inputReader.readLine();
		}
		return sents;
	}

	private static void writeDoc(BufferedWriter writer, String title, List<List<String[]>> sents) throws IOException {
		writer.write(title);
		writer.newLine();
		for(List<String[]> sent : sents) {
			int columns = sent.get(0).length;
			int[] maxLength = new int[columns];
			for(String[] token : sent) {
				for(int i = 0; i < columns; ++i) {
					maxLength[i] = Math.max(maxLength[i], token[i].length());
				}
			}
			
			for(String[] token : sent) {
				for(int i = 0; i < columns; ++i) {
					if(i > 0) {
						writer.write(getContinunousWhiteSpace(maxLength[i] - token[i].length() + 3));
					}
					writer.write(token[i]);
				}
				writer.newLine();
			}
			writer.newLine();
		}
		writer.write("#end document");
		writer.newLine();
		writer.flush();
	}
	
	private static String getContinunousWhiteSpace(int n){
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < n; ++i){
			sb.append(" ");
		}
		return sb.toString();
	}
}
