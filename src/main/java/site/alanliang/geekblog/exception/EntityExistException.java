package site.alanliang.geekblog.exception;

/**
 * @Descriptin 名称不唯一异常
 * @Author AlanLiang
 * Date 2020/4/9 13:37
 * Version 1.0
 **/
public class EntityExistException extends RuntimeException {
    public EntityExistException(String message) {
        super(message);
    }
}
