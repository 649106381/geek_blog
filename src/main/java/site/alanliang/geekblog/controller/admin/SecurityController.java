package site.alanliang.geekblog.controller.admin;

import com.wf.captcha.utils.CaptchaUtil;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @Descriptin TODO
 * @Author AlanLiang
 * Date 2020/5/14 17:41
 * Version 1.0
 **/
@Controller
@RequestMapping("/admin")
public class SecurityController {

    @GetMapping({"/", "/login.html"})
    public String loginPage() {
        return "admin/home/login";
    }

    @GetMapping(value = "/403.html")
    public String noPermission() {
        return "error/403";
    }

    @RequestMapping("/captcha")
    public void captcha(HttpServletRequest request, HttpServletResponse response) throws Exception {
        CaptchaUtil.out(request, response);
    }
}
