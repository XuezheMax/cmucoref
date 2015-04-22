package cmucoref.io;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.zip.GZIPOutputStream;

public class ObjectWriter {
private ObjectOutputStream out = null;
	
	public ObjectWriter(String file){
		try {
			out = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(file + ".gz")));
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	public void writeObject(Object obj) throws IOException{
		out.writeObject(obj);
	}
	
	public void writeInt(int val) throws IOException {
		out.writeInt(val);
	}
	
	public void writeBoolean(boolean val) throws IOException {
		out.writeBoolean(val);
	}
	
	public void close(){
		try {
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	public void reset() throws IOException{
		out.reset();
	}
}
