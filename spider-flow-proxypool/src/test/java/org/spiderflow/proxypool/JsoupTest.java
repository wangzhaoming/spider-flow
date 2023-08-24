package org.spiderflow.proxypool;

import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 测试网络探测
 *
 * @author tdx.lq
 * @since 2021-04-15 23:47
 */
public class JsoupTest {
    private final static Logger logger = LoggerFactory.getLogger(JsoupTest.class);
    public static void main(String[] args) {
        try {
            long st = System.currentTimeMillis();
            String site = "http://www.sse.com.cn";
            Jsoup.connect(site)
                    .ignoreContentType(true)
                    .ignoreHttpErrors(true)
                    .timeout(30000)
                    .proxy("8.133.191.41", 8080)
                    .execute();
            st =  System.currentTimeMillis() - st;
            logger.info("耗时：{}ms",st);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
