package com.abc.ppmimage;

import java.awt.image.*;
import java.io.*;
import java.util.*;

/**
 * Instances are immutable.
 */
public final class PpmImage {
    private static final int SLOW_FACTOR = 2_500;

    public static final String FORMAT = "P3";
    public static final int MAX_COLOR_VALUE = 255;

    private final Row[] privateRows;
    private final int columnCount;

    public PpmImage(Row[] rows) throws IllegalArgumentException {
        if (rows == null || rows.length == 0) throw new IllegalArgumentException("rows must have at least one Row");
        privateRows = rows.clone(); // grab a snapshot

        int colCount = privateRows[0].getColumnCount(); // we know we have at least one row
        for (int i = 1; i < privateRows.length; i++) {
            if (colCount != privateRows[i].getColumnCount()) {
                throw new IllegalArgumentException("every row must have exactly " + colCount +
                    " pixels; rows[" + i + "] has " + privateRows[i].getColumnCount() + " pixels");
            }
        }
        columnCount = colCount;
    }

    public int getRowCount() { return privateRows.length; }
    public int getColumnCount() { return columnCount; }

    public Row getRowAt(int rowIndex) throws IndexOutOfBoundsException {
        confirmValidRowIndex(rowIndex, privateRows.length);
        return privateRows[rowIndex];
    }

    public Pixel getPixelAt(int rowIndex, int colIndex) throws IndexOutOfBoundsException {
        confirmValidRowIndex(rowIndex, privateRows.length);
        confirmValidColIndex(colIndex, columnCount);
        return privateRows[rowIndex].getPixelAt(colIndex);
    }

    public String getMagicNumberString() { return FORMAT; }
    public int getMaxColorComponentValue() { return MAX_COLOR_VALUE; }

    public BufferedImage asBufferedImage() {
        BufferedImage bi = new BufferedImage(columnCount, privateRows.length, BufferedImage.TYPE_INT_RGB);
        for (int rowIndex = 0; rowIndex < privateRows.length; rowIndex++) {
            Row row = privateRows[rowIndex];
            for (int colIndex = 0; colIndex < columnCount; colIndex++) {
                bi.setRGB(colIndex, rowIndex, row.getPixelAt(colIndex).asIntBits());
            }
        }
        return bi;
    }

    public static PpmImage createFromFilename(String filename) throws FileNotFoundException, IOException {
        try (Scanner s = new Scanner(new File(filename))) {
            String format = s.nextLine();
            if (!FORMAT.equalsIgnoreCase(format)) throw new IOException("only \"" + FORMAT + "\" format is supported");
            int width = s.nextInt();
            int height = s.nextInt();
            int maxColorValue = s.nextInt();
            if (maxColorValue != MAX_COLOR_VALUE) throw new IOException("only [0.." + MAX_COLOR_VALUE +
                "] color values supported");

            Row[] rows = new Row[height];
            for (int rowIndex = 0; rowIndex < height; rowIndex++) {
                Pixel[] pixels = new Pixel[width];
                for (int colIndex = 0; colIndex < width; colIndex++) {
                    pixels[colIndex] = new Pixel(s.nextInt(), s.nextInt(), s.nextInt());
                }
                rows[rowIndex] = new Row(pixels);
            }
            return new PpmImage(rows);
        }
    }

    public void writeToFilename(String filename) throws FileNotFoundException, IOException {
        try (PrintWriter pw = new PrintWriter(new File(filename))) {
            pw.println(FORMAT);
            pw.printf("%d %d%n", columnCount, privateRows.length);
            pw.printf("%d%n", MAX_COLOR_VALUE);

            for (int rowIndex = 0; rowIndex < privateRows.length; rowIndex++) {
                for (int colIndex = 0; colIndex < columnCount; colIndex++) {
                    Pixel pixel = privateRows[rowIndex].getPixelAt(colIndex);
                    pw.printf("%d %d %d%n", pixel.red, pixel.green, pixel.blue);
                }
            }
        }
    }

    private static void confirmValidRowIndex(int rowIndex, int rowCount) throws IndexOutOfBoundsException {
        if (rowIndex < 0 || rowIndex >= rowCount) {
            throw new IndexOutOfBoundsException("rowIndex=" + rowIndex +
                " is not in the valid range of [0.." + (rowCount - 1) + "]");
        }
    }

    private static void confirmValidColIndex(int colIndex, int colCount) throws IndexOutOfBoundsException {
        if (colIndex < 0 || colIndex >= colCount) {
            throw new IndexOutOfBoundsException("colIndex=" + colIndex +
                " is not in the valid range of [0.." + (colCount - 1) + "]");
        }
    }

    /**
     * Instances are immutable.
     */
    public static final class Row {
        private final Pixel[] privatePixels;

        public Row(Pixel[] pixels) throws IllegalArgumentException {
            if (pixels == null || pixels.length == 0) throw new IllegalArgumentException("pixels must have at least " +
                "one Pixel");

            privatePixels = pixels.clone(); // grab a snapshot
        }

        public int getColumnCount() { return privatePixels.length; }

        public Pixel getPixelAt(int colIndex) throws IndexOutOfBoundsException {
            PpmImage.confirmValidColIndex(colIndex, privatePixels.length);
            return privatePixels[colIndex];
        }

        public Pixel[] getPixels() { return privatePixels.clone(); /* return a copy to maintain immutability */ }

        /** Returns a new Row flipped horizontally (as seen in a mirror). */
        public Row asRowFlippedHorizontally() {
            Row row = null;
            for (int slowIndex = 0; slowIndex < SLOW_FACTOR; slowIndex++) {
                Pixel[] flippedPixels = getPixels(); // gives us a copy that we can manipulate
                for(int i = 0; i < flippedPixels.length / 2; i++)
                {
                    Pixel tmp = flippedPixels[i];
                    flippedPixels[i] = flippedPixels[flippedPixels.length - i - 1];
                    flippedPixels[flippedPixels.length - i - 1] = tmp;
                }
                row = new Row(flippedPixels);
            }
            return row;
        }
    }  // type Row

    /**
     * Holds color information about a pixel as red, green, and blue components each in the range [0..255].
     * When the red, green, and blue values are all the same, this is a shade of gray (including black and white).
     * Samples:
     * <pre>
     *  red  green blue  color
     * ----- ----- ----- ----------
     *    0     0     0  black
     *  255   255   255  white
     *  255     0     0  red
     *    0   255     0  green
     *    0     0     0  blue
     *  255   255     0  yellow
     *  255     0   255  magenta
     *    0   255   255  cyan
     *  255   128     0  orange
     *  135   206   235  sky blue
     *  128   128   128  gray
     *  192   192   192  silver
     *  211   211   211  light gray
     * </pre>
     *
     * Instances are immutable.
     */
    public static final class Pixel {
        public final int red;
        public final int green;
        public final int blue;

        /** Specified values are each quietly coerced into 0..255 range. */
        public Pixel(int red, int green, int blue) {
            this.red   = Math.max(0, Math.min(red,   255));
            this.green = Math.max(0, Math.min(green, 255));
            this.blue  = Math.max(0, Math.min(blue,  255));
        }

        /** Returns a new Pixel which is a shade of gray */
        public Pixel asGrayscale() {
            Pixel pixel = null;
            for (int slowIndex = 0; slowIndex < SLOW_FACTOR; slowIndex++) {
                // https://en.wikipedia.org/wiki/Relative_luminance
                // The luminosity method, re-calculates the red, green, and blue values according to the following formula:
                //     grayval = 0.21 * red + 0.72 * green +  0.07 * blue
                double grayLevelAsDouble = 0.21 * red + 0.72 * green + 0.07 * blue;
                int grayLevelAsInt = (int) Math.round(grayLevelAsDouble);
                int grayLevelBoundedInt = Math.max(0, Math.min(grayLevelAsInt, 255));
                pixel = new Pixel(grayLevelBoundedInt, grayLevelBoundedInt, grayLevelBoundedInt);
            }
            return pixel;
        }

        /** return lower 24-bits, red, green, blue: <code>0000 0000 rrrr rrrr gggg gggg bbbb bbbb</code> */
        public int asIntBits() {
            return red << 16 | green << 8 | blue;
        }
    }  // type Pixel
}
