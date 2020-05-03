package site.alanliang.geekblog.query;

import lombok.Data;

import java.util.Date;

/**
 * @Descriptin TODO
 * @Author AlanLiang
 * Date 2020/5/3 17:26
 * Version 1.0
 **/
@Data
public class CategoryQuery {
    private String name;

    private Boolean display;

    private Date startDate;

    private Date endDate;
}
