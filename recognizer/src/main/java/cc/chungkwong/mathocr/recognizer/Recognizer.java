package cc.chungkwong.mathocr.recognizer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import cc.chungkwong.mathocr.extractor.BoundBox;
import cc.chungkwong.mathocr.extractor.Trace;
import cc.chungkwong.mathocr.extractor.TracePoint;

public class Recognizer {
    private static Tap tap=null;
    public static List<Candidate> recognize(List<Trace> traces,int candidateCount,int lengthLimit){
        if(tap==null){
            tap=new Tap();
        }
        traces=simplify(traces,-1,0.99);
        double[][] feature=getFeature(traces);
        int m=feature.length;
        int n=feature[0].length;
        Tensor x=new Tensor(new int[]{m,n});
        float[] components=x.getComponents();
        for(int i=0, ind=0;i<m;i++){
            for(int j=0;j<n;j++,ind++){
                components[ind]=(float)feature[i][j];
            }
        }
        return tap.recognize(x,candidateCount,lengthLimit);
    }
    public static List<Trace> simplify(List<Trace> traceList, double distThresholdPre, double cosThresholdPre){
        double cosThreshold=cosThresholdPre*cosThresholdPre;
        if(distThresholdPre<0){
            int[] sizes=new int[traceList.size()];
            int i=-1;
            for (Trace trace : traceList) {
                BoundBox box = trace.getBoundBox();
                sizes[++i]=Math.max(box.getWidth(),box.getHeight());
            }
            Arrays.sort(sizes);
            if(sizes.length>0){
                distThresholdPre=0.08*sizes[sizes.length*3/4];
            }else{
                distThresholdPre=16;
            }
        }
        double distThreshold=distThresholdPre*distThresholdPre;
        List<Trace> simplified=new ArrayList<>(traceList.size());
        for(Trace trace:traceList){
            Trace simplifiedTrace=new Trace(new ArrayList<TracePoint>(trace.getPoints().size()));
            Iterator<TracePoint> iterator=trace.getPoints().iterator();
            if(iterator.hasNext()){
                TracePoint lastlast=iterator.next();
                simplifiedTrace.getPoints().add(lastlast);
                if(iterator.hasNext()){
                    TracePoint last=iterator.next();
                    while(iterator.hasNext()){
                        TracePoint next=iterator.next();
                        double dxForward=next.getX()-last.getX();
                        double dxBackward=last.getX()-lastlast.getX();
                        double dyForward=next.getY()-last.getY();
                        double dyBackward=last.getY()-lastlast.getY();
                        double innerProduct=dxForward*dxBackward+dyForward*dyBackward;
                        if(innerProduct<=0
                                ||innerProduct*innerProduct<cosThreshold
                                *(dxForward*dxForward+dyForward*dyForward)
                                *(dxBackward*dxBackward+dyBackward*dyBackward)){
                            if(dxBackward*dxBackward+dyBackward*dyBackward>distThreshold){
                                simplifiedTrace.getPoints().add(last);
                                lastlast=last;
                            }
                        }
                        last=next;
                    }
                    simplifiedTrace.getPoints().add(last);
                }
                simplified.add(simplifiedTrace);
            }
        }
//		System.out.println(distThreshold+"\t"+simplified.getTraces().stream().mapToInt((t)->t.getPoints().size()).average().getAsDouble());
        return simplified;
    }
    public static double[][] getFeature(List<Trace> traceList){
        double xSum=0, ySum=0, xxSum=0, xxCSum=0, length=0;
        int count=0;
        for(Trace trace:traceList){
            Iterator<TracePoint> iterator=trace.getPoints().iterator();
            if(iterator.hasNext()){
                TracePoint last=iterator.next();
                while(iterator.hasNext()){
                    TracePoint curr=iterator.next();
                    double len=Math.hypot(curr.getX()-last.getX(),curr.getY()-last.getY());
                    xSum+=(last.getX()+curr.getX())*len;
                    ySum+=(last.getY()+curr.getY())*len;
                    xxSum+=(last.getX()*last.getX()+curr.getX()*curr.getX())*len;
                    xxCSum+=(last.getX()*curr.getX())*len;
                    length+=len;
                    ++count;
                    last=curr;
                }
            }
        }
        if(!(length>0)){
            length=Trace.getBoundBox(traceList).getHeight()+1e-7;
        }
        double centerX=xSum*0.5/length;
        double centerY=ySum*0.5/length;
        double var=Math.sqrt(((xxSum+xxCSum)/3+centerX*centerX*length-centerX*xSum)/length);
        if(!(var>0)){
            var=Trace.getBoundBox(traceList).getHeight()*0.5+1e-7;
        }
        double[][] features=new double[count+traceList.size()][8];
        int index=0;
        for(Trace trace:traceList){
            Iterator<TracePoint> iterator=trace.getPoints().iterator();
            while(iterator.hasNext()){
                TracePoint curr=iterator.next();
                features[index][0]=(curr.getX()-centerX)/var;
                features[index][1]=(curr.getY()-centerY)/var;
                if(iterator.hasNext()){
                    features[index][6]=1.0;
                }else{
                    features[index][7]=1.0;
                }
                ++index;
            }
        }
        for(int i=0, imax=index-1;i<imax;i++){
            features[i][2]=features[i+1][0]-features[i][0];
            features[i][3]=features[i+1][1]-features[i][1];
        }
        for(int i=0, imax=index-2;i<imax;i++){
            features[i][4]=features[i+2][0]-features[i][0];
            features[i][5]=features[i+2][1]-features[i][1];
        }
        return features;
    }
}
