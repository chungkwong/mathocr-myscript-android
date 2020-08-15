package cc.chungkwong.mathocr.recognizer;

import org.bytedeco.openblas.global.openblas;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Tensor {
    private static final boolean USE_OPENBLAS;
    static{
        float[] a={1f,2f,3f,4f};
        float[] b={7f,8f};
        float[] c={0f,0f};
        try{
            openblas.cblas_sgemm(openblas.CblasRowMajor,openblas.CblasNoTrans,openblas.CblasNoTrans,2,1,2,1.0f,a,2,b,1,0.0f,c,1);
        }catch(Exception ex){
            Logger.getLogger(Tensor.class.getName()).log(Level.SEVERE,null,ex);
        }
        USE_OPENBLAS=c[1]!=0f;
    }
    public static void setThread(int num){
        if(USE_OPENBLAS){
            org.bytedeco.openblas.presets.openblas.blas_set_num_threads(num);
        }
    }
    private final int offset;
    private final int[] shape;
    private final float[] components;
    private final int size;
    private final int[] strides;
    public Tensor(int[] shape){
        this(shape,new float[getSize(shape)]);
    }
    public Tensor(int[] shape,float[] components){
        this(shape,components,0);
    }
    public Tensor(int[] shape,float[] components,int offset){
        this.offset=offset;
        this.shape=shape;
        this.components=components;
        this.strides=new int[shape.length];
        int tmp=1;
        for(int i=shape.length-1;i>=0;i--){
            strides[i]=tmp;
            tmp*=shape[i];
        }
        this.size=tmp;
    }
    public int[] getShape(){
        return shape;
    }
    public int getDimension(){
        return shape.length;
    }
    public float[] getComponents(){
        return components;
    }
    public int getOffset(){
        return offset;
    }
    public int getSize(){
        return size;
    }
    public int[] getStrides(){
        return strides;
    }
    public float get(int... indices){
        int index=offset;
        for(int i=0;i<indices.length;i++){
            index+=strides[i]*indices[i];
        }
        return components[index];
    }
    public Tensor getSubTensor(int... indices){
        int targetOffset=offset;
        for(int i=0;i<indices.length;i++){
            targetOffset+=strides[i]*indices[i];
        }
        int[] targetShape= Arrays.copyOfRange(shape,indices.length,shape.length);
        return new Tensor(targetShape,components,targetOffset);
    }
    public void setTo(Tensor tensor){
        if(!Arrays.equals(shape,tensor.shape)){
            throw new RuntimeException(Arrays.toString(shape)+" cannot be set to"+Arrays.toString(tensor.shape));
        }
        System.arraycopy(tensor.components,tensor.offset,components,offset,getSize());
    }
    public Tensor reshape(int... targetShape){
        if(getSize(shape)!=getSize(targetShape)){
            throw new RuntimeException(Arrays.toString(shape)+" cannot be reshaped to "+Arrays.toString(targetShape));
        }
        return new Tensor(targetShape,components,offset);
    }
    public Tensor transpose(){
        int[] targetShape=new int[shape.length];
        for(int i=0, j=shape.length-1;i<shape.length;i++,j--){
            targetShape[i]=shape[j];
        }
        Tensor t=new Tensor(targetShape);
        for(int i=0, ind=0;i<shape[1];i++){
            for(int j=0;j<shape[0];j++,ind++){
                t.components[ind]=components[j*shape[1]+i];
            }
        }
        return t;
    }
    public Tensor tanh(){
        Tensor t=new Tensor(shape);
        for(int i=offset, j=0;j<size;i++,j++){
            t.components[j]=tanh(components[i]);
        }
        return t;
    }
    private static float tanh(float x){
        if(Float.isNaN(x)){
            return x;
        }
        if(x>10.0){
            return 1.0f;
        }
        if(x<-10){
            return -1.0f;
        }
        if(x==0){
            return x;
        }
        boolean negate=false;
        if(x<0.0){
            x=-x;
            negate=true;
        }
        float result;
        if(x>=0.5){
            double tmp=Math.exp(x*2);
            result=(float)((tmp-1)/(tmp+1));
        }else{
            double tmp=Math.expm1(x*2);
            result=(float)(tmp/(tmp+2));
        }
        if(negate){
            result=-result;
        }
        return result;
    }
    public Tensor exp(){
        Tensor t=new Tensor(shape);
        for(int i=offset, j=0;j<size;i++,j++){
            t.components[j]=(float)Math.exp(components[i]);
        }
        return t;
    }
    public Tensor sigmoid(){
        Tensor t=new Tensor(shape);
        for(int i=offset, j=0;j<size;i++,j++){
            t.components[j]=1/(1+(float)Math.exp(-components[i]));
        }
        return t;
    }
    public Tensor softmax(){
        if(shape.length>1){
            Tensor[] tensors=new Tensor[shape[0]];
            for(int i=0;i<shape[0];i++){
                tensors[i]=getSubTensor(i).softmax();
            }
            return stack(tensors);
        }else{
            float sum=0;
            Tensor tensor=new Tensor(shape);
            float max=Float.NEGATIVE_INFINITY;
            for(int i=0, j=offset;i<size;i++,j++){
                float tmp=components[j];
                if(tmp>max){
                    max=tmp;
                }
            }
            for(int i=0, j=offset;i<size;i++,j++){
                float tmp=(float)Math.exp(components[j]-max);
                tensor.components[i]=tmp;
                sum+=tmp;
            }
            for(int i=0;i<size;i++){
                tensor.components[i]/=sum;
            }
            return tensor;
        }
    }
    public Tensor max(int axis){
        int[] targetShape=Arrays.copyOf(shape,shape.length-1);
        System.arraycopy(shape,axis+1,targetShape,axis,shape.length-axis-1);
        Tensor t=new Tensor(targetShape);
        Arrays.fill(t.components,Float.NEGATIVE_INFINITY);
        int m=strides[axis]*shape[axis], n=strides[axis];
        for(int iOld=offset, end=offset+size, count=0;iOld<end;iOld++,count++){
            int index=(count/m)*n+count%n;
            if(components[iOld]>t.components[index]){
                t.components[index]=components[iOld];
            }
        }
        return t;
    }
    public Tensor sum(int axis,boolean keepDim){
        int[] targetShape;
        if(keepDim){
            targetShape=Arrays.copyOf(shape,shape.length);
            targetShape[axis]=1;
        }else{
            targetShape=Arrays.copyOf(shape,shape.length-1);
            System.arraycopy(shape,axis+1,targetShape,axis,shape.length-axis-1);
        }
        Tensor t=new Tensor(targetShape);
        int m=strides[axis]*shape[axis], n=strides[axis];
        for(int iOld=offset, end=offset+size, count=0;iOld<end;iOld++,count++){
            t.components[(count/m)*n+count%n]+=components[iOld];
        }
        return t;
    }
    public Tensor mean(int axis,boolean keepDim){
        Tensor t=sum(axis,keepDim);
        int d=shape[axis];
        for(int i=t.offset, end=t.offset+t.size;i<end;i++){
            t.components[i]/=d;
        }
        return t;
    }
    public Tensor slice(int part,int dim){
        if(shape.length==3){
            int[] targetShape={shape[0],shape[1],dim};
            Tensor tensor=new Tensor(targetShape);
            int delta=shape[2];
            for(int i=0, iOld=part*dim, iNew=0;i<shape[0];i++){
                for(int j=0;j<shape[1];j++,iOld+=delta,iNew+=dim){
                    System.arraycopy(components,iOld,tensor.components,iNew,dim);
                }
            }
            return tensor;
        }else{
            int[] targetShape={shape[0],dim};
            Tensor tensor=new Tensor(targetShape);
            int delta=shape[1];
            for(int i=0, iOld=part*dim, iNew=0;i<shape[0];i++,iOld+=delta,iNew+=dim){
                System.arraycopy(components,iOld,tensor.components,iNew,dim);
            }
            return tensor;
        }
    }
    public Tensor conv(Tensor filters){
        int samples=shape[0];
        int steps=shape[1];
        int outChannels=filters.shape[0];
        int window=filters.shape[1];
        Tensor tensor=new Tensor(new int[]{steps,samples,outChannels});
        float[] filterComponents=filters.components;
        float[] tensorComponents=tensor.components;
        for(int step=0, offset=0;step<steps;step++){
            int startStep=Math.max(step-window/2,0);
            int endStep=Math.min(step+window/2+1,steps);
            int startWindow=step>=window/2?window-1:window/2+step;
            for(int sample=0, offsetIStart=startStep;sample<samples;sample++,offsetIStart+=steps){
                for(int outChannel=0, offsetFStart=startWindow;outChannel<outChannels;outChannel++,offset++,offsetFStart+=window){
                    float sum=0;
                    for(int i=startStep, offsetI=offsetIStart, offsetF=offsetFStart;i<endStep;i++,offsetI++,offsetF--){
                        sum+=components[offsetI]*filterComponents[offsetF];
                    }
                    tensorComponents[offset]=sum;
                }
            }
        }
        return tensor;
    }
    public Tensor tile(int copies){
        int[] targetShape=Arrays.copyOf(shape,shape.length);
        targetShape[targetShape.length-2]*=copies;
        Tensor tensor=new Tensor(targetShape);
        int n=shape[shape.length-1];
        int m=size/n;
        for(int i=0, iOld=offset, iNew=0;i<m;i++,iOld+=n){
            for(int j=0;j<copies;j++,iNew+=n){
                System.arraycopy(components,iOld,tensor.components,iNew,n);
            }
        }
        return tensor;
    }
    public Tensor concat(Tensor x,int axis){
        int[] targetShape=Arrays.copyOf(shape,shape.length);
        targetShape[axis]+=x.getShape()[axis];
        Tensor t=new Tensor(targetShape);
        int m=size/strides[axis]/shape[axis], n=strides[axis]*shape[axis], l=strides[axis]*x.shape[axis];
        for(int i=0, index=0;i<m;i++){
            System.arraycopy(components,i*n,t.components,index,n);
            index+=n;
            System.arraycopy(x.components,i*l,t.components,index,l);
            index+=l;
        }
        return t;
    }
    public Tensor reverse(){
        Tensor t=new Tensor(shape);
        for(int i=t.shape[0]-1, j=0;i>=0;i--,j++){
            t.getSubTensor(i).setTo(getSubTensor(j));
        }
        return t;
    }
    public Tensor downSample(){
        int[] targetShape=Arrays.copyOf(shape,shape.length);
        targetShape[0]=(shape[0]+1)/2;
        Tensor t=new Tensor(targetShape);
        for(int i=0, j=0;j<shape[0];i++,j+=2){
            t.getSubTensor(i).setTo(getSubTensor(j));
        }
        return t;
    }
    public Tensor add(Tensor x){
//		if(!Arrays.equals(shape,x.shape)){
//			throw new RuntimeException(Arrays.toString(shape)+" cannot be added to"+Arrays.toString(x.shape));
//		}
        Tensor t=new Tensor(shape);
        for(int i=offset, j=x.offset, k=0;k<size;i++,j++,k++){
            if(j==x.size+x.offset){
                j=x.offset;
            }
            t.components[k]=components[i]+x.components[j];
        }
        return t;
    }
    public Tensor subtract(Tensor x){
//		if(!Arrays.equals(shape,x.shape)){
//			throw new RuntimeException(Arrays.toString(shape)+" cannot be added to"+Arrays.toString(x.shape));
//		}
        Tensor t=new Tensor(shape);
        for(int i=offset, j=x.offset, k=0;k<size;i++,j++,k++){
            if(j==x.size+x.offset){
                j=x.offset;
            }
            t.components[k]=components[i]-x.components[j];
        }
        return t;
    }
    public Tensor multiplyElementwise(Tensor x){
        if(!Arrays.equals(shape,x.shape)){
            throw new RuntimeException(Arrays.toString(shape)+" cannot be added to"+Arrays.toString(x.shape));
        }
        Tensor t=new Tensor(shape);
        for(int i=offset, j=x.offset, k=0;k<size;i++,j++,k++){
            t.components[k]=components[i]*x.components[j];
        }
        return t;
    }
    public Tensor multiplyElementwise2(Tensor x){
        Tensor t=new Tensor(x.shape);
        int repeat=x.shape[x.shape.length-1];
        for(int i=offset-1, j=x.offset, k=0;k<x.size;j++,k++){
            if(k%repeat==0){
                ++i;
            }
            t.components[k]=components[i]*x.components[j];
        }
        return t;
    }
    public Tensor divideElementwise(Tensor x){
//		if(!Arrays.equals(shape,x.shape)){
//			throw new RuntimeException(Arrays.toString(shape)+" cannot be added to"+Arrays.toString(x.shape));
//		}
        Tensor t=new Tensor(shape);
        for(int i=offset, j=x.offset, k=0;k<size;i++,j++,k++){
            if(j==x.size+x.offset){
                j=x.offset;
            }
            t.components[k]=components[i]/x.components[j];
        }
        return t;
    }
    public Tensor multiply(Tensor x){
        int jCount=x.shape[1];
        int kCount=x.shape[0];
        int iCount=size/kCount;
        if(kCount!=shape[shape.length-1]){
            throw new RuntimeException(kCount+" is not "+shape[shape.length-1]);
        }
        int[] targetShape=Arrays.copyOf(shape,shape.length);
        targetShape[targetShape.length-1]=jCount;
        if(USE_OPENBLAS){
            float[] a=offset==0&&size==components.length?components:Arrays.copyOfRange(components,offset,offset+size);
            float[] b=x.offset==0&&x.size==x.components.length?x.components:Arrays.copyOfRange(x.components,x.offset,x.offset+x.size);
            float[] c=new float[iCount*jCount];
            openblas.cblas_sgemm(openblas.CblasRowMajor,openblas.CblasNoTrans,openblas.CblasNoTrans,iCount,jCount,kCount,1.0f,a,kCount,b,jCount,0.0f,c,jCount);
//			org.netlib.blas.Sgemm.sgemm("N","N",jCount,iCount,kCount,1.0f,b,0,jCount,a,0,kCount,0.0f,c,0,jCount);
//			org.jblas.NativeBlas.sgemm('N','N',jCount,iCount,kCount,1.0f,b,0,jCount,a,0,kCount,0.0f,c,0,jCount);
            return new Tensor(targetShape,c,0);
        }else{
            Tensor t=new Tensor(targetShape);
            for(int i=0, ind=0, ii=offset;i<iCount;i++,ii+=kCount){
                for(int j=0, jj=x.offset;j<jCount;j++,ind++,jj++){
                    for(int k=0, iii=ii, jjj=jj;k<kCount;k++,iii+=1,jjj+=jCount){
                        t.components[ind]+=components[iii]*x.components[jjj];
                    }
                }
            }
            return t;
//		FloatMatrix a=new FloatMatrix(kCount,iCount,components).transpose();
//		FloatMatrix b=new FloatMatrix(jCount,kCount,x.components).transpose();
//		FloatMatrix c=a.mmul(b).transpose();
//		return new Tensor(targetShape,c.data,0);
        }
    }
    @Override
    public String toString(){
        if(shape.length==2){
            StringBuilder buf=new StringBuilder();
            buf.append("Tensor: shape=");
            buf.append(Arrays.toString(shape));
            for(int i=0, ind=offset;i<shape[0];i++){
                buf.append('\n');
                for(int j=0;j<shape[1];j++,ind++){
                    buf.append(components[ind]).append('\t');
                }
            }
            return buf.toString();
        }
        return "Tensor(shape="+Arrays.toString(shape)+",data="+Arrays.toString(components)+")";
    }
    public static int getSize(int[] shape){
        int size=1;
        for(int i:shape){
            size*=i;
        }
        return size;
    }
    public static Tensor fill(float value,int... shape){
        Tensor t=new Tensor(shape);
        Arrays.fill(t.components,value);
        return t;
    }
    public static Tensor stack(Tensor... tensors){
        int[] shape=new int[tensors[0].getShape().length+1];
        shape[0]=tensors.length;
        System.arraycopy(tensors[0].shape,0,shape,1,tensors[0].shape.length);
        Tensor tensor=new Tensor(shape);
        int offset=0;
        for(Tensor t:tensors){
            System.arraycopy(t.components,t.offset,tensor.components,offset,t.size);
            offset+=t.size;
        }
        return tensor;
    }
}
