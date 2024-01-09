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
        TimingService.startControllerTime();
        Object result;
        try {
            result = joinPoint.proceed();
        } finally {
            TimingService.stopControllerTime();
        }
        return result;
    }

    @Around(value = "@within(org.springframework.stereotype.Service)")
    public Object serviceTime(ProceedingJoinPoint joinPoint) throws Throwable {
        TimingService.startServiceTime();
        Object result;
        try {
            result = joinPoint.proceed();
        } finally {
            TimingService.stopServiceTime();
        }
        return result;
    }

    @Around(value = "execution(* org.springframework.data.mongodb.repository.MongoRepository+.*(..))")
    public Object repositoryTime(ProceedingJoinPoint joinPoint) throws Throwable {
        TimingService.startRepositoryTime();
        Object result;
        try {
            result = joinPoint.proceed();
        } finally {
            TimingService.stopRepositoryTime();
        }
        return result;
    }
}
