package site.alanliang.geekblog.service;

import site.alanliang.geekblog.vo.InitInfoVO;

/**
 * @Descriptin TODO
 * @Author AlanLiang
 * Date 2020/4/5 22:15
 * Version 1.0
 **/
public interface MenuService {
    /**
     * 获取菜单树
     * @return
     */
    InitInfoVO menu();
}
