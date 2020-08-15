package cc.chungkwong.mathocr.recognizer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class Ll1Grammar {
    private final Map<Condition,List<String>> table;
    public Ll1Grammar(Map<String,List<List<String>>> rules, Map<String, Set<String>> start, Map<String,Set<String>> follow){
        this.table=new HashMap<Condition,List<String>>();
        for(Map.Entry<String,List<List<String>>> entry:rules.entrySet()){
            String target=entry.getKey();
            List<List<String>> childrens=entry.getValue();
            for(List<String> forward:childrens){
                List<String> rev=new ArrayList<String>(forward);
                Collections.reverse(rev);
                boolean nullable=true;
                for(String child:forward){
                    if(start.containsKey(child)){
                        for(String s:start.get(child)){
                            if(!s.isEmpty()){
                                addEntry(target,s,rev);
                            }
                        }
                        if(!start.get(child).contains("")){
                            nullable=false;
                            break;
                        }
                    }else{
                        addEntry(target,child,rev);
                        nullable=false;
                        break;
                    }
                }
                if(nullable){
                    for(String f:follow.get(target)){
                        addEntry(target,f,rev);
                    }
                }
            }
        }
//		System.out.println(table);
    }
    private void addEntry(String target,String token,List<String> rule){
        Condition condition=new Condition(target,token);
        if(table.containsKey(condition)){
            System.out.println("A conflict found: "+target+"\t"+token+"\t"+rule+"\t"+table.get(target));
        }else{
            table.put(condition,rule);
        }
    }
    public static final List<String> FAILED= Arrays.asList("<eol>","failed");
    public static final List<String> START=Arrays.asList("<eol>","start");
    private static Map<String,List<List<String>>> loadRules(InputStream in) throws IOException {
        Map<String,List<List<String>>> rules=new HashMap<String,List<List<String>>>();
        BufferedReader reader=new BufferedReader(new InputStreamReader(in, "UTF-8"));
        String line;
        String target=null;
        while((line=reader.readLine())!=null){
            String[] pair=line.split("\t");
            if(pair.length>=1){
                if(!pair[0].isEmpty()){
                    target=pair[0];
                    rules.put(target,new ArrayList<List<String>>());
                }
                List<String> children=new ArrayList<String>(pair.length-1);
                for(int i=1;i<pair.length;i++){
                    children.add(pair[i]);
                }
                rules.get(target).add(children);
            }
        }
        reader.close();
        return rules;
    }
    private static Map<String,Set<String>> findStart(Map<String,List<List<String>>> rules){
        Map<String,Set<String>> start=new HashMap<String,Set<String>>();
        for(String target:rules.keySet()){
            start.put(target,new HashSet<String>());
        }
        boolean changed=true;
        while(changed){
            changed=false;
            for(Map.Entry<String,List<List<String>>> entry:rules.entrySet()){
                String target=entry.getKey();
                List<List<String>> childrens=entry.getValue();
                Set<String> starts=start.get(target);
                int collected=starts.size();
                for(List<String> children:childrens){
                    boolean nullable=true;
                    for(String child:children){
                        nullable=false;
                        if(start.containsKey(child)){
                            for(String string:start.get(child)){
                                if(string.isEmpty()){
                                    nullable=true;
                                }else{
                                    starts.add(string);
                                }
                            }
                        }else{
                            starts.add(child);
                        }
                        if(!nullable){
                            break;
                        }
                    }
                    if(nullable){
                        starts.add("");
                    }
                }
                if(starts.size()>collected){
                    changed=true;
                }
            }
        }
//		System.out.println(start);
        return start;
    }
    private static Map<String,Set<String>> findFollow(Map<String,List<List<String>>> rules,Map<String,Set<String>> start){
        Map<String,Set<String>> follow=new HashMap<String,Set<String>>();
        for(String target:rules.keySet()){
            follow.put(target,new HashSet<String>());
        }
        follow.get("start").add("<eol>");
        boolean changed=true;
        while(changed){
            changed=false;
            for(Map.Entry<String,List<List<String>>> entry:rules.entrySet()){
                String target=entry.getKey();
                List<List<String>> childrens=entry.getValue();
                for(List<String> children:childrens){
                    Set<String> s=follow.get(target);
                    for(int i=children.size()-1;i>=0;i--){
                        String child=children.get(i);
                        if(follow.containsKey(child)){
                            Set<String> f=follow.get(child);
                            for(String t:s){
                                if(!follow.containsKey(t)&&!f.contains(t)){
                                    f.add(t);
                                    changed=true;
                                }
                            }
                            if(start.get(child).contains("")){
                                s=new HashSet<String>(s);
                                s.addAll(start.get(child));
                            }else{
                                s=new HashSet<String>(start.get(child));
                            }
                        }else{
                            s=new HashSet<String>();
                            s.add(child);
                        }
                    }
                }
            }
        }
//		System.out.println(follow);
        return follow;
    }
    public static Ll1Grammar load(InputStream in) throws IOException{
        Map<String,List<List<String>>> rules=loadRules(in);
        Map<String,Set<String>> start=findStart(rules);
        Map<String,Set<String>> follow=findFollow(rules,start);
        return new Ll1Grammar(rules,start,follow);
    }
    public List<String> next(List<String> state,String token){
        while(true){
            int index=state.size()-1;
            if(index>=0){
                String target=state.get(index);
                if(token.equals(target)){
                    return state.subList(0,index);
                }else{
                    Condition condition=new Condition(target,token);
                    if(table.containsKey(condition)){
                        state=new ArrayList<String>(state.subList(0,index));
                        state.addAll(table.get(condition));
                    }else{
                        return FAILED;
                    }
                }
            }else{
                return FAILED;
            }
        }
    }
    public static boolean isFailed(List<String> state){
        return state==FAILED;
    }
    public static boolean isFinished(List<String> state){
        return state.isEmpty();
    }
    public boolean isLegal(String... tokens){
        List<String> state=START;
//		System.out.println(state);
        for(String token:tokens){
            state=next(state,token);
//			System.out.println(state);
        }
        state=next(state,"<eol>");
//		System.out.println(state);
        return isFinished(state);
    }
    private static class Condition{
        private final String target;
        private final String token;
        public Condition(String target,String token){
            this.target=target;
            this.token=token;
        }
        public String getTarget(){
            return target;
        }
        public String getToken(){
            return token;
        }
        @Override
        public boolean equals(Object obj){
            return obj instanceof Condition&&target.equals(((Condition)obj).target)&&token.equals(((Condition)obj).token);
        }
        @Override
        public int hashCode(){
            int hash=3;
            hash=97*hash+this.target.hashCode();
            hash=97*hash+this.token.hashCode();
            return hash;
        }
        @Override
        public String toString(){
            return "("+target+","+token+")";
        }
    }
}
