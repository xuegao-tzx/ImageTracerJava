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

/**
 * The type Selective blur.
 */
class SelectiveBlur {


    // Gaussian kernels for blur
    private static final double[][] gks = {{0.27901, 0.44198, 0.27901}, {0.135336, 0.228569, 0.272192, 0.228569, 0.135336}, {0.086776, 0.136394, 0.178908, 0.195843, 0.178908, 0.136394, 0.086776},
            {0.063327, 0.093095, 0.122589, 0.144599, 0.152781, 0.144599, 0.122589, 0.093095, 0.063327}, {0.049692, 0.069304, 0.089767, 0.107988, 0.120651, 0.125194, 0.120651, 0.107988, 0.089767, 0.069304, 0.049692}};


    /**
     * Blur image data.
     *
     * @param imgd the imgd
     * @param rad  the rad
     * @param del  the del
     * @return the image data
     */
// Selective Gaussian blur for preprocessing
    static ImageData blur(ImageData imgd, float rad, float del) {
        int i, j, k, d, idx;
        double racc, gacc, bacc, aacc, wacc;
        ImageData imgd2 = new ImageData(imgd.width, imgd.height, new byte[imgd.width * imgd.height * 4]);

        // radius and delta limits, this kernel
        int radius = (int) Math.floor(rad);
        if (radius < 1) return imgd;
        if (radius > 5) radius = 5;
        int delta = (int) Math.abs(del);
        if (delta > 1024) delta = 1024;
        double[] thisgk = SelectiveBlur.gks[radius - 1];

        // loop through all pixels, horizontal blur
        // End of horizontal blur
        for (j = 0; j < imgd.height; j++)
            for (i = 0; i < imgd.width; i++) {

                racc = 0;
                gacc = 0;
                bacc = 0;
                aacc = 0;
                wacc = 0;
                // gauss kernel loop
                // add weighted color values
                for (k = -radius; k < (radius + 1); k++)
                    if (((i + k) > 0) && ((i + k) < imgd.width)) {
                        idx = ((j * imgd.width) + i + k) * 4;
                        racc += imgd.data[idx] * thisgk[k + radius];
                        gacc += imgd.data[idx + 1] * thisgk[k + radius];
                        bacc += imgd.data[idx + 2] * thisgk[k + radius];
                        aacc += imgd.data[idx + 3] * thisgk[k + radius];
                        wacc += thisgk[k + radius];
                    }
                // The new pixel
                idx = ((j * imgd.width) + i) * 4;
                imgd2.data[idx] = (byte) Math.floor(racc / wacc);
                imgd2.data[idx + 1] = (byte) Math.floor(gacc / wacc);
                imgd2.data[idx + 2] = (byte) Math.floor(bacc / wacc);
                imgd2.data[idx + 3] = (byte) Math.floor(aacc / wacc);

            }// End of width loop

        // copying the half blurred imgd2
        byte[] himgd = imgd2.data.clone();

        // loop through all pixels, vertical blur
        // End of vertical blur
        for (j = 0; j < imgd.height; j++)
            for (i = 0; i < imgd.width; i++) {

                racc = 0;
                gacc = 0;
                bacc = 0;
                aacc = 0;
                wacc = 0;
                // gauss kernel loop
                // add weighted color values
                for (k = -radius; k < (radius + 1); k++)
                    if (((j + k) > 0) && ((j + k) < imgd.height)) {
                        idx = (((j + k) * imgd.width) + i) * 4;
                        racc += himgd[idx] * thisgk[k + radius];
                        gacc += himgd[idx + 1] * thisgk[k + radius];
                        bacc += himgd[idx + 2] * thisgk[k + radius];
                        aacc += himgd[idx + 3] * thisgk[k + radius];
                        wacc += thisgk[k + radius];
                    }
                // The new pixel
                idx = ((j * imgd.width) + i) * 4;
                imgd2.data[idx] = (byte) Math.floor(racc / wacc);
                imgd2.data[idx + 1] = (byte) Math.floor(gacc / wacc);
                imgd2.data[idx + 2] = (byte) Math.floor(bacc / wacc);
                imgd2.data[idx + 3] = (byte) Math.floor(aacc / wacc);

            }// End of width loop

        // Selective blur: loop through all pixels
        // End of Selective blur
        for (j = 0; j < imgd.height; j++)
            for (i = 0; i < imgd.width; i++) {

                idx = ((j * imgd.width) + i) * 4;
                // d is the difference between the blurred and the original pixel
                d = Math.abs(imgd2.data[idx] - imgd.data[idx]) + Math.abs(imgd2.data[idx + 1] - imgd.data[idx + 1]) +
                        Math.abs(imgd2.data[idx + 2] - imgd.data[idx + 2]) + Math.abs(imgd2.data[idx + 3] - imgd.data[idx + 3]);
                // selective blur: if d>delta, put the original pixel back
                if (d > delta) {
                    imgd2.data[idx] = imgd.data[idx];
                    imgd2.data[idx + 1] = imgd.data[idx + 1];
                    imgd2.data[idx + 2] = imgd.data[idx + 2];
                    imgd2.data[idx + 3] = imgd.data[idx + 3];
                }
            }

        return imgd2;

    }// End of blur()
}
