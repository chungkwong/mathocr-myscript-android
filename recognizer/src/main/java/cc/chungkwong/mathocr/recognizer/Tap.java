package cc.chungkwong.mathocr.recognizer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Tap {
    //	private final boolean[] downSample={false,false,true,true};
//	private final int downSampled=4;
    private final boolean[] downSample={false,false,false,false};
    private final int downSampled=1;
    private final Map<String,Tensor> weights;
    private final String[] dictionary;
    private final Ll1Grammar grammar;
    public Tap(){
        try{
            this.weights=Npz.load(getClass().getResourceAsStream("offline.npz"));
            this.dictionary=loadDictionry(getClass().getResourceAsStream("dictionary.txt"));
            this.grammar=Ll1Grammar.load(getClass().getResourceAsStream("grammar.txt"));
        }catch(IOException ex){
            Logger.getLogger(Tap.class.getName()).log(Level.SEVERE,null,ex);
            throw new RuntimeException();
        }
    }
    public Tap(Map<String,Tensor> weights,String[] dictionary,Ll1Grammar grammar){
        this.weights=weights;
        this.dictionary=dictionary;
        this.grammar=grammar;
    }
    public List<Candidate> recognize(Tensor x, int k, int maxlen){
        if(Thread.interrupted()){
            Logger.getLogger(Tap.class.getName()).log(Level.INFO,"Interrupted");
            return Collections.emptyList();
        }
        int numThread=Runtime.getRuntime().availableProcessors();
        ExecutorService threadPool= Executors.newFixedThreadPool(numThread);
        List<Double> sample_score=new ArrayList<Double>();
        List<int[][]> sample_alternative=new ArrayList<int[][]>();
        List<Tensor[]> sample_attention=new ArrayList<Tensor[]>();
        int live_k=1;
        int dead_k=0;
        List<Double> hyp_scores=new ArrayList<Double>(k);
        hyp_scores.add(0.0);
        List<int[][]> hyp_alternative=new ArrayList<int[][]>();
        hyp_alternative.add(new int[0][]);
        List<Tensor[]> hyp_attention=new ArrayList<Tensor[]>();
        hyp_attention.add(new Tensor[0]);
        List<Tensor> hyp_states=new ArrayList<Tensor>();
        List<List<String>> hyp_stack=new ArrayList<List<String>>();
        hyp_stack.add(Ll1Grammar.START);
        Tensor.setThread(numThread);
        Tensor[] ret=encode(x);
        Tensor.setThread(1);
        Tensor[] next_state={ret[0]};
        Tensor ctx0=ret[1];
        int[][] next_w={{-1}};
        int SeqL=x.getShape()[0];
        for(boolean down:downSample){
            if(down){
                SeqL=(SeqL+1)/2;
            }
        }
        Tensor[] next_alpha_past={new Tensor(new int[]{1,SeqL})};
        int batchCount=1;
        int voc_size=dictionary.length;
        int l=Math.min(k,voc_size);
        for(int ii=0;ii<maxlen;ii++){
            if(Thread.interrupted()){
                Logger.getLogger(Tap.class.getName()).log(Level.INFO,"Interrupted");
                return Collections.emptyList();
            }
//			System.out.println(ii);
            List<Future<Tensor[]>> futures=new ArrayList<Future<Tensor[]>>(batchCount);
            for(int i=0;i<batchCount;i++){
                final int[] next_w_batch=next_w[i];
                final Tensor next_state_batch=next_state[i];
                final Tensor next_alpha_past_batch=next_alpha_past[i];
                final Tensor ctx_batch=ctx0.tile(next_w_batch.length);
//				System.out.println("next_w="+Arrays.toString(next_w_batch));
//				System.out.println("ctx="+ctx_batch);
//				System.out.println("next_state="+next_state_batch);
//				System.out.println("next_alpha_past="+next_alpha_past_batch);
                futures.add(threadPool.submit(new Callable<Tensor[]>(){
                    public Tensor[] call() throws Exception{
                        return decode(next_w_batch,ctx_batch,next_state_batch,next_alpha_past_batch);
                    }
                }));
            }
            Tensor[] next_state_tmp=new Tensor[live_k];
            Tensor[] next_alpha_past_tmp=new Tensor[live_k];
            float[] next_p=new float[live_k*l];
            int[][] trans_word=new int[live_k][];
            for(int i=0, ind=0, ip=0;i<batchCount;i++){
                try{
                    ret=futures.get(i).get();
                }catch(InterruptedException| ExecutionException ex){
                    Logger.getLogger(Tap.class.getName()).log(Level.INFO,"Interrupted",ex);
                    Thread.interrupted();
                    return Collections.emptyList();
                }
                int batchSize=next_w[i].length;
                for(int j=0, pOffset=ret[0].getOffset();j<batchSize;j++,ind++,pOffset+=voc_size){
                    next_state_tmp[ind]=ret[1].getSubTensor(j);
                    next_alpha_past_tmp[ind]=ret[2].getSubTensor(j);
                    trans_word[ind]=argsortDecreasing(ret[0].getComponents(),pOffset,pOffset+voc_size,k);
                    for(int n=0;n<trans_word[ind].length;n++){
                        int m=trans_word[ind][n];
                        next_p[ip++]=ret[0].getComponents()[m];
                        trans_word[ind][n]-=pOffset;
                    }
                }
            }
//			System.out.println("next_w="+Arrays.toString(next_w));
//			System.out.println("next_state="+next_state);
//			System.out.println("next_alpha_past="+next_alpha_past[0]);
//			System.out.println(Arrays.toString(next_p.getShape())+", "+Arrays.toString(next_state.getShape())+", "+Arrays.toString(next_alpha_past.getShape()));
//			System.out.println("next_p="+next_p);
            double[] cand_flat=new double[next_p.length];
            for(int i=0, ind=0;i<hyp_scores.size();i++){
                double old=hyp_scores.get(i);
                for(int j=0;j<l;j++,ind++){
                    cand_flat[ind]=old-Math.log(next_p[ind]);
                }
            }
            int[] ranks_flat=argsort(cand_flat,0,cand_flat.length,k-dead_k);
//			System.out.println(Arrays.toString(ranks_flat));
            int[] trans_indices=new int[ranks_flat.length];
            int[] word_indices=new int[ranks_flat.length];
            double[] costs=new double[ranks_flat.length];
            for(int i=0;i<ranks_flat.length;i++){
                int index=ranks_flat[i];
                int trans=index/l;
                trans_indices[i]=trans;
                word_indices[i]=trans_word[trans][index%l];
                costs[i]=cand_flat[index];
            }
//			System.out.println(Arrays.toString(trans_indices)+":"+Arrays.toString(word_indices));
//			System.out.println("cost="+Arrays.toString(costs));
            List<Double> new_hyp_scores=new ArrayList<Double>();
            List<int[][]> new_hyp_alternative=new ArrayList<int[][]>();
            List<Tensor[]> new_hyp_attention=new ArrayList<Tensor[]>();
            List<Tensor> new_hyp_states=new ArrayList<Tensor>();
            List<List<String>> new_hyp_stack=new ArrayList<List<String>>();
            List<Tensor> new_hyp_alpha_past=new ArrayList<Tensor>();
            for(int idx=0;idx<ranks_flat.length;idx++){
                int t=trans_indices[idx];
                int w=word_indices[idx];
                new_hyp_scores.add(costs[idx]);
                int[][] alternative=hyp_alternative.get(t);
                alternative= Arrays.copyOf(alternative,alternative.length+1);
                Tensor[] attention=hyp_attention.get(t);
                attention=Arrays.copyOf(attention,attention.length+1);
                int[] selected=new int[l];
                selected[0]=w;
                int tmp=0;
                for(int ww:trans_word[t]){
                    if(ww!=w){
                        selected[++tmp]=ww;
                    }
                }
                alternative[alternative.length-1]=selected;
                new_hyp_alternative.add(alternative);
                attention[attention.length-1]=next_alpha_past_tmp[t];
                new_hyp_attention.add(attention);
                new_hyp_states.add(next_state_tmp[t]);
                new_hyp_stack.add(grammar.next(hyp_stack.get(t),dictionary[w]));
                new_hyp_alpha_past.add(next_alpha_past_tmp[t]);
            }
            int new_live_k=0;
            hyp_scores.clear();
            hyp_alternative.clear();
            hyp_attention.clear();
            hyp_states.clear();
            hyp_stack.clear();
            List<Tensor> hyp_alpha_past=new ArrayList<Tensor>();
            for(int idx=0;idx<new_hyp_alternative.size();idx++){
                if(new_hyp_alternative.get(idx)[new_hyp_alternative.get(idx).length-1][0]==0){
                    if(Ll1Grammar.isFinished(new_hyp_stack.get(idx))){
                        sample_score.add(new_hyp_scores.get(idx));
                        sample_alternative.add(new_hyp_alternative.get(idx));
                        sample_attention.add(new_hyp_attention.get(idx));
                        dead_k+=1;
                    }
                }else{
                    if(!Ll1Grammar.isFailed(new_hyp_stack.get(idx))){
                        new_live_k+=1;
                        hyp_scores.add(new_hyp_scores.get(idx));
                        hyp_alternative.add(new_hyp_alternative.get(idx));
                        hyp_attention.add(new_hyp_attention.get(idx));
                        hyp_states.add(new_hyp_states.get(idx));
                        hyp_stack.add(new_hyp_stack.get(idx));
                        hyp_alpha_past.add(new_hyp_alpha_past.get(idx));
                    }
                }
            }
            live_k=new_live_k;
            if(new_live_k<1){
                break;
            }
            if(dead_k>=k){
                break;
            }
            int maxBatchSize=(live_k+numThread-1)/numThread;
            batchCount=(live_k+maxBatchSize-1)/maxBatchSize;
            next_w=new int[batchCount][];
            next_state=new Tensor[batchCount];
            next_alpha_past=new Tensor[batchCount];
            for(int i=0, j=0;i<live_k;i+=maxBatchSize,j++){
                int iEnd=Math.min(i+maxBatchSize,live_k);
                next_w[j]=new int[iEnd-i];
                for(int lll=i;lll<iEnd;lll++){
                    int[][] ll=hyp_alternative.get(lll);
                    next_w[j][lll-i]=ll[ll.length-1][0];
                }
                next_state[j]=Tensor.stack(hyp_states.subList(i,iEnd).toArray(new Tensor[0]));
                next_alpha_past[j]=Tensor.stack(hyp_alpha_past.subList(i,iEnd).toArray(new Tensor[0]));
            }
        }
        threadPool.shutdown();
        List<Candidate> candidates=new ArrayList<Candidate>(sample_score.size());
        for(int i=0;i<sample_score.size();i++){
            candidates.add(new Candidate(sample_alternative.get(i),extractAttention(sample_attention.get(i),x),sample_score.get(i),dictionary));
        }
        Collections.sort(candidates,new Comparator<Candidate>(){
            public int compare(Candidate t,Candidate t1){
                return Double.compare(t.getScore(),t1.getScore());
            }
        });
        return candidates;
    }
    private int[] extractAttention(Tensor[] attention,Tensor x){
        float[][] a=new float[attention.length][];
        if(attention.length>0){
            a[0]=attention[0].getComponents();
            for(int i=1;i<attention.length;i++){
                a[i]=attention[i].subtract(attention[i-1]).getComponents();
            }
            List<Integer> result=new ArrayList<Integer>();
            int steps=x.getShape()[0];
            int lastColumn=x.getShape()[1]-1;
            float[] score=new float[attention.length];
            for(int i=0;i<steps;i++){
                int j=i/downSampled;
                for(int k=0;k<score.length;k++){
                    score[k]+=a[k][j];
                }
                if(x.get(i,lastColumn)==1.0f){
                    float max=Float.NEGATIVE_INFINITY;
                    int maxIndex=-1;
                    for(int k=0;k<score.length;k++){
                        float f=score[k];
                        if(f>max){
                            max=f;
                            maxIndex=k;
                        }
                    }
                    result.add(maxIndex);
                    Arrays.fill(score,0);
                }
            }
            int[] array=new int[result.size()];
            for(int i=0;i<result.size();i++){
                array[i]=result.get(i);
            }
            return array;
        }else{
            return new int[0];
        }
    }
    private Tensor[] encode(Tensor x){
        Tensor xr=x.reverse();
        Tensor h=x;
        Tensor hr=xr;
//		System.out.println("x="+x);
//		System.out.println("xr="+xr);
        for(int i=0;i<downSample.length;i++){
            Tensor proj=encode(h,"encoder"+i);
            Tensor projr=encode(hr,"encoder_r"+i);
            h=proj.concat(projr.reverse(),1);
//			System.out.println("proj="+proj);
//			System.out.println("projr="+projr);
//			System.out.println("h="+h);
            if(downSample[i]){
                h=h.downSample();
//				System.out.println("h="+h);
            }
            hr=h.reverse();
//			System.out.println("hr="+hr);
        }
        Tensor ctx=h;
        Tensor ctx_mean=ctx.mean(0,true);
//		System.out.println("ctx_mean="+ctx_mean);
        Tensor initState=ctx_mean.multiply(weights.get("ff_state_W")).add(weights.get("ff_state_b")).tanh();
        return new Tensor[]{initState,ctx.reshape(ctx.getShape()[0],1,ctx.getShape()[1])};
    }
    private Tensor encode(Tensor x,String layerName){
        int nsteps=x.getShape()[0];
        int dim=weights.get(layerName+"_Ux").getShape()[1];
        Tensor state_below_=x.multiply(weights.get(layerName+"_W")).add(weights.get(layerName+"_b"));
        Tensor state_belowx=x.multiply(weights.get(layerName+"_Wx")).add(weights.get(layerName+"_bx"));
        Tensor x_=state_below_, xx_=state_belowx, h_=Tensor.fill(0,1,dim), U=weights.get(layerName+"_U"), Ux=weights.get(layerName+"_Ux");
        Tensor seq=new Tensor(new int[]{nsteps,dim});
//		System.out.println("x_="+x_);
//		System.out.println("xx_="+xx_);
        for(int i=0;i<nsteps;i++){
            Tensor preact=h_.multiply(U).add(x_.getSubTensor(i));
//			System.out.println("preact="+preact);
            Tensor r=preact.slice(0,dim).sigmoid();
            Tensor u=preact.slice(1,dim).sigmoid();
//			System.out.println("r="+r);
//			System.out.println("u="+u);
            Tensor preactx=h_.multiply(Ux).multiplyElementwise(r).add(xx_.getSubTensor(i));
            Tensor h=preactx.tanh();
//			System.out.println("h="+h);
            h=u.multiplyElementwise(h_).add(Tensor.fill(1,u.getShape()).subtract(u).multiplyElementwise(h));
//			System.out.println("h="+h);
            seq.getSubTensor(i).setTo(h.reshape(dim));
            h_=h;
        }
        return seq;
    }
    private Tensor[] decode(int[] y,Tensor ctx,Tensor init_state,Tensor alpha_past){
        Tensor wembDec=weights.get("Wemb_dec");
        int dimWord=wembDec.getShape()[1];
        Tensor emb=new Tensor(new int[]{y.length,dimWord});
        for(int i=0;i<y.length;i++){
            if(y[i]>=0){
                emb.getSubTensor(i).setTo(wembDec.getSubTensor(y[i]));
            }
        }
        Tensor[] step=decodeStep(emb,ctx,init_state,alpha_past);
        Tensor next_state=step[0];
        Tensor ctxs=step[1];
        Tensor next_alpha_past=step[2];
        Tensor logit_lstm=next_state.multiply(weights.get("ff_logit_lstm_W")).add(weights.get("ff_logit_lstm_b"));
        Tensor logit_prev=emb.multiply(weights.get("ff_logit_prev_W")).add(weights.get("ff_logit_prev_b"));
        Tensor logit_ctx=ctxs.multiply(weights.get("ff_logit_ctx_W")).add(weights.get("ff_logit_ctx_b"));
        Tensor logit=logit_lstm.add(logit_prev).add(logit_ctx);
        logit=logit.reshape(logit.getShape()[0],logit.getShape()[1]/2,2).max(2);
        logit=logit.multiply(weights.get("ff_logit_W")).add(weights.get("ff_logit_b"));
        Tensor next_probs=logit.softmax();
        return new Tensor[]{next_probs,next_state,next_alpha_past};
    }
    private Tensor[] decodeStep(Tensor emb,Tensor ctx,Tensor init_state,Tensor alpha_past){
        Tensor x_=emb.multiply(weights.get("decoder_W")).add(weights.get("decoder_b"));
        Tensor xx_=emb.multiply(weights.get("decoder_Wx")).add(weights.get("decoder_bx"));
        Tensor yg=emb.multiply(weights.get("decoder_Wyg")).add(weights.get("decoder_byg"));
        Tensor h_=init_state;
        Tensor alpha_past_=alpha_past;
        Tensor pctx_=ctx.multiply(weights.get("decoder_Wc_att")).add(weights.get("decoder_b_att"));
        Tensor cc_=ctx;
        Tensor preact1=h_.multiply(weights.get("decoder_U")).add(x_).sigmoid();
        int dim=weights.get("decoder_Wcx").getShape()[1];
        Tensor r1=preact1.slice(0,dim);
        Tensor u1=preact1.slice(1,dim);
        Tensor preactx1=h_.multiply(weights.get("decoder_Ux")).multiplyElementwise(r1).add(xx_).tanh();
        Tensor h1=u1.multiplyElementwise(h_).add(Tensor.fill(1.0f,u1.getShape()).subtract(u1).multiplyElementwise(preactx1));
        Tensor g_m=h_.multiply(weights.get("decoder_Whg")).add(weights.get("decoder_bhg")).add(yg).sigmoid();
        Tensor mt=h1.multiply(weights.get("decoder_Umg")).tanh().multiplyElementwise(g_m);
        Tensor pstate_=h1.multiply(weights.get("decoder_W_comb_att"));
        Tensor filter=weights.get("decoder_conv_Q");
        filter=filter.reshape(filter.getShape()[0],filter.getShape()[2]);
        Tensor cover_F=alpha_past_.conv(filter);
        Tensor cover_vector=cover_F.multiply(weights.get("decoder_conv_Uf")).add(weights.get("decoder_conv_b"));
        Tensor pctx__=cover_vector.add(pctx_).add(pstate_).tanh();
        Tensor alpha=pctx__.multiply(weights.get("decoder_U_att")).add(weights.get("decoder_c_tt"));
        Tensor pctx_when=mt.multiply(weights.get("decoder_W_m_att")).add(pstate_).tanh();
        Tensor alpha_when=pctx_when.multiply(weights.get("decoder_U_when_att")).add(weights.get("decoder_c_when_att")).exp();
        alpha=alpha.reshape(alpha.getShape()[0],alpha.getShape()[1]).exp();
        Tensor alpha_mean=alpha.mean(0,true);
        alpha_when=alpha_mean.concat(alpha_when.transpose(),0);
        alpha=alpha.divideElementwise(alpha.sum(0,true));
        alpha_when=alpha_when.divideElementwise(alpha_when.sum(0,true));
        Tensor beta=alpha_when.getSubTensor(alpha_when.getShape()[0]-1);//boardcast
        alpha_past=alpha_past_.add(alpha.transpose());
        Tensor ctx_=alpha.multiplyElementwise2(cc_).sum(0,false);//boardcast
        ctx_=beta.multiplyElementwise2(mt).add(Tensor.fill(1.0f,beta.getShape()).subtract(beta).multiplyElementwise2(ctx_));
        Tensor preact2=h1.multiply(weights.get("decoder_U_nl")).add(weights.get("decoder_b_nl")).add(ctx_.multiply(weights.get("decoder_Wc"))).sigmoid();
        Tensor r2=preact2.slice(0,dim);
        Tensor u2=preact2.slice(1,dim);
        Tensor preactx2=h1.multiply(weights.get("decoder_Ux_nl")).add(weights.get("decoder_bx_nl")).multiplyElementwise(r2).add(ctx_.multiply(weights.get("decoder_Wcx"))).tanh();
        Tensor h2=u2.multiplyElementwise(h1).add(Tensor.fill(1,u2.getShape()).subtract(u2).multiplyElementwise(preactx2));
        Tensor next_state=h2, ctxs=ctx_, next_alpha_past=alpha_past;
        return new Tensor[]{next_state,ctxs,next_alpha_past};
    }
    private static int[] argsort(double[] cand_flat,int start,int end,int length){
        int resultLength=Math.min(end-start,length);
        int[] result=new int[resultLength];
        double[] resultScore=new double[resultLength];
        Arrays.fill(resultScore,Double.POSITIVE_INFINITY);
        for(int i=start;i<end;i++){
            double d=cand_flat[i];
            int tmp=resultLength-1;
            if(d<resultScore[tmp]){
                while(tmp>0&&d<resultScore[tmp-1]){
                    --tmp;
                    result[tmp+1]=result[tmp];
                    resultScore[tmp+1]=resultScore[tmp];
                }
                resultScore[tmp]=d;
                result[tmp]=i;
            }
        }
        return result;
    }
    private static int[] argsortDecreasing(float[] cand_flat,int start,int end,int length){
        int resultLength=Math.min(end-start,length);
        int[] result=new int[resultLength];
        float[] resultScore=new float[resultLength];
        Arrays.fill(resultScore,Float.NEGATIVE_INFINITY);
        for(int i=start;i<end;i++){
            float d=cand_flat[i];
            int tmp=resultLength-1;
            if(d>resultScore[tmp]){
                while(tmp>0&&d>resultScore[tmp-1]){
                    --tmp;
                    result[tmp+1]=result[tmp];
                    resultScore[tmp+1]=resultScore[tmp];
                }
                resultScore[tmp]=d;
                result[tmp]=i;
            }
        }
        return result;
    }
    public static String[] loadDictionry(InputStream in) throws IOException{
        BufferedReader reader=new BufferedReader(new InputStreamReader(in, "UTF-8"));
        List<String[]> pairs=new ArrayList<String[]>();
        String line;
        while((line=reader.readLine())!=null){
            String[] parts=line.split("\t");
            if(parts.length==2){
                pairs.add(parts);
            }
        }
        String[] dictionary=new String[pairs.size()];
        for(String[] pair:pairs){
            dictionary[Integer.parseInt(pair[1])]=pair[0];
        }
        reader.close();
        return dictionary;
    }

}
