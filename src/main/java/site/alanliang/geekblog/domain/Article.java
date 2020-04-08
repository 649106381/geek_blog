package site.alanliang.geekblog.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Date;

/**
 * @Descriptin 文章
 * @Author AlanLiang
 * Date 2020/4/5 20:53
 * Version 1.0
 **/
@Data
@TableName("t_article")
public class Article {
    @TableId(type = IdType.AUTO)
    private Long id;
    @NotBlank(message = "文章标题不能为空")
    @Length(max = 100, message = "文章标题长度不能超过100")
    private String title;

    private String summary;
    @NotBlank(message = "文章内容不能为空")
    private String content;

    private String textContent;

    private String cover;

    private Integer views;

    private Integer likes;

    private Integer comments;

    private Boolean appreciable;

    private Boolean commentable;

    private Boolean published;

    private Long authorId;
    @NotNull(message = "请选择一个分类")
    private Long categoryId;

    private Date createTime;

    private Date updateTime;

    private Integer type;
}
