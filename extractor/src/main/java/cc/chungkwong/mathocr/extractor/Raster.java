package cc.chungkwong.mathocr.extractor;

import java.util.Arrays;

/**
 * Binary bitmap image
 *
 * @author Chan Chung Kwong
 */
public class Raster {
    private final byte[] data;
    private final int width;
    private final int height;

    /**
     * Create a image
     *
     * @param pixels   pixels of the image where black is marked 0
     * @param width  width
     * @param height height
     */
    public Raster(byte[] pixels, int width, int height) {
        this.data = new byte[((width + 2) * (height + 2))];
        Arrays.fill(data,0,width+2, (byte) 0xFF);
        for (int i = 0, k = 0, ind = width + 3; i < height; i++,k+=width,ind+=width+2) {
            data[ind-1]= (byte) 0xFF;
            System.arraycopy(pixels,k,data,ind,width);
            data[ind+width]= (byte) 0xFF;
        }
        Arrays.fill(data,data.length-width-2,data.length, (byte) 0xFF);
        this.width = width + 2;
        this.height = height + 2;
    }

    /**
     * @return pixels
     */
    public byte[] getData() {
        return data;
    }

    /**
     * @return height
     */
    public int getHeight() {
        return height;
    }

    /**
     * @return width
     */
    public int getWidth() {
        return width;
    }
}