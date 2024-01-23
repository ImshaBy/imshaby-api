package by.imsha.aop.timing;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Timing {
    private Long time;
    private Integer level;

    public void increaseLevel (){
        level = level + 1;
    }

    public void decreaseLevel (){
        level = level - 1;
        if (level == 0) {
            time = System.currentTimeMillis() - time;
        }
    }
}
