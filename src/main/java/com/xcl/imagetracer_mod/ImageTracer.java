/*
 * Copyright 2022 田梓萱, xcl@xuegao-tzx.top
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.xcl.imagetracer_mod;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;

////////////////////////////////////////////////////////////////////////////////////////
//                                                                                    //
//  Open source address of the project:https://github.com/xuegao-tzx/ImageTracerJava  //
//  本项目开源地址:https://gitee.com/xuegao-tzx/ImageTracerJava                          //
//  作者:田梓萱(XCL)                                                                    //
//                                                                                    //
////////////////////////////////////////////////////////////////////////////////////////
/**
 * The type Image tracer.
 */
public class ImageTracer {

    /**
     * The Versionnumber.
     */
    static String versionnumber = "1.1.4.516";
    private static int[] rawdata;

    /**
     * Instantiates a new Image tracer.
     */
    public ImageTracer() {
    }

    /**
     * Save string.
     *
     * @param filename the filename
     * @param str      the str
     * @throws Exception the exception
     */
// Saving a String as a file
    public static void saveString(String filename, String str) throws Exception {
        File file = new File(filename);
        // if file doesnt exists, then create it
        if (!file.exists()) file.createNewFile();
        FileWriter fw = new FileWriter(file.getAbsoluteFile());
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write(str);
        bw.close();
    }

    // Loading a file to ImageData, ARGB byte order
    private static ImageData loadImageData(String filename, HashMap<String, Float> options) throws Exception {

        BufferedImage image = ImageIO.read(new File(filename));
        return ImageTracer.loadImageData(image);
    }

    private static ImageData loadImageData(BufferedImage image) throws Exception {

        int width = image.getWidth();
        int height = image.getHeight();
        ImageTracer.rawdata = image.getRGB(0, 0, width, height, null, 0, width);
        byte[] data = new byte[ImageTracer.rawdata.length * 4];
        for (int i = 0; i < ImageTracer.rawdata.length; i++) {
            data[(i * 4) + 3] = ImageTracer.bytetrans((byte) (ImageTracer.rawdata[i] >>> 24));
            data[i * 4] = ImageTracer.bytetrans((byte) (ImageTracer.rawdata[i] >>> 16));
            data[(i * 4) + 1] = ImageTracer.bytetrans((byte) (ImageTracer.rawdata[i] >>> 8));
            data[(i * 4) + 2] = ImageTracer.bytetrans((byte) (ImageTracer.rawdata[i]));
        }
        return new ImageData(width, height, data);
    }

    // The bitshift method in loadImageData creates signed bytes where -1 -> 255 unsigned ; -128 -> 128 unsigned ;
    // 127 -> 127 unsigned ; 0 -> 0 unsigned ; These will be converted to -128 (representing 0 unsigned) ...
    // 127 (representing 255 unsigned) and tosvgcolorstr will add +128 to create RGB values 0..255
    private static byte bytetrans(byte b) {
        if (b < 0) return (byte) (b + 128);
        else return (byte) (b - 128);
    }

    /**
     * Image to svg string.
     *
     * @param filename the filename
     * @param options  the options
     * @param palette  the palette
     * @return the string
     * @throws Exception the exception
     */
    ////////////////////////////////////////////////////////////
    //
    //  User friendly functions
    //
    ////////////////////////////////////////////////////////////
    // Loading an image from a file, tracing when loaded, then returning the SVG String
    public static String imageToSVG(String filename, HashMap<String, Float> options, byte[][] palette) throws Exception {
        System.out.println("自定义配置:" + options.toString());
        ImageData imgd = ImageTracer.loadImageData(filename, options);
        return ImageTracer.imagedataToSVG(imgd, options, palette);
    }// End of imageToSVG()

    // Tracing ImageData, then returning the SVG String
    private static String imagedataToSVG(ImageData imgd, HashMap<String, Float> options, byte[][] palette) {
        IndexedImage ii = ImageTracer.imagedataToTracedata(imgd, options, palette);
        return SVGUtils.getsvgstring(ii, options);
    }// End of imagedataToSVG()

    // Tracing ImageData, then returning IndexedImage with tracedata in layers
    private static IndexedImage imagedataToTracedata(ImageData imgd, HashMap<String, Float> options, byte[][] palette) {
        // 1. Color quantization
        IndexedImage ii = VectorizingUtils.colorquantization(imgd, palette, options);
        // 2. Layer separation and edge detection
        int[][][] rawlayers = VectorizingUtils.layering(ii);
        // 3. Batch pathscan
        ArrayList<ArrayList<ArrayList<Integer[]>>> bps = VectorizingUtils.batchpathscan(rawlayers, (int) (Math.floor(options.get("pathomit"))));
        // 4. Batch interpollation
        ArrayList<ArrayList<ArrayList<Double[]>>> bis = VectorizingUtils.batchinternodes(bps);
        // 5. Batch tracing
        ii.layers = VectorizingUtils.batchtracelayers(bis, options.get("ltres"), options.get("qtres"));
        return ii;
    }// End of imagedataToTracedata()

    /**
     * The type Indexed image.
     */
// Container for the color-indexed image before and tracedata after vectorizing
    public static class IndexedImage {
        /**
         * The Width.
         */
        public int width, /**
         * The Height.
         */
        height;
        /**
         * The Array.
         */
        int[][] array; // array[x][y] of palette colors
        /**
         * The Palette.
         */
        byte[][] palette;// array[palettelength][4] RGBA color palette
        /**
         * The Layers.
         */
        ArrayList<ArrayList<ArrayList<Double[]>>> layers;// tracedata

        /**
         * Instantiates a new Indexed image.
         *
         * @param marray   the marray
         * @param mpalette the mpalette
         */
        IndexedImage(int[][] marray, byte[][] mpalette) {
            array = marray;
            palette = mpalette;
            width = marray[0].length - 2;
            height = marray.length - 2;// Color quantization adds +2 to the original width and height
        }
    }

    /**
     * The type Image data.
     */
// https://developer.mozilla.org/en-US/docs/Web/API/ImageData
    public static class ImageData {
        /**
         * The Width.
         */
        public int width, /**
         * The Height.
         */
        height;
        /**
         * The Data.
         */
        public byte[] data; // raw byte data: R G B A R G B A ...

        /**
         * Instantiates a new Image data.
         *
         * @param mwidth  the mwidth
         * @param mheight the mheight
         * @param mdata   the mdata
         */
        ImageData(int mwidth, int mheight, byte[] mdata) {
            width = mwidth;
            height = mheight;
            data = mdata;
        }
    }

}// End of ImageTracer class
