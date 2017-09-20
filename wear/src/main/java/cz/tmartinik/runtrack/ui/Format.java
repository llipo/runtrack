package cz.tmartinik.runtrack.ui;

import java.text.DecimalFormat;

/**
 * Created by tmartinik on 20.9.2017.
 */

public class Format {

    private static DecimalFormat decimal = new DecimalFormat("0.00");

    public static String decimal(Double num){
        return decimal.format(num);
    }
}
