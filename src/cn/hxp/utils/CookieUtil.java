package cn.hxp.utils;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import cn.hxp.entity.THxpUser;
import cn.hxp.service.THxpUserBiz;
import cn.hxp.service.impl.THxpUserImpl;

import javax.annotation.Resource;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.sun.org.apache.xml.internal.security.exceptions.Base64DecodingException;
import com.sun.org.apache.xml.internal.security.utils.Base64;

public class CookieUtil {
	
	@Resource
	private static THxpUserBiz userBiz;
	
	// 保存cookie时的cookieName
	public final static String cookieDomainName = "hxp";
	// 加密cookie时的网站自定码
	private final static String webKey = "123321";
	// 设置cookie有效期是两个星期，根据需要自定义
	private final static long cookieMaxAge = 60 * 60 * 24 * 7 * 2;

	// 保存Cookie到客户端-------------------------------------------------------------------------
	// 在CheckLogonServlet.java中被调用
	// 传递进来的user对象中封装了在登陆时填写的用户名与密码
	public static void saveCookie(THxpUser user, HttpServletResponse response) {
		// cookie的有效期
		long validTime = System.currentTimeMillis() + (cookieMaxAge * 5000);
		// MD5加密用户详细信息
//		String cookieValueWithMd5 = getMD5(user.getUserName() + ":"
//				+ user.getUserPassword() + ":" + validTime + ":" + webKey);
		String passwordMD5 = getMD5(user.getUserPassword()+":"+ webKey);
		// 将要被保存的完整的Cookie值

		String cookieValue = user.getUserName() + ":" + passwordMD5 + ":" + validTime;
//		String cookieValue = user.getUserName() + ":" + passwordMD5 + ":" + validTime + ":"
//				+ cookieValueWithMd5;
		// 再一次对Cookie的值进行BASE64编码
		String cookieValueBase64 = new String(Base64.encode(cookieValue
				.getBytes()));
		// 开始保存Cookie
		Cookie cookie = new Cookie(cookieDomainName, cookieValueBase64);
		// 存两年(这个值应该大于或等于validTime)
		cookie.setMaxAge(60 * 60 * 24 * 365 * 2);
		// cookie有效路径是网站根目录
		cookie.setPath("/");
		// 向客户端写入
		response.addCookie(cookie);
	}

	// 获取Cookie组合字符串的MD5码的字符串----------------------------------------------------------------
	public static String getMD5(String value) {
		String result = null;
		try {
			byte[] valueByte = value.getBytes();
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(valueByte);
			result = toHex(md.digest());
		} catch (NoSuchAlgorithmException e1) {
			e1.printStackTrace();
		}
		return result;
	}

	// 将传递进来的字节数组转换成十六进制的字符串形式并返回----------------------------------------------------------------
	private static String toHex(byte[] buffer) {
		StringBuffer sb = new StringBuffer(buffer.length * 2);
		for (int i = 0; i < buffer.length; i++) {
			sb.append(Character.forDigit((buffer[i] & 0xf0) >> 4, 16));
			sb.append(Character.forDigit(buffer[i] & 0x0f, 16));
		}
		return sb.toString();
	}

	// 读取Cookie,自动完成登陆操作----------------------------------------------------------------
	// 在Filter程序中调用该方法,见AutoLogonFilter.java
	public static Map<String, String> readCookieAndLogon(HttpServletRequest request,
			HttpServletResponse response, THxpUser user,String cookieValues[])
			throws IOException, ServletException, UnsupportedEncodingException, Base64DecodingException {
		// 根据cookieName取cookieValue
		Map<String,String> map = new HashMap<String, String>();
//		Cookie cookies[] = request.getCookies();
//		String cookieValue = null;
//		if (cookies != null) {
//			for (int i = 0; i < cookies.length; i++) {
//				if (cookieDomainName.equals(cookies[i].getName())) {
//					cookieValue = cookies[i].getValue();
//					break;
//				}
//			}
//		}
		// 如果cookieValue为空,返回,
//		if (cookieValue == null) {
//			map.put("result", "0");
//			map.put("msg", "未找到cookie");
//			return map;
//		}
//		 如果cookieValue不为空,才执行下面的代码
//		 先得到的CookieValue进行Base64解码
//		String cookieValueAfterDecode = new String(Base64.decode(cookieValue),
//				"utf-8");
		// 对解码后的值进行分拆,得到一个数组,如果数组长度不为3,就是非法登陆
//		String cookieValues[] = cookieValueAfterDecode.split(":");
		if (cookieValues.length != 3) {
//			response.setContentType("text/html;charset=utf-8");
//			PrintWriter out = response.getWriter();
//			out.println("你正在用非正常方式进入本站...");
//			out.close();
			map.put("result", "1");
			map.put("msg", "非法登录");
			return map;
		}
		
//		 判断是否在有效期内,过期就删除Cookie
		long validTimeInCookie = new Long(cookieValues[1]);
		if (validTimeInCookie < System.currentTimeMillis()) {
			// 删除Cookie
			clearCookie(response);
			map.put("result", "2");
			map.put("msg", "cookie已失效");
			return map;
		}
//		String username = cookieValues[0];
		// 如果user返回不为空,就取出密码,使用用户名+密码+有效时间+ webSiteKey进行MD5加密
		if (user != null) {
			String md5ValueInCookie = cookieValues[2];
			String md5ValueFromUser = getMD5(user.getUserName() + ":"
					+ user.getUserPassword() + ":" + validTimeInCookie + ":"
					+ webKey);
			// 将结果与Cookie中的MD5码相比较,如果相同,写入Session,自动登陆成功,并继续用户请求
			if (md5ValueFromUser.equals(md5ValueInCookie)) {
				map.put("result", "3");
				map.put("msg", "验证成功！");
				return map;
			}
		} else {
			// 返回为空执行
			map.put("result", "4");
			map.put("msg", "验证失败！");
			return map;
		}
		return map;
	}

	// 用户注销时,清除Cookie,在需要时可随时调用-----------------------------------------------------
	public static void clearCookie(HttpServletResponse response) {
		Cookie cookie = new Cookie(cookieDomainName, null);
		cookie.setMaxAge(0);
		cookie.setPath("/");
		response.addCookie(cookie);
	}

}
