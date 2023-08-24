package org.spiderflow.core.executor.shape;

import com.alibaba.fastjson.JSON;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.ibatis.jdbc.SQL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spiderflow.context.SpiderContext;
import org.spiderflow.core.serializer.FastJsonSerializer;
import org.spiderflow.core.utils.DataSourceUtils;
import org.spiderflow.core.utils.DataUtils;
import org.spiderflow.core.utils.ExpressionUtils;
import org.spiderflow.executor.ShapeExecutor;
import org.spiderflow.io.SpiderResponse;
import org.spiderflow.listener.SpiderListener;
import org.spiderflow.model.SpiderNode;
import org.spiderflow.model.SpiderOutput;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

/**
 * 输出执行器
 * @author Administrator
 *
 */
@Component
public class OutputExecutor implements ShapeExecutor, SpiderListener {

	public static final String OUTPUT_ALL = "output-all";

	public static final String OUTPUT_NAME = "output-name";

	public static final String OUTPUT_VALUE = "output-value";

	public static final String DATASOURCE_ID = "datasourceId";

	public static final String OUTPUT_DATABASE = "output-database";

	public static final String OUTPUT_CSV = "output-csv";

	public static final String TABLE_NAME = "tableName";

	public static final String CSV_NAME = "csvName";

	public static final String CSV_ENCODING = "csvEncoding";

	private static Logger logger = LoggerFactory.getLogger(OutputExecutor.class);

	/**
	 * 输出CSVPrinter节点变量
	 */
	private final Map<String, CSVPrinter> cachePrinter = new HashMap<>();

	@Override
	public void execute(SpiderNode node, SpiderContext context, Map<String,Object> variables) {
		SpiderOutput output = new SpiderOutput();
		output.setNodeName(node.getNodeName());
		output.setNodeId(node.getNodeId());
		boolean outputAll = "1".equals(node.getStringJsonValue(OUTPUT_ALL));
		boolean databaseFlag = "1".equals(node.getStringJsonValue(OUTPUT_DATABASE));
		boolean csvFlag = "1".equals(node.getStringJsonValue(OUTPUT_CSV));
		if (outputAll) {
			outputAll(output, variables);
		}
		List<Map<String, String>> outputs = node.getListJsonValue(OUTPUT_NAME, OUTPUT_VALUE);
		Map<String, Object> outputData = null;
		if (databaseFlag || csvFlag) {
			outputData = new LinkedHashMap<>(outputs.size());
		}
		for (Map<String, String> item : outputs) {
			Object value = null;
			String outputValue = item.get(OUTPUT_VALUE);
			String outputName = item.get(OUTPUT_NAME);
			try {
				value = ExpressionUtils.execute(outputValue, variables);
				context.pause(node.getNodeId(),"common",outputName,value);
				logger.debug("输出{}={}", outputName,value);
			} catch (Exception e) {
				logger.error("输出{}出错，异常信息：{}", outputName,e);
			}
			output.addOutput(outputName, value);
			variables.put(outputName, value);
			if (databaseFlag || csvFlag) {
				outputData.put(outputName, value);
			}
		}
		if(databaseFlag){
			String dsId = node.getStringJsonValue(DATASOURCE_ID);
			String tableName = node.getStringJsonValue(TABLE_NAME);
			if (StringUtils.isBlank(dsId)) {
				logger.warn("数据源ID为空！");
			} else if (StringUtils.isBlank(tableName)) {
				logger.warn("表名为空！");
			} else {
				outputDB(dsId, tableName, outputData);
			}
		}
		if (csvFlag) {
			String csvName = node.getStringJsonValue(CSV_NAME);
			outputCSV(node, context, csvName, outputData);
		}
		context.addOutput(output);
	}

	/**
	 * 输出所有参数
	 * @param output
	 * @param variables
	 */
	private void outputAll(SpiderOutput output,Map<String,Object> variables){
		for (Map.Entry<String, Object> item : variables.entrySet()) {
			Object value = item.getValue();
			if (value instanceof SpiderResponse) {
				SpiderResponse resp = (SpiderResponse) value;
				output.addOutput(item.getKey() + ".html", resp.getHtml());
				continue;
			}
			//去除不输出的信息
			if ("ex".equals(item.getKey())) {
				continue;
			}
			//去除不能序列化的参数
			try {
				JSON.toJSONString(value, FastJsonSerializer.serializeConfig);
			} catch (Exception e) {
				e.printStackTrace();
				continue;
			}
			//输出信息
			output.addOutput(item.getKey(), item.getValue());
		}
	}

	private void outputDB(String databaseId, String tableName, Map<String, Object> data) {
		if (data == null || data.isEmpty()) {
			return;
		}
		JdbcTemplate template = new JdbcTemplate(DataSourceUtils.getDataSource(databaseId));
		Set<String> keySet = data.keySet();
		Object[] params = new Object[data.size()];
		SQL sql = new SQL();
		//设置表名
		sql.INSERT_INTO(tableName);
		int index = 0;
		//设置字段名
		for (String key : keySet) {
			sql.VALUES(key, "?");
			params[index] = data.get(key);
			index++;
		}
		try {
			//执行sql
			template.update(sql.toString(), params);
		} catch (Exception e) {
			logger.error("执行sql出错,异常信息:{}", e.getMessage(), e);
			ExceptionUtils.wrapAndThrow(e);
		}
	}

	private void outputCSV(SpiderNode node, SpiderContext context, String csvName, Map<String, Object> data) {
		if (data == null || data.isEmpty()) {
			return;
		}
		csvName = (String) ExpressionUtils.execute(csvName, null);
		String key = context.getId() + "-" + node.getNodeId();
		String[] headers = data.keySet().toArray(new String[data.size()]);
		try {
			synchronized (cachePrinter) { // fixme 性能不好
				CSVPrinter printer = cachePrinter.get(key);
				if (printer == null) {
					CSVFormat format = CSVFormat.DEFAULT.withHeader(headers);
					String csvEncoding = node.getStringJsonValue(CSV_ENCODING);

					File file = new File(csvName);
					if(file.exists()) {
						printer = new CSVPrinter(new OutputStreamWriter(new FileOutputStream(file, true), csvEncoding), format.withSkipHeaderRecord());
					}
					else {
						printer = new CSVPrinter(new OutputStreamWriter(new FileOutputStream(file), csvEncoding), format);
					}
					cachePrinter.put(key, printer);
				}
				printer.printRecord(DataUtils.replaceNewlineCharInString(data.values()));
			}
		} catch (IOException e) {
			logger.error("文件输出错误,异常信息:{}", e.getMessage(), e);
			ExceptionUtils.wrapAndThrow(e);
		}
	}

	@Override
	public String supportShape() {
		return "output";
	}

	@Override
	public void beforeStart(SpiderContext context) {

	}

	@Override
	public void afterEnd(SpiderContext context) {
		this.releasePrinters(context);
	}

	private void releasePrinters(SpiderContext context) {
		synchronized (cachePrinter) {
			for (Iterator<Map.Entry<String, CSVPrinter>> iterator = this.cachePrinter.entrySet().iterator(); iterator.hasNext(); ) {
				Map.Entry<String, CSVPrinter> entry = iterator.next();
				// 仅删除当前流程的printer
				if (entry.getKey().startsWith(context.getId())) {
					CSVPrinter printer = entry.getValue();
					if (printer != null) {
						try {
							printer.flush();
							printer.close();
							this.cachePrinter.remove(entry.getKey());
						} catch (IOException e) {
							logger.error("文件输出错误,异常信息:{}", e.getMessage(), e);
							ExceptionUtils.wrapAndThrow(e);
						}
					}
				}
			}
		}
	}
}
