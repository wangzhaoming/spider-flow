package org.spiderflow.proxypool;

import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spiderflow.proxypool.model.Proxy;
import org.spiderflow.proxypool.service.ProxyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class ProxyPoolManager {
	
	@Autowired
	private ProxyService proxyService;

	@Value("${spider.proxypool.check.sites:https://www.baidu.com}")
	private String checkSites;

	@Value("${spider.proxypool.check.timeout:10000}")
	private int checkTimeout;

	private final List<String> checkSiteArr = new ArrayList<>();

	private List<Proxy> proxys = Collections.emptyList();
	
	private static final Logger logger = LoggerFactory.getLogger(ProxyPoolManager.class);
	
	@PostConstruct
	private void init(){
		//读取全部代理存到内存中
		this.proxys = new CopyOnWriteArrayList<>(proxyService.findAll());
		//解析检测站点字符串
		if(StringUtils.isNotBlank(checkSites)){
			String[] tempArr = checkSites.split(",");
			for(String item : tempArr){
				item = item.trim();
				if(item.startsWith("http")){
					checkSiteArr.add(item);
				}
			}
		}
		logger.info("设置探测站点为：{}", checkSiteArr);
		logger.info("设置探测超时为：{}", checkTimeout);
	}

	synchronized public int count(){
		return this.proxys.size();
	}
	
	public void remove(Proxy proxy){
		this.proxys.remove(proxy);
		this.proxyService.remove(proxy);
	}
	
	public boolean add(Proxy proxy){
		if(this.proxys.contains(proxy)){
			return true;
		}
		if(check(proxy) != -1){
			boolean flag = proxyService.insert(proxy);
			if(flag){
				this.proxys.add(proxy);
			}
			return flag;
		}
		return false;
	}
	
	/**
	 * 随机获取一个http代理
	 * @return
	 */
	public Proxy getHttpProxy(){
		return getHttpProxy(true);
	}
	
	/**
	 * 随机获取一个https代理
	 * @return
	 */
	public Proxy getHttpsProxy(){
		return getHttpsProxy(true);
	}
	
	/**
	 * 随机获取一个HTTP代理
	 * @return
	 */
	public Proxy getHttpProxy(boolean anonymous){
		return random(get("http", anonymous));
	}
	
	/**
	 * 随机获取一个HTTPS代理
	 * @return
	 */
	public Proxy getHttpsProxy(boolean anonymous){
		return random(get("https", anonymous));
	}
	
	private Proxy random(List<Proxy> proxys){
		int size = 0;
		if(proxys != null && (size = proxys.size()) > 0){
			return proxys.get(RandomUtils.nextInt(0, size));
		}
		return null;
	}

	/**
	 * 查询符合条件的代理IP列表
	 *
	 * @param type      代理类型 http 或 https
	 * @param anonymous 是否支持匿名连接
	 * @return 代理IP列表
	 */
	private List<Proxy> get(String type, boolean anonymous) {
		List<Proxy> proxyList = new ArrayList<>();
		if (this.proxys != null) {
			for (Proxy proxy : proxys) {
				if (type.equalsIgnoreCase(proxy.getType()) && proxy.getAnonymous() == anonymous) {
					proxyList.add(proxy);
				}
			}
		}
		return proxyList;
	}
	
	/**
	 * 检测代理
	 * @param proxy
	 * @return
	 */
	public long check(Proxy proxy){
		try {
			long st = System.currentTimeMillis();
			for(String site : checkSiteArr) {
				logger.info("开始探测：{}",site);
				logger.info("使用代理：{}:{}",proxy.getIp(),proxy.getPort());
				Jsoup.connect(site)
						.ignoreContentType(true)
						.ignoreHttpErrors(true)
						.timeout(checkTimeout)
						.proxy(proxy.getIp(), proxy.getPort())
						.execute();
			}
			st =  System.currentTimeMillis() - st;
			logger.info("检测代理：{}:{},延迟:{}",proxy.getIp(),proxy.getPort(),st);
			return st;
		} catch (Exception e) {
			logger.info("检测代理：{}:{},超时",proxy.getIp(),proxy.getPort());
			logger.error("探测异常：{}",e.getMessage());
			return -1;
		}
	}
}
