package cc.chungkwong.mathocr.extractor;

import android.graphics.Bitmap;

public class Binarizer {
    private static final int wR = 316, wG = 624, wB = 84, divisor = 1024;
    private final double weight;
    private final int window;

    /**
     * Construct a ThreholdSauvola
     *
     * @param weight the weight
     * @param window the size of each window
     */
    public Binarizer(double weight, int window) {
        this.weight = weight;
        this.window = window;
    }

    private static int square(int k) {
        return k * k;
    }

    public Raster apply(Bitmap bitmap, boolean whiteOnBlack) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        byte[] pixels = new byte[width * height];
        int[] row=new int[width];
        if (whiteOnBlack) {
            for (int i = 0, ind = 0; i < height; i++) {
                bitmap.getPixels(row, 0, width, 0, i, width, 1);
                for (int j = 0; j < width; j++, ind++) {
                    int pixel = row[j];
                    int alpha = (pixel >>> 24) & 0xff, red = (pixel >>> 16) & 0xff, green = (pixel >>> 8) & 0xff, blue = pixel & 0xff;
                    pixels[ind] = (byte) ((255 - (red * wR + green * wG + blue * wB) / divisor) * alpha / 255);
                }
            }
        }else {
            for (int i = 0, ind = 0; i < height; i++) {
                bitmap.getPixels(row, 0, width, 0, i, width, 1);
                for (int j = 0; j < width; j++, ind++) {
                    int pixel = row[j];
                    int alpha = (pixel >>> 24) & 0xff, red = (pixel >>> 16) & 0xff, green = (pixel >>> 8) & 0xff, blue = pixel & 0xff;
                    pixels[ind] = (byte) (255 - (255 - (red * wR + green * wG + blue * wB) / divisor) * alpha / 255);
                }
            }
        }
        int[] integral=new int[width+1];
        int[] integralSquare=new int[width+1];
        int dl=(window+1)/2, dr=window/2;
        int[][] old=new int[dl][width+1];
        for(int i=0, ind=0, imax=Math.min(height,dr+1);i<imax;i++){
            for(int j=1;j<=width;j++,ind++){
                int pixel=(pixels[ind])&0xFF;
                integral[j]+=pixel;
                integralSquare[j]+=pixel*pixel;
            }
        }
        int dr1=Math.min(dr,width);
        int dr2=Math.max(width-dr+1,1);
        for(int i=0, ind=0, curr=0;i<height;i++){
            int winTop=Math.max(i-dl,-1), winBottom=Math.min(height-1,i+dr);
            int sum=0;
            int squareSum=0;
            for(int j=1;j<=dr1;j++){
                sum+=integral[j];
                squareSum+=integralSquare[j];
            }
            for(int j=1;j<=width-dr;j++,ind++){
                int winLeft=Math.max(j-dl,0), winRight=j+dr;
                sum+=integral[winRight]-integral[winLeft];
                squareSum+=integralSquare[winRight]-integralSquare[winLeft];
                int pixel=pixels[ind]&0xFF;
                old[curr][j]=pixel;
                if(pixel!=0xFF&&pixel!=0x00){
                    int area=((winBottom-winTop)*(winRight-winLeft));
                    double factor=1.0/area;
                    double mean=sum*factor;
                    double s=Math.sqrt(squareSum*factor-mean*mean);
                    int lim=(int)(mean*(1+weight*(s/128-1)));
                    pixels[ind]=pixel<=lim?0x00:(byte)0xff;
                }
            }
            for(int j=dr2;j<=width;j++,ind++){
                int winLeft=Math.max(j-dl,0), winRight=width;
                sum-=integral[winLeft];
                squareSum-=integralSquare[winLeft];
                int pixel=pixels[ind]&0xFF;
                old[curr][j]=pixel;
                if(pixel!=0xFF&&pixel!=0x00){
                    int area=((winBottom-winTop)*(winRight-winLeft));
                    double factor=1.0/area;
                    double mean=sum*factor;
                    double s=Math.sqrt(squareSum*factor-mean*mean);
                    int lim=(int)(mean*(1+weight*(s/128-1)));
                    pixels[ind]=pixel<=lim?0x00:(byte)0xff;
                }
            }
            if(++curr==dl){
                curr=0;
            }
            if(i>=dl-1){
                for(int j=1, index=(winTop+1)*width;j<=width;j++,index++){
                    int pixel=old[curr][j];
                    integral[j]-=pixel;
                    integralSquare[j]-=pixel*pixel;
                }
            }
            if(winBottom+1<height){
                for(int j=1, index=(winBottom+1)*width;j<=width;j++,index++){
                    int pixel=(pixels[index])&0xFF;
                    integral[j]+=pixel;
                    integralSquare[j]+=pixel*pixel;
                }
            }
        }
        return new Raster(pixels, width, height);
    }

    private int grayscale(int color) {
        int alpha = (color >>> 24) & 0xff, red = (color >>> 16) & 0xff, green = (color >>> 8) & 0xff, blue = color & 0xff;
        return 255 - (255 - (red * wR + green * wG + blue * wB) / divisor) * alpha / 255;
    }
}
