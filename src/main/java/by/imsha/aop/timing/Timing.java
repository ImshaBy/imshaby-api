package by.imsha.aop.timing;

import lombok.Data;

@Data
public class Timing {
    private Long appTime;
    private Long controllerTime;
    private Long serviceTime;
    private Integer serviceLevel = 0;
    private Long repositoryTime;
    private Integer repositoryLevel = 0;
}
