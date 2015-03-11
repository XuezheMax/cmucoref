package cmucoref.document;

import java.io.File;
import cmucoref.io.AnnotatedDocumentReader;

public class AnnotatedDocumentCleaner {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String dirPath = args[0];
		File dir = new File(dirPath);
		File[] files = dir.listFiles();
		
		for(File file : files){
			System.out.println(file.getName());
		}
	}

}
