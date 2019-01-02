/*
 * MIT License
 *
 * Copyright (c) 2016
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package com.codingrodent.emulator.utilities;

import com.codingrodent.emulator.emulator.SystemContext;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class NasToBinFileConverter {
    /**
     * Debug code to test processing
     *
     * @param args No parameters
     */
    public static void main(String[] args) {
        System.out.println("-- start --");
        try {
            SystemContext context = SystemContext.createInstance();
            context.logInfoEvent("NAS to BIN file converter - Start");
            NasToBinFileConverter converter = new NasToBinFileConverter();
            converter.convert("temp");
            context.logInfoEvent("NAS to BIN file converter - Completed");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        System.out.println("-- stop --");
    }

    /**
     * Convert any .nas files into .bin format
     *
     * @param path Path where files are located to convert
     */
    private void convert(String path) {
        File dir = new File(path);
        String[] allFiles = dir.list(new NASFilter());
        if (null == allFiles)
            throw new RuntimeException("No files available!");
        for (String allFile : allFiles) {
            System.out.println("----- Converting: " + allFile + " -----");
            File in = new File(path + File.separator + allFile);
            File out = new File(path + File.separator + allFile + ".bin");
            try {
                LineNumberReader lr = new LineNumberReader(new BufferedReader(new InputStreamReader(new FileInputStream(in), StandardCharsets.UTF_8)));
                FileOutputStream fos = new FileOutputStream(out);
                while (lr.ready()) {
                    String line = lr.readLine();
                    if (null != line) {
                        String[] parts = line.split(" ");
                        if (parts.length >= 8) {
                            for (int j = 1; j <= 8; j++) {
                                byte[] val = {(byte) Utilities.getHexValue(parts[j])};
                                fos.write(val);
                                System.out.print(parts[j] + "-");
                            }
                            System.out.println();
                        }
                    }
                }
                lr.close();
                fos.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Inner class used to select .NAS files
     */
    private static class NASFilter implements FilenameFilter {

        @Override
        public boolean accept(File dir, String name) {
            return name.endsWith(".nas");
        }

    }

}
