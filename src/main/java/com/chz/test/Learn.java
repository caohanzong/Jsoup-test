package com.chz.test;

import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpUtil;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author snicker
 * @date 2022/1/25 10:31
 * @Describe
 */
public class Learn {
    // 获取当前主机的处理机内核数
    // public static int coreSize = Runtime.getRuntime().availableProcessors();
    // 处理的太快，会超出QPS限制，报错status=514
    public static int coreSize = 4;
    // 启用线程池，第一个参数为执行的线程的数量，第二个参数为最大执行线程的数量，第三个为long类型的线程保持时间，第四个为时间的单位，第五个为线程集合
    public static ThreadPoolExecutor executor = new ThreadPoolExecutor(coreSize, coreSize+1, 10L, TimeUnit.SECONDS, new LinkedBlockingDeque<>());

    public static void main(String[] args) throws IOException {
        String regx = "[^0-9]";
        Pattern compile = Pattern.compile(regx);
        // 爬取的图片网站的根链接
        String baseUrl = "https://www.tupianzj.com";
        // 实现翻页循环
        for (int j = 1; j < 10; j++) {
            System.out.println("正在下载第" + j + "页");
            Connection connect = Jsoup.connect(baseUrl + "/sheying/fengjing/list_10_" + j + ".html");
            // 获取当前页面的元素
            Document document = connect.get();
            // 通过标签class名，获取照片对应的图集的链接的标签
            Elements elements = document.body().getElementsByClass("list_con_box_ul").select("li");

            for (Element element : elements) {
                // 线程
                Runnable task = () -> {
                    try {
                        // 获取首页照片的标签的链接，需要拼接
                        String hrefStr = element.child(0).attr("href");
                        Connection subConnection = Jsoup.connect(baseUrl + hrefStr);
                        // 添加用户代理，伪装成浏览器
                        subConnection.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/94.0.4606.81 Safari/537.36");
                        // 设置超时时间为8000毫秒
                        subConnection.timeout(8000);
                        // 获取新页面的页面文档
                        Document subDocument = subConnection.get();
                        // 新页面的第二个h1标签，获取图片的名称
                        String title = subDocument.body().getElementsByTag("h1").eq(1).html();
                        System.out.println("正在下载：" + title);
                        String pageStr = subDocument.body().getElementsByClass("pages").select("li").eq(0).text();
                        // 使用正则表达式，匹配除页码以外的内容
                        Matcher matcher = compile.matcher(pageStr);

                        if (StringUtils.isNoneBlank(matcher.replaceAll("").trim())) {
                            // 将除了页码以外通过正则表达式匹配到的内容替换为""，并且除去空格
                            int pageNo = Integer.parseInt(matcher.replaceAll("").trim());
                            // 获取相同图片名称下的所有照片
                            for (int i = 0; i < pageNo; i++) {
                                // 拼接图集打开后不同的的url
                                String url = baseUrl + hrefStr;
                                if (i != 0) {
                                    int page = i + 1;
                                    url = url.replace(".html", "_" + page + ".html");
                                }
                                // 获取新的页面的连接和页面文档
                                subConnection = Jsoup.connect(url);
                                subDocument = subConnection.get();
                                // 通过保存图片的页面元素的id获取图片的地址
                                String src = subDocument.getElementById("bigpicimg").attr("src");
                                System.out.println(src);
                                // 通过图片地址进行下载，第一个参数为地址，第二个参数为保存地址，fileUtil.mkdir方法内的文件夹，有则使用，无则创建
                                HttpUtil.downloadFile(src, FileUtil.mkdir("d:/learn/" + title + "/"));
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                };
                // 执行线程task
                executor.execute(task);
            }
        }
    }


}
















































