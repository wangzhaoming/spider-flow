package org.spiderflow.selenium.driver;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.spiderflow.model.SpiderNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

@Component
public class RemoteFireFoxDriverProvider implements DriverProvider {

	@Value("${selenium.driver.remote:null}")
	private String remoteDriverPath;

	@Override
	public String support() {
		return remoteDriverPath != null ? "remote" : null;
	}

	@Override
	public WebDriver getWebDriver(SpiderNode node, String proxyStr) {
		FirefoxOptions options = new FirefoxOptions();
		FirefoxProfile profile = new FirefoxProfile();
		if (StringUtils.isNotBlank(proxyStr)) {
			String[] hp = proxyStr.split(":");
			profile.setPreference("network.proxy.type", 1);
			profile.setPreference("network.proxy.http", hp[0]);
			profile.setPreference("network.proxy.http_port", NumberUtils.toInt(hp[1], 8080));
			profile.setPreference("network.proxy.no_proxies_on", "");
		}

		//设置User-Agent
		String userAgent = node.getStringJsonValue(USER_AGENT);
		if (StringUtils.isNotBlank(userAgent)) {
			profile.setPreference("general.useragent.override", userAgent);
		}
		//无头模式
		if ("1".equals(node.getStringJsonValue(HEADLESS))) {
			options.setHeadless(true);
		}
		// 是否启用JS,firefox必须启用javascript
		//profile.setPreference("javascript.enabled",!"1".equals(node.getStringJsonValue(JAVASCRIPT_DISABLED)));
		// 禁止加载图片
		if ("1".equals(node.getStringJsonValue(IMAGE_DISABLED))) {
			profile.setPreference("permissions.default.image", 2);
		}
		//设置窗口大小
		String windowSize = node.getStringJsonValue(WINDOW_SIZE);
		if (StringUtils.isNotBlank(windowSize)) {
			options.addArguments("--window-size=" + windowSize);
		}
		//设置其他参数
		String arguments = node.getStringJsonValue(ARGUMENTS);
		if (StringUtils.isNotBlank(arguments)) {
			options.addArguments(Arrays.asList(arguments.split("\r\n")));
		}
		String preferences = node.getStringJsonValue("preferences");
		if (StringUtils.isNotBlank(preferences)) {
			Arrays.asList(preferences.split("\r\n")).forEach(preference -> {
				int index = preference.indexOf("=");
				if (index > -1 && preference.length() > index + 1) {
					String key = preference.substring(0, index);
					String value = preference.substring(index + 1);
					if (StringUtils.isNotBlank(value)) {
						if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
							profile.setPreference(key, "true".equalsIgnoreCase(value));
						} else if ("0".equals(value) || NumberUtils.toInt(value, 0) != 0) {
							profile.setPreference(key, NumberUtils.toInt(value, 0));
						} else {
							profile.setPreference(key, value);
						}
					}
				}
			});
		}
		options.setProfile(profile);
		RemoteWebDriver driver = null;
		try {
			driver = new RemoteWebDriver(new URL(remoteDriverPath), options);
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
		//最大化
		if ("1".equals(node.getStringJsonValue(MAXIMIZED))) {
			driver.manage().window().maximize();
		}
		return driver;
	}
}
