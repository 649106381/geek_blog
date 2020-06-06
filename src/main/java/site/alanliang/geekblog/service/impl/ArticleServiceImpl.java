package site.alanliang.geekblog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import site.alanliang.geekblog.common.Constant;
import site.alanliang.geekblog.dao.ArticleMapper;
import site.alanliang.geekblog.dao.ArticleTagMapper;
import site.alanliang.geekblog.dao.OperationLogMapper;
import site.alanliang.geekblog.dao.TagMapper;
import site.alanliang.geekblog.dto.ArticleDocument;
import site.alanliang.geekblog.model.Article;
import site.alanliang.geekblog.model.ArticleTag;
import site.alanliang.geekblog.model.Tag;
import site.alanliang.geekblog.query.ArchivesQuery;
import site.alanliang.geekblog.query.ArticleQuery;
import site.alanliang.geekblog.repository.ArticleDocumentRepository;
import site.alanliang.geekblog.service.ArticleService;
import site.alanliang.geekblog.utils.HighLightUtil;
import site.alanliang.geekblog.utils.UserInfoUtil;
import site.alanliang.geekblog.vo.ArticleDateVO;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Descriptin TODO
 * @Author AlanLiang
 * Date 2020/4/8 9:00
 * Version 1.0
 **/
@Service
@CacheConfig(cacheNames = "article")
public class ArticleServiceImpl implements ArticleService {

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private ArticleTagMapper articleTagMapper;

    @Autowired
    private TagMapper tagMapper;

    @Autowired
    private OperationLogMapper operationLogMapper;

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    @Autowired
    private ArticleDocumentRepository articleDocumentRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void increaseLikes(Long id) {
        QueryWrapper<Article> wrapper = new QueryWrapper<>();
        wrapper.select("likes").eq("id", id);
        Article article = articleMapper.selectOne(wrapper);
        article.setId(id);
        article.setLikes(article.getLikes() + 1);
        articleMapper.updateById(article);
    }

    @Override
    @Cacheable(key = "'listPreviewPageByDate:'+#current")
    public Page<Article> listPreviewPageByDate(Integer current, Integer size, ArchivesQuery archivesQuery) {
        Page<Article> articlePage = new Page<>(current, size);
        return articleMapper.listPreviewPageByDate(articlePage);
    }

    @Override
    public List<ArticleDocument> listByKeyword(String keyword) throws IOException {
        SearchRequest searchRequest = new SearchRequest("article_document");
        //匹配查询
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        MultiMatchQueryBuilder multiMatchQueryBuilder = QueryBuilders.multiMatchQuery(keyword, "title", "summary", "content");
        TermQueryBuilder termQueryBuilder = QueryBuilders.termQuery("published", true);
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery().must(multiMatchQueryBuilder).must(termQueryBuilder);
        sourceBuilder.query(boolQueryBuilder);
        //高亮
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field("title").field("summary").field("content");
        highlightBuilder.preTags("<em class='search-keyword'>");
        highlightBuilder.postTags("</em>");
        sourceBuilder.highlighter(highlightBuilder);
        //执行搜索
        searchRequest.source(sourceBuilder);
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        //解析结果
        List<ArticleDocument> articleDocuments = new ArrayList<>();
        for (SearchHit hit : searchResponse.getHits().getHits()) {
            Map<String, Object> map = hit.getSourceAsMap();//原来的结果
            //解析高亮的字段
            HighLightUtil.parseField(hit, "title");
            HighLightUtil.parseField(hit, "summary");
            HighLightUtil.parseField(hit, "content");

            ArticleDocument articleDocument = new ArticleDocument();
            articleDocument.setId(Long.valueOf((Integer) map.get("id")));
            articleDocument.setTitle((String) map.get("title"));
            articleDocument.setSummary((String) map.get("summary"));
            articleDocument.setContent((String) map.get("content"));
            articleDocuments.add(articleDocument);
        }
        return articleDocuments;
    }


    @Override
    @Cacheable(key = "'countByDate:'+#dateFilterType")
    public List<ArticleDateVO> countByDate(Integer dateFilterType) {
        if (dateFilterType == null) {
            dateFilterType = Constant.FILTER_BY_DAY;
        }
        return articleMapper.countByDate(dateFilterType);
    }

    @Override
    @Cacheable(key = "'listPreviewPageByTagId:'+#tagId+':'+#current")
    public Page<Article> listPreviewPageByTagId(Integer current, Integer size, Long tagId) {
        Page<Article> articlePage = new Page<>(current, size);
        return articleMapper.listPreviewPageByTagId(articlePage, tagId);
    }

    @Override
    @Cacheable(key = "'listPreviewPageByCategoryId:'+#categoryId+':'+#current")
    public Page<Article> listPreviewPageByCategoryId(Integer current, Integer size, Long categoryId) {
        Page<Article> articlePage = new Page<>(current, size);
        return articleMapper.listPreviewPageByCategoryId(articlePage, categoryId);
    }

    @Override
    public Article getNextPreviewById(Long id) {
        return articleMapper.selectNextPreviewById(id);
    }

    @Override
    public Article getPrevPreviewById(Long id) {
        return articleMapper.selectPrevPreviewById(id);
    }

    @Override
    @Cacheable(key = "'getDetailById:'+#id")
    public Article getDetailById(Long id) {
        //浏览次数加1
        increaseViews(id);
        return articleMapper.selectDetailById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void increaseViews(Long id) {
        QueryWrapper<Article> wrapper = new QueryWrapper<>();
        wrapper.select("views").eq("id", id);
        Article article = articleMapper.selectOne(wrapper);
        article.setId(id);
        article.setViews(article.getViews() + 1);
        articleMapper.updateById(article);
    }

    @Override
    @Cacheable(key = "'getById:'+#id")
    public Article getById(Long id) {
        return articleMapper.selectById(id);
    }

    @Override
    @Cacheable(key = "'countAll'")
    public long countAll() {
        return articleMapper.selectCount(null);
    }

    @Override
    @Cacheable(key = "'listTop'")
    public List<Article> listTop() {
        QueryWrapper<Article> wrapper = new QueryWrapper<>();
        wrapper.select("id", "title", "summary", "cover")
                .eq("published", true)
                .eq("top", true)
                .last("limit " + Constant.MAX_TOP_ARTICLES);
        return articleMapper.selectList(wrapper);
    }

    @Override
    @CacheEvict(allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public void removeByIdList(List<Long> idList) {
        articleMapper.deleteBatchIds(idList);
        //从ElasticSearch中删除
        ArrayList<ArticleDocument> articleDocuments = new ArrayList<>();
        for (Long id : idList) {
            ArticleDocument articleDocument = new ArticleDocument();
            articleDocument.setId(id);
            articleDocuments.add(articleDocument);
        }
        articleDocumentRepository.deleteAll(articleDocuments);
    }

    @Override
    @CacheEvict(allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public void removeById(Long id) {
        articleMapper.deleteById(id);
        //从ElasticSearch中删除
        articleDocumentRepository.deleteById(id);
    }

    @Override
    @Cacheable(key = "'listPreviewByPage:'+#current")
    public Page<Article> listPreviewByPage(Integer current, Integer size) {
        Page<Article> articlePage = new Page<>(current, size);
        return articleMapper.listPreviewByPage(articlePage);
    }

    @Override
    @Cacheable(key = "'listRecommend'")
    public List<Article> listRecommend() {
        return articleMapper.listRecommend(Constant.MAX_RECOMMEND_ARTICLES);
    }

    @Override
    @Cacheable
    public Page<Article> listTableByPage(Integer current, Integer size, ArticleQuery articleQuery) {
        System.out.println("查询数据库");
        Page<Article> page = new Page<>(current, size);
        QueryWrapper<Article> wrapper = new QueryWrapper<>();
        if (!StringUtils.isEmpty(articleQuery.getTitle())) {
            wrapper.like("title", articleQuery.getTitle());
        }
        if (articleQuery.getType() != null) {
            wrapper.eq("type", articleQuery.getType());
        }
        if (articleQuery.getCategoryId() != null) {
            wrapper.eq("category_id", articleQuery.getCategoryId());
        }
        if (articleQuery.getPublished() != null) {
            wrapper.eq("published", articleQuery.getPublished());
        }
        if (articleQuery.getStartDate() != null && articleQuery.getEndDate() != null) {
            wrapper.between(Constant.TABLE_ALIAS_ARTICLE + "create_time", articleQuery.getStartDate(), articleQuery.getEndDate());
        }
        return articleMapper.listTableByPage(page, wrapper);
    }

    @Override
    @Cacheable(key = "'listNewest'")
    public List<Article> listNewest() {
        QueryWrapper<Article> wrapper = new QueryWrapper<>();
        wrapper.select("id", "title", "summary", "create_time")
                .orderByDesc("create_time")
                .last("limit " + Constant.NEWEST_PAGE_SIZE);
        return articleMapper.selectList(wrapper);
    }

    @Override
    public Integer countByLastIndexViewToNow() {
        String username = UserInfoUtil.getUsername();
        if (StringUtils.isEmpty(username)) {
            return 0;
        }
        Date date = operationLogMapper.selectLastIndexViewTimeByUsername(username);
        QueryWrapper<Article> wrapper = new QueryWrapper<>();
        wrapper.between("create_time", date, new Date());
        return articleMapper.selectCount(wrapper);
    }

    @Override
    public Boolean reachedMaxRecommend() {
        QueryWrapper<Article> wrapper = new QueryWrapper<>();
        wrapper.eq("recommend", true);
        Integer count = articleMapper.selectCount(wrapper);
        return count >= Constant.MAX_RECOMMEND_ARTICLES;
    }

    @Override
    public Boolean reachedMaxTop() {
        QueryWrapper<Article> wrapper = new QueryWrapper<>();
        wrapper.eq("top", true);
        Integer count = articleMapper.selectCount(wrapper);
        return count >= Constant.MAX_TOP_ARTICLES;
    }

    @Override
    @CacheEvict(allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public void saveOrUpdate(Article article) {
        QueryWrapper<Tag> tagWrapper = new QueryWrapper<>();
        tagWrapper.select("id", "name");
        List<Tag> tagList = tagMapper.selectList(tagWrapper);
        //存在新标签则添加新标签
        List<Tag> newTagList = article.getTagList().stream().filter(t -> (t.getId() == null)).collect(Collectors.toList());
        for (Tag newTag : newTagList) {
            //添加标签
            newTag.setColor(Constant.DEFAULT_COLOR);
            newTag.setCreateTime(new Date());
            newTag.setUpdateTime(newTag.getCreateTime());
            tagMapper.insert(newTag);
        }
        if (article.getId() == null) {
            //新增
            articleMapper.insert(article);
        } else {
            //更新
            //更新文章信息
            articleMapper.updateById(article);
            //删除原有标签
            QueryWrapper<ArticleTag> articleTagWrapper = new QueryWrapper<>();
            articleTagWrapper.eq("article_id", article.getId());
            articleTagMapper.delete(articleTagWrapper);
            //从ElasticSearch中删除
            articleDocumentRepository.deleteById(article.getId());
        }
        //添加新标签
        List<Long> tagIdList = article.getTagList().stream().map(Tag::getId).collect(Collectors.toList());
        articleTagMapper.insertBatch(article.getId(), tagIdList);
        //添加到ElasticSearch中
        ArticleDocument articleDocument = new ArticleDocument();
        BeanUtils.copyProperties(article, articleDocument);
        if (articleDocument.getPublished() == null) {
            QueryWrapper<Article> wrapper = new QueryWrapper<>();
            wrapper.select("published").eq("id", article.getId());
            Article temp = articleMapper.selectOne(wrapper);
            articleDocument.setPublished(temp.getPublished());
        }
        articleDocumentRepository.save(articleDocument);
    }

}
