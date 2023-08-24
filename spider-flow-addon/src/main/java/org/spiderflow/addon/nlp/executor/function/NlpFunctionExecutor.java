package org.spiderflow.addon.nlp.executor.function;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.corpus.tag.Nature;
import com.hankcs.hanlp.seg.Segment;
import org.spiderflow.annotation.Comment;
import org.spiderflow.annotation.Example;
import org.spiderflow.executor.FunctionExecutor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@Comment("自然语言处理")
public class NlpFunctionExecutor implements FunctionExecutor {

    private static final Segment segment = HanLP.newSegment().enableOrganizationRecognize(true);
    @Override
    public String getFunctionPrefix() {
        return "nlp";
    }

    @Comment("从语料中解析机构名称，返回一个列表")
    @Example("${nlp.findOrgs(content)}")
    public static List<String> findOrgs(String text) {
        return segment.seg(text).stream().filter(t -> t.nature.equals(Nature.nt))
                .map(term -> term.word)
                .distinct()
                .collect(Collectors.toList());
    }

    public static void main(String[] args) {
        String t="经上海市食品药品检验研究院等4家药品检验机构检验，发现11家企业生产的14批次药品不符合规定。现将相关情况通告如下：\n" +
                "一、经上海市食品药品检验研究院检验，标示为山东瑞安药业有限公司委托赤峰万泽药业股份有限公司生产的3批次他克莫司软膏不符合规定，不符合规定项目为有关物质。\n" +
                "经中国食品药品检定研究院检验，标示为浙江尖峰药业有限公司生产的1批次盐酸奥洛他定滴眼液不符合规定，不符合规定项目为装量。\n" +
                "经深圳市药品检验研究院检验，标示为山西康益恒泰药业有限公司生产的1批次炒僵蚕不符合规定，不符合规定项目为性状、总灰分。\n" +
                "经浙江省食品药品检验研究院检验，标示为河北润华药业有限公司、河北康益强药业有限公司、安徽旭松中药饮片有限公司、安徽省泽华国药饮片有限公司、山东千禾中药饮片有限公司、成都鹤祥天药业有限公司、榆林市广济堂中药开发有限责任公司、新疆恩泽中药饮片有限公司生产的9批次炒酸枣仁不符合规定，不符合规定项目为水分。\n" +
                "二、对上述不符合规定药品，药品监督管理部门已要求相关企业和单位采取暂停销售使用、召回等风险控制措施，对不符合规定原因开展调查并切实进行整改。\n" +
                "三、国家药品监督管理局要求相关省级药品监督管理部门依据《中华人民共和国药品管理法》，组织对上述企业和单位存在的涉嫌违法行为立案调查，并按规定公开查处结果。\n" +
                "特此通告。\n" +
                "附件：1. 14批次不符合规定药品名单\n" +
                "2. 不符合规定项目的小知识\n" +
                "\n" +
                "                              国家药监局\n" +
                "2023年8月15日\n" +
                "国家药品监督管理局2023年第36号通告附件1.docx\n" +
                "国家药品监督管理局2023年第36号通告附件2.doc";

            System.out.println(findOrgs(t));
    }
}
