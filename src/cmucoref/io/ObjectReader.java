package cmucoref.io;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

public class ObjectReader {
private ObjectInputStream in = null;
	
	public ObjectReader(String file){
		try {
			in = new ObjectInputStream(new FileInputStream(file));
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	public Object readObject() throws ClassNotFoundException, IOException{
		return in.readObject();
	}
	
	public int readInt() throws IOException {
		return in.readInt();
	}
	
	public boolean readBoolean() throws IOException {
		return in.readBoolean();
	}
	
	public void close(){
		try {
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}
}
