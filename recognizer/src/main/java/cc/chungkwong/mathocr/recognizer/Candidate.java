package cc.chungkwong.mathocr.recognizer;

import java.util.ArrayList;
import java.util.List;

public class Candidate {
    private final double score;
    private final int[][] alternatives;
    private final int[] attention;
    private final String[] dictionary;
    public Candidate(int[][] alternatives,int[] attention,double score,String[] dictionary){
        this.score=score;
        this.alternatives=alternatives;
        this.attention=attention;
        this.dictionary=dictionary;
    }
    public String getLatex(){
        StringBuilder buf=new StringBuilder();
        boolean start=true;
        for(int[] alternative:alternatives){
            if(alternative[0]!=0){
                if(start){
                    start=false;
                }else{
                    buf.append(' ');
                }
                buf.append(dictionary[alternative[0]]);
            }
        }
        return buf.toString();
    }
    public int getTokenCount(){
        return alternatives.length;
    }
    public int[] getAttention(){
        return attention;
    }
    public List<String> getAlternative(int index){
        int[] alternative=alternatives[index];
        List<String> tokens=new ArrayList<String>(alternative.length);
        for(int i:alternative){
            tokens.add(dictionary[i]);
        }
        return tokens;
    }
    public double getScore(){
        return score;
    }
    @Override
    public String toString(){
        StringBuilder buf=new StringBuilder();
        buf.append(score).append('\t');
        for(int[] alternative:alternatives){
            buf.append("[ ");
            for(int i:alternative){
                buf.append(dictionary[i]).append(' ');
            }
            buf.append("] ");
        }
        return buf.toString();
//		return score+"\t"+getLatex();
    }
}
