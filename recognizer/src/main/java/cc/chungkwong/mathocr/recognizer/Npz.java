package cc.chungkwong.mathocr.recognizer;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Npz {
    public static Map<String,Tensor> load(InputStream in) throws IOException {
        in=new BufferedInputStream(in);
        ZipInputStream zip=new ZipInputStream(in);
        Map<String,Tensor> data=new HashMap<String,Tensor>();
        ZipEntry entry;
        while((entry=zip.getNextEntry())!=null){
            String name=entry.getName();
            if(name.endsWith(".npy")){
                System.out.println(name);
                try{
                    Tensor tensor=loadTensor(zip);
                    System.out.println(Arrays.toString(tensor.getShape()));
                    zip.closeEntry();
                    data.put(name.substring(0,name.length()-4),tensor);
                }catch(IOException ex){
                    Logger.getLogger(Npz.class.getName()).log(Level.WARNING,"",ex);
                }
            }
        }
        zip.close();
        return data;
    }
    private static Tensor loadTensor(ZipInputStream zip) throws IOException{
        DataInputStream in=new DataInputStream(zip);
        if(in.readUnsignedByte()!=0x93||in.readUnsignedByte()!='N'||in.readUnsignedByte()!='U'||in.readUnsignedByte()!='M'||in.readUnsignedByte()!='P'||in.readUnsignedByte()!='Y'){
            throw new IOException("Wrong magic string");
        }
        int majorVersion=in.read();
        int minorVersion=in.read();
        int headerLength=in.readUnsignedByte()|in.readUnsignedByte()<<8;
        byte[] buf=new byte[headerLength];
        in.read(buf);
        String header=new String(buf, "UTF-8").trim().replaceAll(" ","");
        Matcher matcher= Pattern.compile("\\{'descr':'([^']+)','fortran_order':(True|False),'shape':\\(([^)]*)\\),\\}").matcher(header);
        matcher.matches();
        String type=matcher.group(1);
        if(!"<f4".equals(type)){
            throw new IOException(type+" is not supported");
        }
        String fortran=matcher.group(2);
        if("True".equals(type)){
            throw new IOException("Fortran is not supported");
        }
        List<Integer> dimensions=new ArrayList<Integer>();
        for(String dim:matcher.group(3).split(",")){
            if(!dim.trim().isEmpty()){
                dimensions.add(Integer.parseInt(dim));
            }
        }
        int[] shape=new int[dimensions.size()];
        for(int i=0;i<dimensions.size();i++){
            shape[i]=dimensions.get(i);
        }
        Tensor tensor=new Tensor(shape);
        float[] components=tensor.getComponents();
        for(int i=0, size=components.length;i<size;i++){
            components[i]=Float.intBitsToFloat(in.readUnsignedByte()|in.readUnsignedByte()<<8|in.readUnsignedByte()<<16|in.readUnsignedByte()<<24);
        }
        return tensor;
    }
}
