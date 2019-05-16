package cc.chungkwong.mathocr.extractor;

/**
 * Stroke width transformation
 *
 * @author Chan Chung Kwong
 */
public class StrokeWidthTransform {
    public static final byte HORIZONTAL = 1, VERTICAL = 2, THROW = 4, PRESS = 8;

    /**
     * Transform a bitmap image
     *
     * @param bitmap to be transform
     * @return stroke width space
     */
    public static StrokeSpace transform(Raster bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        byte[] pixels = bitmap.getData();
        short[] thicknessH = new short[width * height];
        short[] thicknessS = new short[width * height];
        int[] nC = new int[width];
        int[] nwC = new int[width + 1];
        int[] neC = new int[width + 1];
        for (int i = 0, ind = 0; i < height; i++) {
            int wC = 0;
            int nwCp = 0;
            for (int j = 0; j < width; j++, ind++) {
                if (pixels[ind] == 0) {
                    ++wC;
                    ++nC[j];
                    int tmp = nwC[j + 1];
                    nwC[j + 1] = nwCp + 1;
                    nwCp = tmp;
                    neC[j] = neC[j + 1] + 1;
                } else {
                    if (wC > 0) {
                        int t = wC;
                        for (int k = 1, ind0 = ind - 1; k <= t; k++, ind0--) {
                            if (thicknessH[ind0] == 0 || t < thicknessH[ind0]) {
                                thicknessH[ind0] = (short) t;
                            }
                        }
                        wC = 0;
                    }
                    if (nC[j] > 0) {
                        int t = nC[j];
                        for (int k = 1, ind0 = ind - width; k <= nC[j]; k++, ind0 -= width) {
                            if (thicknessH[ind0] == 0 || t < thicknessH[ind0]) {
                                thicknessH[ind0] = (short) t;
                            }
                        }
                        nC[j] = 0;
                    }
                    if (nwCp > 0) {
                        int t = nwCp;
                        int d = width + 1;
                        for (int k = 1, ind0 = ind - d; k <= t; k++, ind0 -= d) {
                            if (thicknessS[ind0] == 0 || t < thicknessS[ind0]) {
                                thicknessS[ind0] = (short) t;
                            }
                        }
                    }
                    nwCp = nwC[j + 1];
                    nwC[j + 1] = 0;
                    if (neC[j + 1] > 0) {
                        int t = neC[j + 1];
                        int d = width - 1;
                        for (int k = 1, ind0 = ind - d; k <= t; k++, ind0 -= d) {
                            if (thicknessS[ind0] == 0 || t < thicknessS[ind0]) {
                                thicknessS[ind0] = (short) t;
                            }
                        }
                    }
                    neC[j] = 0;
                }
            }
        }
        return new StrokeSpace(thicknessH, thicknessS, width, height);
    }

    /**
     * Space of stroke width
     */
    public static class StrokeSpace {
        private final short[] thicknessH;
        private final short[] thicknessS;
        private final int width, height;

        /**
         * Create a space of stroke width
         *
         * @param thicknessH thickness in horizontal or vertical direction
         * @param thicknessS thickness in throwing or pressing direction
         * @param width      width of the image
         * @param height     height of the image
         */
        public StrokeSpace(short[] thicknessH, short[] thicknessS, int width, int height) {
            this.thicknessH = thicknessH;
            this.thicknessS = thicknessS;
            this.width = width;
            this.height = height;
        }

        /**
         * @return thickness in horizontal or vertical direction
         */
        public short[] getThicknessH() {
            return thicknessH;
        }

        /**
         * @return thickness in throwing or pressing direction
         */
        public short[] getThicknessS() {
            return thicknessS;
        }

        /**
         * @return width of the image
         */
        public int getWidth() {
            return width;
        }

        /**
         * @return height of the image
         */
        public int getHeight() {
            return height;
        }
    }
}
