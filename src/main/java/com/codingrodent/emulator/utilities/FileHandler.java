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

public class FileHandler {

    private final SystemContext systemContext;

    /*
      this class will read in a standard rom image into any memory structure
     */
    public FileHandler() {
        systemContext = SystemContext.createInstance();
    }

    /**
     * Read a standard tape dump file into an array and return
     *
     * @param fileName The file to read
     * @return The block of memory read
     * @throws FileNotFoundException Thrown if the file specified does not exist
     * @throws IOException           Thrown if a failure occurs whiel reading the file
     */
    public MemoryChunk readHexDumpFile(String fileName) throws IOException {
        MemoryChunk memory = new MemoryChunk();
        String line;
        int address, base;

        systemContext.logInfoEvent("Reading .nas format file : " + fileName);
        LineNumberReader source = new LineNumberReader(new BufferedReader(new InputStreamReader(new FileInputStream(fileName), "UTF-8")));
        //
        boolean firstTime = true;
        while (true) { // read a line

            String inputLine = source.readLine();
            if ((null == inputLine) || (inputLine.charAt(0) == '.')) {
                break;
            }
            line = inputLine.trim();
            //System.out.println("<" + line + ">");

            // convert and place into memory
            address = Utilities.getHexValue(line.substring(0, 4));
            //System.out.println("Address : " + address + " : " + line.substring(0, 4));
            if (firstTime) {
                memory.setBase(address);
                firstTime = false;
            }
            base = 5;
            for (int i = 0; i < 8; i++) {
                memory.writeByte(Utilities.getHexValue(line.substring(base, base + 2)));
                base = base + 3;
            }
        }
        source.close();
        return memory;
    }

    /**
     * Write a standard tape dump file to a writer
     *
     * @param memory The chunk of memory to write
     * @param out    The writer to be written to
     * @throws IOException Thrown if a failure occurs while reading the file
     */
    public void writeHexDumpFile(MemoryChunk memory, Writer out) throws IOException {
        int checksum;
        int size = memory.getSize();
        int base = memory.getBase();
        int end = base + size;
        for (int address = base; address < end; address = address + 8) {
            checksum = (address >> 8) + (address & 0x00FF);
            out.write(Utilities.getWord(address) + " ");
            for (int column = 0; column < 8; column++) {
                int data = memory.readByte(address + column);
                checksum = checksum + data;
                out.write(Utilities.getByte(data) + " ");
            }
            out.write(Utilities.getByte(checksum & 0x00FF));
            out.write(0);
            out.write(0);
            out.write(13);
        }
    }

    /**
     * Read a binary dump of a piece of memory
     *
     * @param fileName The file to read
     * @param base     Base address to be read into
     * @return The block of memory read
     * @throws FileNotFoundException Thrown if the file specified does not exist
     * @throws IOException           Thrown if a failure occurs whiel reading the file
     */
    public MemoryChunk readBinaryDumpFile(String fileName, int base) throws IOException {
        MemoryChunk memory = new MemoryChunk();
        memory.setBase(base);
        //Utilities Utilities = new Utilities();
        systemContext.logInfoEvent("Reading .nas format file : " + fileName);
        DataInputStream source = new DataInputStream(new FileInputStream(fileName));
        byte[] code = new byte[1];
        while (-1 != source.read(code)) {
            memory.writeByte(code[0]);
        }
        source.close();
        return memory;
    }
}
