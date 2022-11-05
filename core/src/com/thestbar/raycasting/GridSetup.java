package com.thestbar.raycasting;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GridSetup {
    private BufferedReader bufferedReader;
    private int[] grid;

    public GridSetup(String mapFileName, int gridHeight, int gridWidth) throws IOException {
        List<String> listOfStrings = new ArrayList<>();
        bufferedReader = new BufferedReader(new FileReader(mapFileName));
        String line = bufferedReader.readLine();

        while(line != null) {
            listOfStrings.add(line);
            line = bufferedReader.readLine();
        }

        bufferedReader.close();

        grid = new int[gridHeight * gridWidth];
        int index = 0;

        for(String s: listOfStrings) {
            char[] a = s.toCharArray();
            for(char c: a) {
                if(c != ',' && c != ' ')
                    grid[index++] = c - '0';
            }
        }
    }

    public int[] getGrid() {
        return this.grid;
    }
}
