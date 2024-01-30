package by.imsha.aop.timing;

import lombok.Data;

@Data
public class Timing {
    private Long currentTime;
    private Long time;
    private Integer level;

    private static final int START_LEVEL = 0;
    private static final int LEVEL_STEP = 1;

    public Timing() {
        time = 0L;
        level = START_LEVEL;
    }

    public void increaseLevel (){
        if (level == START_LEVEL){
            currentTime = System.currentTimeMillis();
        }
        level += LEVEL_STEP;
    }

    public void decreaseLevel (){
        level -= LEVEL_STEP;
        if (level == START_LEVEL) {
            time += System.currentTimeMillis() - currentTime;
        }
    }
}
