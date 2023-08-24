package org.spiderflow.addon.baidu.executor.function;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.spiderflow.annotation.Comment;
import org.spiderflow.annotation.Example;
import org.spiderflow.executor.FunctionExecutor;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.Date;

@Component
@Comment("百度搜索扩展方法")
public class BaiduExtendFunction implements FunctionExecutor {

    @Comment("将百度搜索结果中的时间转换为yyyy-MM-dd格式的字符串")
    @Example("${baidu.normalizeDate('5小时前')}")
    public static String normalizeDate(String date) {
        if (date == null) {
            return null;
        }
        if (date.contains("小时前")) {
            return DateFormatUtils.format(new Date(), "yyyy-MM-dd");
        }
        if (!date.contains("年")) {
            date = LocalDateTime.now().getYear() + "年" + date;
        }
        try {
            return DateFormatUtils.format(DateUtils.parseDate(date, "yyyy年M月d日"), "yyyy-MM-dd");
        } catch (ParseException e) {
            return null;
        }
    }

    @Override
    public String getFunctionPrefix() {
        return "baidu";
    }
}
