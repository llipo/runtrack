package cz.tmartinik.runtrack.model;

/**
 * Created by tmartinik on 20.9.2017.
 */

public class Tempo {
    private Double speed;

    public Tempo(float speed) {
        this.speed = (double)speed;
    }

    public Double getMinKm(){
        return kmhToMinkm(speed);
    }

    private Double kmhToMinkm(Double speed) {
        Double tempo = 60d/speed;
        double whole = Math.floor(tempo);
        Double part = ((double)Math.round((tempo - whole)*60))/100d;
        return whole+part;
    }
}
