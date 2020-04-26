package site.alanliang.geekblog.controller.web;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import site.alanliang.geekblog.anntation.AccessLog;
import site.alanliang.geekblog.common.Constant;
import site.alanliang.geekblog.domain.Article;
import site.alanliang.geekblog.dto.ArchivesVo;
import site.alanliang.geekblog.query.ArticleQuery;
import site.alanliang.geekblog.service.ArticleService;
import site.alanliang.geekblog.utils.DateUtil;
import site.alanliang.geekblog.vo.ArticleDateVo;

import java.util.List;

/**
 * @Descriptin TODO
 * @Author AlanLiang
 * Date 2020/4/22 9:12
 * Version 1.0
 **/
@RestController
public class ArchivesController {

    @Autowired
    private ArticleService articleService;

    @AccessLog("访问归档页")
    @GetMapping("/archives")
    public ResponseEntity<Object> archives(@RequestParam(value = "dataType", required = false) Integer dateFilterType) {

        List<ArticleDateVo> articleDates = articleService.countArticleByDate(dateFilterType);
        for (ArticleDateVo articleDate : articleDates) {
            articleDate.setDate(DateUtil.formatDate(articleDate.getYear(), articleDate.getMonth(), articleDate.getDay()));
            articleDate.setYear(null);
            articleDate.setMonth(null);
            articleDate.setDay(null);
        }

        Page<Article> pageInfo = articleService.listPageArticlePreviewByDate(1, Integer.parseInt(Constant.PAGE_SIZE), null);

        ArchivesVo archivesVo = new ArchivesVo();
        archivesVo.setArticleDates(articleDates);
        archivesVo.setPageInfo(pageInfo);
        return new ResponseEntity<>(archivesVo, HttpStatus.OK);
    }

    @GetMapping("/archives-articles")
    public ResponseEntity<Object> archivesArticles(@RequestParam(value = "current", defaultValue = "1") Integer current,
                                                   @RequestParam(value = "size", defaultValue = Constant.PAGE_SIZE) Integer size,
                                                   ArticleQuery articleQuery) {

        Page<Article> pageInfo = articleService.listPageArticlePreviewByDate(current, size, articleQuery);
        return new ResponseEntity<>(pageInfo, HttpStatus.OK);
    }

}
