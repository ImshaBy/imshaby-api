package by.imsha.aop.timing;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class TimingAspect {

    @Around(value = "@within(org.springframework.web.bind.annotation.RestController)")
    public Object controllerTime(ProceedingJoinPoint joinPoint) throws Throwable {
        TimingService.startTime("controller");
        Object result;
        try {
            result = joinPoint.proceed();
        } finally {
            TimingService.stopTime("controller");
        }
        return result;
    }

    @Around(value = "@within(org.springframework.stereotype.Service)")
    public Object serviceTime(ProceedingJoinPoint joinPoint) throws Throwable {
        TimingService.startTime("service");
        Object result;
        try {
            result = joinPoint.proceed();
        } finally {
            TimingService.stopTime("service");
        }
        return result;
    }

    @Around(value = "execution(* org.springframework.data.mongodb.repository.MongoRepository+.*(..))")
    public Object repositoryTime(ProceedingJoinPoint joinPoint) throws Throwable {
        TimingService.startTime("repository");
        Object result;
        try {
            result = joinPoint.proceed();
        } finally {
            TimingService.stopTime("repository");
        }
        return result;
    }
}
