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

import com.xcl.imagetracer_mod.ImageTracer.ImageData;
import com.xcl.imagetracer_mod.ImageTracer.IndexedImage;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * The type Vectorizing utils.
 */
class VectorizingUtils {


    ////////////////////////////////////////////////////////////
    //
    //  Vectorizing functions
    //
    ////////////////////////////////////////////////////////////

    // Lookup tables for pathscan
    private static final byte[] pathscan_dir_lookup = {0, 0, 3, 0, 1, 0, 3, 0, 0, 3, 3, 1, 0, 3, 0, 0};
    private static final boolean[] pathscan_holepath_lookup = {false, false, false, false, false, false, false, true, false, false, false, true, false, true, true, false};
    // pathscan_combined_lookup[ arr[py][px] ][ dir ] = [nextarrpypx, nextdir, deltapx, deltapy];
    private static final byte[][][] pathscan_combined_lookup = {
            {{-1, -1, -1, -1}, {-1, -1, -1, -1}, {-1, -1, -1, -1}, {-1, -1, -1, -1}},// arr[py][px]==0 is invalid
            {{0, 1, 0, -1}, {-1, -1, -1, -1}, {-1, -1, -1, -1}, {0, 2, -1, 0}},
            {{-1, -1, -1, -1}, {-1, -1, -1, -1}, {0, 1, 0, -1}, {0, 0, 1, 0}},
            {{0, 0, 1, 0}, {-1, -1, -1, -1}, {0, 2, -1, 0}, {-1, -1, -1, -1}},

            {{-1, -1, -1, -1}, {0, 0, 1, 0}, {0, 3, 0, 1}, {-1, -1, -1, -1}},
            {{13, 3, 0, 1}, {13, 2, -1, 0}, {7, 1, 0, -1}, {7, 0, 1, 0}},
            {{-1, -1, -1, -1}, {0, 1, 0, -1}, {-1, -1, -1, -1}, {0, 3, 0, 1}},
            {{0, 3, 0, 1}, {0, 2, -1, 0}, {-1, -1, -1, -1}, {-1, -1, -1, -1}},

            {{0, 3, 0, 1}, {0, 2, -1, 0}, {-1, -1, -1, -1}, {-1, -1, -1, -1}},
            {{-1, -1, -1, -1}, {0, 1, 0, -1}, {-1, -1, -1, -1}, {0, 3, 0, 1}},
            {{11, 1, 0, -1}, {14, 0, 1, 0}, {14, 3, 0, 1}, {11, 2, -1, 0}},
            {{-1, -1, -1, -1}, {0, 0, 1, 0}, {0, 3, 0, 1}, {-1, -1, -1, -1}},

            {{0, 0, 1, 0}, {-1, -1, -1, -1}, {0, 2, -1, 0}, {-1, -1, -1, -1}},
            {{-1, -1, -1, -1}, {-1, -1, -1, -1}, {0, 1, 0, -1}, {0, 0, 1, 0}},
            {{0, 1, 0, -1}, {-1, -1, -1, -1}, {-1, -1, -1, -1}, {0, 2, -1, 0}},
            {{-1, -1, -1, -1}, {-1, -1, -1, -1}, {-1, -1, -1, -1}, {-1, -1, -1, -1}}// arr[py][px]==15 is invalid
    };

    /**
     * Colorquantization indexed image.
     *
     * @param imgd    the imgd
     * @param palette the palette
     * @param options the options
     * @return the indexed image
     */
// 1. Color quantization repeated "cycles" times, based on K-means clustering
    // https://en.wikipedia.org/wiki/Color_quantization    https://en.wikipedia.org/wiki/K-means_clustering
    static IndexedImage colorquantization(ImageData imgd, byte[][] palette, HashMap<String, Float> options) {

        // Selective Gaussian blur preprocessing
        if (options.get("blurradius") > 0)
            imgd = SelectiveBlur.blur(imgd, options.get("blurradius"), options.get("blurdelta"));

        int cycles = (int) Math.floor(options.get("colorquantcycles"));
        // Creating indexed color array arr which has a boundary filled with -1 in every direction
        int[][] arr = new int[imgd.height + 2][imgd.width + 2];
        for (int j = 0; j < (imgd.height + 2); j++) {
            arr[j][0] = -1;
            arr[j][imgd.width + 1] = -1;
        }
        for (int i = 0; i < (imgd.width + 2); i++) {
            arr[0][i] = -1;
            arr[imgd.height + 1][i] = -1;
        }

        int idx = 0, cd, cdl, ci, c1, c2, c3, c4;


        byte[][] original_palette_backup = palette;
        long[][] paletteacc = new long[palette.length][5];

        // Repeat clustering step "cycles" times
        for (int cnt = 0; cnt < cycles; cnt++) {

            // Average colors from the second iteration
            // averaging paletteacc for palette
            //float ratio;
            // averaging
            //ratio = (float)( (double)(paletteacc[k][4]) / (double)(imgd.width*imgd.height) );
            /*// Randomizing a color, if there are too few pixels and there will be a new cycle
					if( (ratio<minratio) && (cnt<(cycles-1)) ){
						palette[k][0] = (byte) (-128+Math.floor(Math.random()*255));
						palette[k][1] = (byte) (-128+Math.floor(Math.random()*255));
						palette[k][2] = (byte) (-128+Math.floor(Math.random()*255));
						palette[k][3] = (byte) (-128+Math.floor(Math.random()*255));
					}*/
            // End of palette loop
            // End of Average colors from the second iteration
            if (cnt > 0) for (int k = 0; k < palette.length; k++)
                if (paletteacc[k][3] > 0) {
                    palette[k][0] = (byte) (-128 + (paletteacc[k][0] / paletteacc[k][4]));
                    palette[k][1] = (byte) (-128 + (paletteacc[k][1] / paletteacc[k][4]));
                    palette[k][2] = (byte) (-128 + (paletteacc[k][2] / paletteacc[k][4]));
                    palette[k][3] = (byte) (-128 + (paletteacc[k][3] / paletteacc[k][4]));
                }

            // Reseting palette accumulator for averaging
            for (int i = 0; i < palette.length; i++) {
                paletteacc[i][0] = 0;
                paletteacc[i][1] = 0;
                paletteacc[i][2] = 0;
                paletteacc[i][3] = 0;
                paletteacc[i][4] = 0;
            }

            // loop through all pixels
            // End of j loop
            for (int j = 0; j < imgd.height; j++)
                for (int i = 0; i < imgd.width; i++) {

                    idx = ((j * imgd.width) + i) * 4;

                    // find closest color from original_palette_backup by measuring (rectilinear)
                    // color distance between this pixel and all palette colors
                    cdl = 256 + 256 + 256 + 256;
                    ci = 0;
                    for (int k = 0; k < original_palette_backup.length; k++) {

                        // In my experience, https://en.wikipedia.org/wiki/Rectilinear_distance works better than https://en.wikipedia.org/wiki/Euclidean_distance
                        c1 = Math.abs(original_palette_backup[k][0] - imgd.data[idx]);
                        c2 = Math.abs(original_palette_backup[k][1] - imgd.data[idx + 1]);
                        c3 = Math.abs(original_palette_backup[k][2] - imgd.data[idx + 2]);
                        c4 = Math.abs(original_palette_backup[k][3] - imgd.data[idx + 3]);
                        cd = c1 + c2 + c3 + (c4 * 4); // weighted alpha seems to help images with transparency

                        // Remember this color if this is the closest yet
                        if (cd < cdl) {
                            cdl = cd;
                            ci = k;
                        }

                    }// End of palette loop

                    // add to palettacc
                    paletteacc[ci][0] += 128 + imgd.data[idx];
                    paletteacc[ci][1] += 128 + imgd.data[idx + 1];
                    paletteacc[ci][2] += 128 + imgd.data[idx + 2];
                    paletteacc[ci][3] += 128 + imgd.data[idx + 3];
                    paletteacc[ci][4]++;

                    arr[j + 1][i + 1] = ci;
                }// End of i loop

        }// End of Repeat clustering step "cycles" times

        return new IndexedImage(arr, original_palette_backup);
    }// End of colorquantization

    /**
     * Layering int [ ] [ ] [ ].
     *
     * @param ii the ii
     * @return the int [ ] [ ] [ ]
     */
// 2. Layer separation and edge detection
    // Edge node types ( ▓:light or 1; ░:dark or 0 )
    // 12  ░░  ▓░  ░▓  ▓▓  ░░  ▓░  ░▓  ▓▓  ░░  ▓░  ░▓  ▓▓  ░░  ▓░  ░▓  ▓▓
    // 48  ░░  ░░  ░░  ░░  ░▓  ░▓  ░▓  ░▓  ▓░  ▓░  ▓░  ▓░  ▓▓  ▓▓  ▓▓  ▓▓
    //     0   1   2   3   4   5   6   7   8   9   10  11  12  13  14  15
    //
    static int[][][] layering(IndexedImage ii) {
        // Creating layers for each indexed color in arr
        int val = 0;
        int aw = ii.array[0].length;
        int ah = ii.array.length;
        int n1;
        int n2;
        int n3;
        int n4;
        int n5;
        int n6;
        int n7;
        int n8;

        int[][][] layers = new int[ii.palette.length][ah][aw];

        // Looping through all pixels and calculating edge node type
        // End of j loop
        for (int j = 1; j < (ah - 1); j++)
            for (int i = 1; i < (aw - 1); i++) {

                // This pixel's indexed color
                val = ii.array[j][i];

                // Are neighbor pixel colors the same?
                n1 = ii.array[j - 1][i - 1] == val ? 1 : 0;
                n2 = ii.array[j - 1][i] == val ? 1 : 0;
                n3 = ii.array[j - 1][i + 1] == val ? 1 : 0;
                n4 = ii.array[j][i - 1] == val ? 1 : 0;
                n5 = ii.array[j][i + 1] == val ? 1 : 0;
                n6 = ii.array[j + 1][i - 1] == val ? 1 : 0;
                n7 = ii.array[j + 1][i] == val ? 1 : 0;
                n8 = ii.array[j + 1][i + 1] == val ? 1 : 0;

                // this pixel"s type and looking back on previous pixels
                layers[val][j + 1][i + 1] = 1 + (n5 * 2) + (n8 * 4) + (n7 * 8);
                if (n4 == 0) layers[val][j + 1][i] = 0 + 2 + (n7 * 4) + (n6 * 8);
                if (n2 == 0) layers[val][j][i + 1] = 0 + (n3 * 2) + (n5 * 4) + 8;
                if (n1 == 0) layers[val][j][i] = 0 + (n2 * 2) + 4 + (n4 * 8);

            }// End of i loop

        return layers;
    }// End of layering()

    // 3. Walking through an edge node array, discarding edge node types 0 and 15 and creating paths from the rest.
    // Walk directions (dir): 0 > ; 1 ^ ; 2 < ; 3 v
    // Edge node types ( ▓:light or 1; ░:dark or 0 )
    // ░░  ▓░  ░▓  ▓▓  ░░  ▓░  ░▓  ▓▓  ░░  ▓░  ░▓  ▓▓  ░░  ▓░  ░▓  ▓▓
    // ░░  ░░  ░░  ░░  ░▓  ░▓  ░▓  ░▓  ▓░  ▓░  ▓░  ▓░  ▓▓  ▓▓  ▓▓  ▓▓
    // 0   1   2   3   4   5   6   7   8   9   10  11  12  13  14  15
    //
    private static ArrayList<ArrayList<Integer[]>> pathscan(int[][] arr, float pathomit) {
        ArrayList<ArrayList<Integer[]>> paths = new ArrayList<>();
        ArrayList<Integer[]> thispath;
        int px = 0;
        int py = 0;
        int w = arr[0].length;
        int h = arr.length;
        int dir = 0;
        boolean pathfinished = true, holepath = false;
        byte[] lookuprow;

        // End of i loop
        // End of j loop
        for (int j = 0; j < h; j++)
            for (int i = 0; i < w; i++)
                if ((arr[j][i] != 0) && (arr[j][i] != 15)) {

                    // Init
                    px = i;
                    py = j;
                    paths.add(new ArrayList<>());
                    thispath = paths.get(paths.size() - 1);
                    pathfinished = false;

                    // fill paths will be drawn, but hole paths are also required to remove unnecessary edge nodes
                    dir = VectorizingUtils.pathscan_dir_lookup[arr[py][px]];
                    holepath = VectorizingUtils.pathscan_holepath_lookup[arr[py][px]];

                    // Path points loop
                    while (!pathfinished) {

                        // New path point
                        thispath.add(new Integer[3]);
                        thispath.get(thispath.size() - 1)[0] = px - 1;
                        thispath.get(thispath.size() - 1)[1] = py - 1;
                        thispath.get(thispath.size() - 1)[2] = arr[py][px];

                        // Next: look up the replacement, direction and coordinate changes = clear this cell, turn if required, walk forward
                        lookuprow = VectorizingUtils.pathscan_combined_lookup[arr[py][px]][dir];
                        arr[py][px] = lookuprow[0];
                        dir = lookuprow[1];
                        px += lookuprow[2];
                        py += lookuprow[3];

                        // Close path
                        if (((px - 1) == thispath.get(0)[0]) && ((py - 1) == thispath.get(0)[1])) {
                            pathfinished = true;
                            // Discarding 'hole' type paths and paths shorter than pathomit
                            if ((holepath) || (thispath.size() < pathomit)) paths.remove(thispath);
                        }

                    }// End of Path points loop

                }// End of Follow path

        return paths;
    }// End of pathscan()


    /**
     * Batchpathscan array list.
     *
     * @param layers   the layers
     * @param pathomit the pathomit
     * @return the array list
     */
// 3. Batch pathscan
    static ArrayList<ArrayList<ArrayList<Integer[]>>> batchpathscan(int[][][] layers, float pathomit) {
        ArrayList<ArrayList<ArrayList<Integer[]>>> bpaths = new ArrayList<>();
        for (int[][] layer : layers) bpaths.add(VectorizingUtils.pathscan(layer, pathomit));
        return bpaths;
    }


    // 4. interpolating between path points for nodes with 8 directions ( East, SouthEast, S, SW, W, NW, N, NE )
    private static ArrayList<ArrayList<Double[]>> internodes(ArrayList<ArrayList<Integer[]>> paths) {
        ArrayList<ArrayList<Double[]>> ins = new ArrayList<>();
        ArrayList<Double[]> thisinp;
        Double[] thispoint;
        Double[] nextpoint = new Double[2];
        Integer[] pp1, pp2, pp3;
        int palen = 0, nextidx = 0, nextidx2 = 0;

        // paths loop
        for (int pacnt = 0; pacnt < paths.size(); pacnt++) {
            ins.add(new ArrayList<>());
            thisinp = ins.get(ins.size() - 1);
            palen = paths.get(pacnt).size();
            // pathpoints loop
            for (int pcnt = 0; pcnt < palen; pcnt++) {

                // interpolate between two path points
                nextidx = (pcnt + 1) % palen;
                nextidx2 = (pcnt + 2) % palen;
                thisinp.add(new Double[3]);
                thispoint = thisinp.get(thisinp.size() - 1);
                pp1 = paths.get(pacnt).get(pcnt);
                pp2 = paths.get(pacnt).get(nextidx);
                pp3 = paths.get(pacnt).get(nextidx2);
                thispoint[0] = (pp1[0] + pp2[0]) / 2.0;
                thispoint[1] = (pp1[1] + pp2[1]) / 2.0;
                nextpoint[0] = (pp2[0] + pp3[0]) / 2.0;
                nextpoint[1] = (pp2[1] + pp3[1]) / 2.0;

                // line segment direction to the next point
                // SouthEast
                if (thispoint[0] < nextpoint[0]) if (thispoint[1] < nextpoint[1]) thispoint[2] = 1.0;
                else // NE
                    // E
                    if (thispoint[1] > nextpoint[1]) thispoint[2] = 7.0;
                    else thispoint[2] = 0.0;
                else // SW
                    // S
                    if (thispoint[0] > nextpoint[0]) if (thispoint[1] < nextpoint[1]) thispoint[2] = 3.0;
                    else // NW
                        // W
                        if (thispoint[1] > nextpoint[1]) thispoint[2] = 5.0;
                        else thispoint[2] = 4.0;
                    else if (thispoint[1] < nextpoint[1]) thispoint[2] = 2.0;
                    else // N
                        // center, this should not happen
                        if (thispoint[1] > nextpoint[1]) thispoint[2] = 6.0;
                        else thispoint[2] = 8.0;

            }// End of pathpoints loop
        }// End of paths loop
        return ins;
    }// End of internodes()


    /**
     * Batchinternodes array list.
     *
     * @param bpaths the bpaths
     * @return the array list
     */
// 4. Batch interpollation
    static ArrayList<ArrayList<ArrayList<Double[]>>> batchinternodes(ArrayList<ArrayList<ArrayList<Integer[]>>> bpaths) {
        ArrayList<ArrayList<ArrayList<Double[]>>> binternodes = new ArrayList<>();
        for (int k = 0; k < bpaths.size(); k++) binternodes.add(VectorizingUtils.internodes(bpaths.get(k)));
        return binternodes;
    }


    // 5. tracepath() : recursively trying to fit straight and quadratic spline segments on the 8 direction internode path

    // 5.1. Find sequences of points with only 2 segment types
    // 5.2. Fit a straight line on the sequence
    // 5.3. If the straight line fails (an error>ltreshold), find the point with the biggest error
    // 5.4. Fit a quadratic spline through errorpoint (project this to get controlpoint), then measure errors on every point in the sequence
    // 5.5. If the spline fails (an error>qtreshold), find the point with the biggest error, set splitpoint = (fitting point + errorpoint)/2
    // 5.6. Split sequence and recursively apply 5.2. - 5.7. to startpoint-splitpoint and splitpoint-endpoint sequences
    // 5.7. TODO? If splitpoint-endpoint is a spline, try to add new points from the next sequence

    // This returns an SVG Path segment as a double[7] where
    // segment[0] ==1.0 linear  ==2.0 quadratic interpolation
    // segment[1] , segment[2] : x1 , y1
    // segment[3] , segment[4] : x2 , y2 ; middle point of Q curve, endpoint of L line
    // segment[5] , segment[6] : x3 , y3 for Q curve, should be 0.0 , 0.0 for L line
    //
    // path type is discarded, no check for path.size < 3 , which should not happen

    private static ArrayList<Double[]> tracepath(ArrayList<Double[]> path, float ltreshold, float qtreshold) {
        int pcnt = 0, seqend = 0;
        double segtype1, segtype2;
        ArrayList<Double[]> smp = new ArrayList<>();
        //Double [] thissegment;
        int pathlength = path.size();

        while (pcnt < pathlength) {
            // 5.1. Find sequences of points with only 2 segment types
            segtype1 = path.get(pcnt)[2];
            segtype2 = -1;
            seqend = pcnt + 1;
            while (
                    ((path.get(seqend)[2] == segtype1) || (path.get(seqend)[2] == segtype2) || (segtype2 == -1))
                            && (seqend < (pathlength - 1))) {
                if ((path.get(seqend)[2] != segtype1) && (segtype2 == -1)) segtype2 = path.get(seqend)[2];
                seqend++;
            }
            if (seqend == (pathlength - 1)) seqend = 0;

            // 5.2. - 5.6. Split sequence and recursively apply 5.2. - 5.6. to startpoint-splitpoint and splitpoint-endpoint sequences
            smp.addAll(VectorizingUtils.fitseq(path, ltreshold, qtreshold, pcnt, seqend));
            // 5.7. TODO? If splitpoint-endpoint is a spline, try to add new points from the next sequence

            // forward pcnt;
            if (seqend > 0) pcnt = seqend;
            else pcnt = pathlength;

        }// End of pcnt loop

        return smp;

    }// End of tracepath()


    // 5.2. - 5.6. recursively fitting a straight or quadratic line segment on this sequence of path nodes,
    // called from tracepath()
    private static ArrayList<Double[]> fitseq(ArrayList<Double[]> path, float ltreshold, float qtreshold, int seqstart, int seqend) {
        ArrayList<Double[]> segment = new ArrayList<>();
        Double[] thissegment;
        int pathlength = path.size();

        // return if invalid seqend
        if ((seqend > pathlength) || (seqend < 0)) return segment;

        int errorpoint = seqstart;
        boolean curvepass = true;
        double px, py, dist2, errorval = 0;
        double tl = (seqend - seqstart);
        if (tl < 0) tl += pathlength;
        double vx = (path.get(seqend)[0] - path.get(seqstart)[0]) / tl;
        double vy = (path.get(seqend)[1] - path.get(seqstart)[1]) / tl;

        // 5.2. Fit a straight line on the sequence
        int pcnt = (seqstart + 1) % pathlength;
        double pl;
        while (pcnt != seqend) {
            pl = pcnt - seqstart;
            if (pl < 0) pl += pathlength;
            px = path.get(seqstart)[0] + (vx * pl);
            py = path.get(seqstart)[1] + (vy * pl);
            dist2 = ((path.get(pcnt)[0] - px) * (path.get(pcnt)[0] - px)) + ((path.get(pcnt)[1] - py) * (path.get(pcnt)[1] - py));
            if (dist2 > ltreshold) curvepass = false;
            if (dist2 > errorval) {
                errorpoint = pcnt;
                errorval = dist2;
            }
            pcnt = (pcnt + 1) % pathlength;
        }

        // return straight line if fits
        if (curvepass) {
            segment.add(new Double[7]);
            thissegment = segment.get(segment.size() - 1);
            thissegment[0] = 1.0;
            thissegment[1] = path.get(seqstart)[0];
            thissegment[2] = path.get(seqstart)[1];
            thissegment[3] = path.get(seqend)[0];
            thissegment[4] = path.get(seqend)[1];
            thissegment[5] = 0.0;
            thissegment[6] = 0.0;
            return segment;
        }

        // 5.3. If the straight line fails (an error>ltreshold), find the point with the biggest error
        int fitpoint = errorpoint;
        curvepass = true;
        errorval = 0;

        // 5.4. Fit a quadratic spline through this point, measure errors on every point in the sequence
        // helpers and projecting to get control point
        double t = (fitpoint - seqstart) / tl, t1 = (1.0 - t) * (1.0 - t), t2 = 2.0 * (1.0 - t) * t, t3 = t * t;
        double cpx = (((t1 * path.get(seqstart)[0]) + (t3 * path.get(seqend)[0])) - path.get(fitpoint)[0]) / -t2;
        double cpy = (((t1 * path.get(seqstart)[1]) + (t3 * path.get(seqend)[1])) - path.get(fitpoint)[1]) / -t2;

        // Check every point
        pcnt = seqstart + 1;
        while (pcnt != seqend) {

            t = (pcnt - seqstart) / tl;
            t1 = (1.0 - t) * (1.0 - t);
            t2 = 2.0 * (1.0 - t) * t;
            t3 = t * t;
            px = (t1 * path.get(seqstart)[0]) + (t2 * cpx) + (t3 * path.get(seqend)[0]);
            py = (t1 * path.get(seqstart)[1]) + (t2 * cpy) + (t3 * path.get(seqend)[1]);

            dist2 = ((path.get(pcnt)[0] - px) * (path.get(pcnt)[0] - px)) + ((path.get(pcnt)[1] - py) * (path.get(pcnt)[1] - py));

            if (dist2 > qtreshold) curvepass = false;
            if (dist2 > errorval) {
                errorpoint = pcnt;
                errorval = dist2;
            }
            pcnt = (pcnt + 1) % pathlength;
        }

        // return spline if fits
        if (curvepass) {
            segment.add(new Double[7]);
            thissegment = segment.get(segment.size() - 1);
            thissegment[0] = 2.0;
            thissegment[1] = path.get(seqstart)[0];
            thissegment[2] = path.get(seqstart)[1];
            thissegment[3] = cpx;
            thissegment[4] = cpy;
            thissegment[5] = path.get(seqend)[0];
            thissegment[6] = path.get(seqend)[1];
            return segment;
        }

        // 5.5. If the spline fails (an error>qtreshold), find the point with the biggest error,
        // set splitpoint = (fitting point + errorpoint)/2
        int splitpoint = (fitpoint + errorpoint) / 2;

        // 5.6. Split sequence and recursively apply 5.2. - 5.6. to startpoint-splitpoint and splitpoint-endpoint sequences
        segment = VectorizingUtils.fitseq(path, ltreshold, qtreshold, seqstart, splitpoint);
        segment.addAll(VectorizingUtils.fitseq(path, ltreshold, qtreshold, splitpoint, seqend));
        return segment;

    }// End of fitseq()


    // 5. Batch tracing paths
    private static ArrayList<ArrayList<Double[]>> batchtracepaths(ArrayList<ArrayList<Double[]>> internodepaths, float ltres, float qtres) {
        ArrayList<ArrayList<Double[]>> btracedpaths = new ArrayList<>();
        for (int k = 0; k < internodepaths.size(); k++)
            btracedpaths.add(VectorizingUtils.tracepath(internodepaths.get(k), ltres, qtres));
        return btracedpaths;
    }


    /**
     * Batchtracelayers array list.
     *
     * @param binternodes the binternodes
     * @param ltres       the ltres
     * @param qtres       the qtres
     * @return the array list
     */
// 5. Batch tracing layers
    static ArrayList<ArrayList<ArrayList<Double[]>>> batchtracelayers(ArrayList<ArrayList<ArrayList<Double[]>>> binternodes, float ltres, float qtres) {
        ArrayList<ArrayList<ArrayList<Double[]>>> btbis = new ArrayList<>();
        for (int k = 0; k < binternodes.size(); k++)
            btbis.add(VectorizingUtils.batchtracepaths(binternodes.get(k), ltres, qtres));
        return btbis;
    }


}
