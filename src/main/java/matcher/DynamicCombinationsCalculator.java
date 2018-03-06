package matcher;

import java.util.Arrays;

public class DynamicCombinationsCalculator implements CombinationsCalculator {

    private int[][] cr;

    public DynamicCombinationsCalculator() {

    }

    public int calculateCombinations(int setSize, int combinationSize) {
        int comb = 0;

        if (setSize > 1) {
            this.cr = new int[setSize + 1][setSize + 1];
            for (int i = 0; i < setSize + 1; i++)
                Arrays.fill(cr[i], -1);

            comb = combinations(setSize, combinationSize);
        }

        return comb;
    }

    private int combinations(int n, int r) {
        int comb = 0;
        if (this.cr[n][r] != -1)
            comb = this.cr[n][r];
        else if (r == 0 || n == r)
            comb = 1;
        else {
            cr[n][r] = (combinations(n - 1, r) + combinations(n - 1, r - 1)) % (int) 1E9;
            comb = cr[n][r];
        }

        return comb;
    }

}
