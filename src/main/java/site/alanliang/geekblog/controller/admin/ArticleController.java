package site.alanliang.geekblog.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import site.alanliang.geekblog.common.JsonResult;
import site.alanliang.geekblog.common.TableResult;
import site.alanliang.geekblog.entity.Article;
import site.alanliang.geekblog.vo.ArticleVO;
import site.alanliang.geekblog.service.ArticleService;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;

/**
 * @Descriptin TODO
 * @Author AlanLiang
 * Date 2020/4/7 23:02
 * Version 1.0
 **/
@RestController
@RequestMapping("/admin/article")
public class ArticleController {

    @Autowired
    private ArticleService articleService;

    @GetMapping
    public TableResult listTableByPage(@RequestParam(value = "page", defaultValue = "1") Integer page,
                                       @RequestParam(value = "limit", defaultValue = "10") Integer limit,
                                       @RequestParam(value = "keyword", required = false) String keyword) {
        QueryWrapper<Article> wrapper = null;
        if (!StringUtils.isEmpty(keyword)) {
            wrapper = new QueryWrapper<>();
            wrapper.like("title", keyword);
        }
        Page<Article> pageInfo = articleService.listTableByPage(page, limit, wrapper);
        return TableResult.tableOk(pageInfo.getRecords(), pageInfo.getTotal());
    }

    @PostMapping
    public JsonResult save(@Validated @RequestBody ArticleVO articleVo) {
        articleVo.setViews(0);
        articleVo.setLikes(0);
        articleVo.setComments(0);
        articleVo.setAppreciable(articleVo.getAppreciable() != null);
        articleVo.setCommentable(articleVo.getCommentable() != null);
        articleVo.setTop(articleVo.getTop() != null);
        articleVo.setRecommend(articleVo.getRecommend() != null);
        articleVo.setCreateTime(new Date());
        articleVo.setUpdateTime(articleVo.getCreateTime());
        articleService.saveOrUpdate(articleVo);
        return JsonResult.ok();
    }

    @PutMapping
    public JsonResult update(@Validated @RequestBody ArticleVO articleVo) {
        articleVo.setAppreciable(articleVo.getAppreciable() != null);
        articleVo.setCommentable(articleVo.getCommentable() != null);
        articleVo.setTop(articleVo.getTop() != null);
        articleVo.setRecommend(articleVo.getRecommend() != null);
        articleVo.setUpdateTime(new Date());
        articleService.saveOrUpdate(articleVo);
        return JsonResult.ok();
    }

    @DeleteMapping("/{id}")
    public JsonResult remove(@NotNull @PathVariable("id") Long id) {
        articleService.removeById(id);
        return JsonResult.ok();
    }

    @DeleteMapping
    public JsonResult batchRemove(@NotEmpty @RequestBody List<Long> idList) {
        articleService.removeByIds(idList);
        return JsonResult.ok();
    }
}
