package cc.chungkwong.mathocr.extractor;

import java.util.Arrays;

public class Thinning {
    public static void thin(Raster bitmap){
        thin(bitmap.getData(),bitmap.getWidth()-2,bitmap.getHeight()-2);
    }
    private static void thin(byte[] foreground,int width,int height){
        byte[] backup=new byte[(height+2)*(width+2)];
        boolean[] ignore=new boolean[height+2];
        boolean[] ignore2=new boolean[height+2];
        Arrays.fill(ignore2,true);
        while(thin(foreground,backup,ignore,ignore2,width,height,true)
                |thin(foreground,backup,ignore,ignore2,width,height,false)){
            boolean[] tmp=ignore2;
            ignore2=ignore;
            ignore=tmp;
            Arrays.fill(ignore2,true);
        }
    }
    private static boolean thin(byte[] pixels,byte[] backup,boolean[] ignore,boolean[] ignore2,
                                int width,int height,boolean firstStep){
        System.arraycopy(pixels,0,backup,0,backup.length);
        boolean chanaged=false;
        boolean[] neighbor=new boolean[8];
        for(int i=1, ind=width+2;i<=height;i++){
            if(ignore[i-1]&&ignore[i+1]&&ignore[i]){
                ind+=(width+2);
                continue;
            }
            ++ind;
            for(int j=0;j<width;j++,ind++){
                if(pixels[ind]==0){
                    neighbor[0]=backup[ind-width-2]==0;
                    neighbor[1]=backup[ind-width-1]==0;
                    neighbor[2]=backup[ind+1]==0;
                    neighbor[3]=backup[ind+width+3]==0;
                    neighbor[4]=backup[ind+width+2]==0;
                    neighbor[5]=backup[ind+width+1]==0;
                    neighbor[6]=backup[ind-1]==0;
                    neighbor[7]=backup[ind-width-3]==0;
                    boolean toDelete;
                    if(firstStep){
                        toDelete=isUselessFirst(neighbor);
                    }else{
                        toDelete=isUselessSecond(neighbor);
                    }
                    if(toDelete){
                        chanaged=true;
                        ignore[i]=false;
                        ignore2[i]=false;
                        pixels[ind]=(byte)0xFF;
                    }
                }
            }
            ++ind;
        }
        return chanaged;
    }

    private static boolean isUselessFirst(boolean[] p) {
        return isNeighborCountUseless(p) && isComponentCountUseless(p) && (!p[0] || !p[2] || !p[4]) && (!p[2] || !p[4] || !p[6]);
    }

    private static boolean isUselessSecond(boolean[] p) {
        return isNeighborCountUseless(p) && isComponentCountUseless(p) && (!p[0] || !p[2] || !p[6]) && (!p[0] || !p[4] || !p[6]);
    }

    private static boolean isNeighborCountUseless(boolean[] p) {
        int count = 0;
        for (int i = 0; i < 8; i++) {
            if (p[i]) {
                ++count;
            }
        }
        return count >= 2 && count <= 6;
    }

    private static boolean isComponentCountUseless(boolean[] p) {
        boolean last = p[7];
        int count = 0;
        for (int i = 0; i < 8; i++) {
            if (p[i] && !last) {
                ++count;
            }
            last = p[i];
        }
        return count == 1 || (p[4] && p[6] && !p[0] && !p[1] && !p[2] && !p[5]) || (p[0] && p[6] && !p[2] && !p[3] && !p[4] && !p[7]);
    }
}
