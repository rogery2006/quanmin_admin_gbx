package com.ybg.rbac.controllor;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import net.sf.json.JSONObject;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import com.ybg.base.jdbc.BaseMap;
import com.ybg.base.jdbc.util.DateUtil;
import com.ybg.base.util.Common;
import com.ybg.base.util.DesUtils;
import com.ybg.base.util.Json;
import com.ybg.base.util.ServletUtil;
import com.ybg.base.util.SystemConstant;
import com.ybg.base.util.VrifyCodeUtil;
import com.ybg.component.email.sendemail.SendEmailInter;
import com.ybg.component.email.sendemail.SendQQmailImpl;
import com.ybg.rbac.resources.service.ResourcesService;
import com.ybg.rbac.user.UserStateConstant;
import com.ybg.rbac.user.domain.UserVO;
import com.ybg.rbac.user.qvo.UserQuery;
import com.ybg.rbac.user.service.LoginService;
import com.ybg.rbac.user.service.UserService;

/*** ???Shiro?????? **/
@Api(tags = "??????????????????")
@Controller
public class LoginControllor {
	
	@Autowired
	UserService				userService;
	@Autowired
	ResourcesService		resourcesService;
	@Autowired
	LoginService			loginService;
	@Autowired
	AuthenticationManager	authenticationManager;
	
	@ApiOperation(value = "????????????", notes = "", produces = MediaType.TEXT_HTML_VALUE)
	@RequestMapping(value = { "/common/login_do/tologin.do", "/" }, method = { RequestMethod.GET, RequestMethod.POST })
	public String tologin(ModelMap map) {
		map.put("icp", SystemConstant.getICP());
		map.put("systemname", SystemConstant.getSystemName());
		map.put("systemdomain", SystemConstant.getSystemdomain());
		return "/login";
	}
	
	@ApiOperation(value = "???????????????????????????", notes = "", produces = MediaType.TEXT_HTML_VALUE)
	@ResponseBody
	@RequestMapping(value = { "/common/login_do/system_authinfo.do" }, method = { RequestMethod.GET, RequestMethod.POST })
	public String system_authinfo() {
		return "?? 2016-2016 " + SystemConstant.getSystemdomain() + " ???????????? ICP??????" + SystemConstant.getICP();
	}
	
	@ApiOperation(value = "???????????? ", notes = "", produces = MediaType.TEXT_HTML_VALUE)
	@RequestMapping(value = "/common/login_do/loginout.do", method = RequestMethod.GET)
	public String loginout(HttpServletRequest request, HttpServletResponse response) {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth != null) {
			new SecurityContextLogoutHandler().logout(request, response, auth);
		}
		return "redirect:/common/login_do/tologin.do";
	}
	
	@ApiOperation(value = "???????????? ", notes = "", produces = MediaType.ALL_VALUE)
	@ApiImplicitParams({ @ApiImplicitParam(name = "username", value = "??????", dataType = "java.lang.String", required = true), @ApiImplicitParam(name = "password", value = "??????", dataType = "java.lang.String", required = true) })
	@RequestMapping(value = "/common/login_do/login.do", method = { RequestMethod.GET, RequestMethod.POST })
	public String login(HttpServletRequest httpServletRequest, ModelMap map) throws Exception {
		// ?????????????????????
		if (!VrifyCodeUtil.checkvrifyCode(httpServletRequest, map)) {
			return "/login";
		}
		String username = ServletUtil.getStringParamDefaultBlank(httpServletRequest, "username");
		String password = ServletUtil.getStringParamDefaultBlank(httpServletRequest, "password");
		UserVO user = userService.login(username);
		// BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
		if (!(user.isAccountNonLocked())) {
			map.put("error", "????????????????????????????????????????????????????????????");
		}
		if (!user.isAccountNonExpired()) {
			map.put("error", "??????????????????");
		}
		if (new DesUtils().encrypt(password).equals(user.getCredentialssalt())) {
			UsernamePasswordAuthenticationToken token2 = new UsernamePasswordAuthenticationToken(user.getUsername(), new DesUtils().decrypt(user.getCredentialssalt()));
			token2.setDetails(new WebAuthenticationDetails(httpServletRequest));
			Authentication authenticatedUser = authenticationManager.authenticate(token2);
			SecurityContextHolder.getContext().setAuthentication(authenticatedUser);
			return "redirect:/common/login_do/index.do";
		}
		else {
			map.put("error", "???????????????????????????");
			return "/login";
		}
	}
	
	@ApiOperation(value = "????????????????????? ", notes = "", produces = MediaType.TEXT_HTML_VALUE)
	@RequestMapping(value = { "/common/login_do/unauthorizedUrl.do" }, method = { RequestMethod.GET, RequestMethod.POST })
	public String unauthorizedUrl() throws Exception {
		return "/denied";
	}
	
	@ApiOperation(value = "????????????", notes = "", produces = MediaType.TEXT_HTML_VALUE)
	@RequestMapping(value = { "/common/login_do/toregister.do" }, method = { RequestMethod.GET, RequestMethod.POST })
	public String toregister() {
		return "/register";
	}
	
	/** ??????
	 *
	 * @throws Exception **/
	@ApiOperation(value = "??????", notes = " ", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	@RequestMapping(value = "/common/login_do/register.do", method = { RequestMethod.GET, RequestMethod.POST })
	public Json register(UserVO user, @RequestParam(name = "email", required = true) String email, @RequestParam(name = VrifyCodeUtil.PARAMETERNAME, required = true) String vrifyCode, HttpSession session) throws Exception {
		Json j = new Json();
		if (!VrifyCodeUtil.checkvrifyCode(vrifyCode, session)) {
			j.setSuccess(true);
			j.setMsg("?????????????????????");
			return j;
		}
		j.setSuccess(true);
		j.setMsg("????????????????????????????????????????????????????????????3??????????????????????????????????????????");
		String now = DateUtil.getDateTime();
		user.setPassword(new DesUtils(user.getUsername() + now).encrypt(user.getPassword()));
		user.setCredentialssalt(new DesUtils().encrypt(user.getPassword()));
		user.setRoleid("10");
		user.setPhone("");
		user.setState(UserStateConstant.DIE);
		user.setCreatetime(now);
		try {
			userService.save(user);
		} catch (Exception e) {
			e.printStackTrace();
			j.setMsg("?????????????????????????????????");
			return j;
		}
		String contemt = "<a href='" + SystemConstant.getSystemdomain() + "/common/login_do/relife.do?userid=" + user.getId() + "&salt=" + user.getCredentialssalt() + "'>??????</a>";
		try {
			SendEmailInter send = new SendQQmailImpl();
			send.sendMail(email, SystemConstant.getSystemName() + "??????", contemt);
		} catch (Exception e) {
			e.printStackTrace();
			BaseMap<String, Object> wheremap = new BaseMap<String, Object>();
			wheremap.put("id", user.getId());
			userService.remove(wheremap);
			j.setMsg("???????????????????????????????????????????????????????????????????????????????????????");
			return j;
		}
		return j;
	}
	
	@ApiOperation(value = "??????????????????", notes = "", produces = MediaType.TEXT_HTML_VALUE)
	@RequestMapping(value = "/common/login_do/relife.do", method = RequestMethod.GET)
	public String relife(@RequestParam(name = "username", required = true) String username, @RequestParam(name = "salt", required = true) String salt, ModelMap map) throws Exception {
		UserQuery qvo = new UserQuery();
		qvo.setUsername(username);
		qvo.setState(UserStateConstant.DIE);
		List<UserVO> list = userService.list(qvo);
		if (list != null && list.size() == 1) {
			BaseMap<String, Object> updatemap = new BaseMap<String, Object>();
			BaseMap<String, Object> wheremap = new BaseMap<String, Object>();
			updatemap.put("state", UserStateConstant.OK);
			wheremap.put("username", list.get(0).getUsername());
			userService.update(updatemap, wheremap);
			map.put("error", "???????????? ?????????????????????");
			return "/login";
		}
		map.put("error", "?????????????????????");
		return "/login";
	}
	
	// /** ???????????? **/
	@ApiOperation(value = "????????????", notes = " ", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiImplicitParams({ @ApiImplicitParam(name = "username", value = "??????", dataType = "java.lang.String", required = true), @ApiImplicitParam(name = "password", value = "??????", dataType = "java.lang.String", required = true) })
	@ResponseBody
	@RequestMapping(value = "/common/login_do/forgetpwd.do", method = RequestMethod.GET)
	public Json forgetpwd(@RequestParam(name = "username", required = true) String username, Model model, @RequestParam(name = VrifyCodeUtil.PARAMETERNAME, required = true) String vrifyCode, HttpSession session) throws Exception {
		Json j = new Json();
		if (!VrifyCodeUtil.checkvrifyCode(vrifyCode, session)) {
			j.setSuccess(true);
			j.setMsg("?????????????????????");
			return j;
		}
		j.setSuccess(true);
		UserQuery userqvo = new UserQuery();
		userqvo.setUsername(username);
		List<UserVO> userlist = userService.list(userqvo);
		if (userlist == null || userlist.size() == 0) {
			j.setSuccess(false);
			j.setMsg("????????????");
			return j;
		}
		UserVO user = userlist.get(0);
		if (user.getState().equals(UserStateConstant.LOCK)) {
			j.setSuccess(false);
			j.setMsg("???????????? ???????????????");
			return j;
		}
		if (user.getState().equals(UserStateConstant.DIE)) {
			j.setSuccess(false);
			j.setMsg("??????????????? ???????????????");
			return j;
		}
		if (!user.getState().equals(UserStateConstant.OK)) {
			j.setSuccess(false);
			j.setMsg("???????????? ???????????????");
			return j;
		}
		// ?????? ???????????? ?????? ?????????????????????ID
		JSONObject json = new JSONObject();
		json.put("uid", user.getId());
		json.put("dietime", DateUtil.getDate());
		String encryptInfo = json.toString();
		encryptInfo = "encryptInfo=" + new DesUtils().encrypt(encryptInfo);
		String contemt = "<a href='" + SystemConstant.getSystemdomain() + "/common/login_do/resetpwd.do?" + encryptInfo + "'>?????????????????????????????????????????????24???00</a>";
		try {
			SendEmailInter send = new SendQQmailImpl();
			send.sendMail(user.getEmail(), SystemConstant.getSystemName() + "-????????????", contemt);
		} catch (Exception e) {
			e.printStackTrace();
			j.setMsg("?????????????????????????????????????????????");
			return j;
		}
		j.setMsg("?????????????????????????????????????????????");
		return j;
	}
	
	// /** ????????????????????? **/
	@ApiOperation("??????????????????")
	@ApiImplicitParams({ @ApiImplicitParam(name = "encryptInfo", value = "????????????", dataType = "java.lang.String", required = true), @ApiImplicitParam(name = "password", value = "??????", dataType = "java.lang.String", required = true) })
	@RequestMapping(value = "/common/login_do/resetpwd.do", method = RequestMethod.GET)
	public String resetpwd(@RequestParam(name = "encryptInfo", required = true) String encryptInfo, Model model) {
		try {
			JSONObject json = JSONObject.fromObject(new DesUtils().decrypt(encryptInfo));
			String userid = json.getString("uid");
			String dietime = json.getString("dietime");
			if (dietime.equals(DateUtil.getDate())) {
				UserVO user = userService.get(userid);
				if (user.getState().equals(UserStateConstant.LOCK)) {
					return "/lock";
				}
				if (user.getState().equals(UserStateConstant.DIE)) {
					return "/die";
				}
				if (!user.getState().equals(UserStateConstant.OK)) {
					return "";
				}
				model.addAttribute("encryptInfo", encryptInfo);
				return "/reset";
			}
		} catch (Exception e) {
			e.printStackTrace();
			model.addAttribute("msg", "??????????????????");
			return "/fail";
		}
		model.addAttribute("msg", "??????????????????");
		return "/fail";
	}
	
	// /** ???????????? **/
	@ApiOperation(value = "????????????", notes = " ", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiImplicitParams({ @ApiImplicitParam(name = "encryptInfo", value = "????????????", dataType = "java.lang.String", required = true), @ApiImplicitParam(name = "password", value = "??????", dataType = "java.lang.String", required = true) })
	@ResponseBody
	@RequestMapping(value = "/common/login_do/resetpassword.do", method = { RequestMethod.GET, RequestMethod.POST })
	public Json resetpassword(@RequestParam(name = "encryptInfo", required = true) String encryptInfo, @RequestParam(name = "password", required = true) String password, Model model) throws Exception {
		Json j = new Json();
		j.setSuccess(true);
		j.setMsg("????????????");
		try {
			JSONObject json = JSONObject.fromObject(new DesUtils().decrypt(encryptInfo));
			String userid = json.getString("uid");
			String dietime = json.getString("dietime");
			if (!dietime.equals(DateUtil.getDate())) {
				j.setSuccess(false);
				j.setMsg("???????????????????????????");
				return j;
			}
			UserVO user = userService.get(userid + "");
			if (user.getState().equals(UserStateConstant.LOCK)) {
				j.setSuccess(false);
				j.setMsg("???????????? ???????????????");
				return j;
			}
			if (user.getState().equals(UserStateConstant.DIE)) {
				j.setSuccess(false);
				j.setMsg("??????????????? ???????????????");
				return j;
			}
			if (!user.getState().equals(UserStateConstant.OK)) {
				j.setSuccess(false);
				j.setMsg("???????????? ???????????????");
				return j;
			}
			BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
			BaseMap<String, Object> updatemap = new BaseMap<String, Object>();
			BaseMap<String, Object> wheremap = new BaseMap<String, Object>();
			updatemap.put("password", passwordEncoder.encode(password));
			updatemap.put("credentialssalt", new DesUtils().encrypt(password));
			wheremap.put("id", user.getId());
			userService.update(updatemap, wheremap);
		} catch (Exception e) {
			e.printStackTrace();
			j.setSuccess(false);
			j.setMsg("???????????????");
			return j;
		}
		return j;
	}
	
	/** ???????????????????????? **/
	@ApiOperation(value = " ????????????????????????", notes = " ", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	@RequestMapping(value = { "/common/login_do/isexist.do" }, method = { RequestMethod.GET, RequestMethod.POST })
	public boolean isexist(UserQuery qvo) {
		return userService.checkisExist(qvo);
	}
	
	/** ????????????
	 * 
	 * @throws Exception **/
	@ApiOperation(value = "????????????", notes = " ", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	@RequestMapping(value = { "/common/login_do/modifypwd" }, method = { RequestMethod.GET, RequestMethod.POST })
	public Json modifypwd(@RequestParam(name = "password", required = true) String password) throws Exception {
		Json j = new Json();
		j.setSuccess(true);
		j.setMsg("????????????");
		UserVO user = (UserVO) Common.findUserSession();
		if (user == null) {
			j.setMsg("???????????????");
			return j;
		}
		BaseMap<String, Object> updatemap = new BaseMap<String, Object>();
		BaseMap<String, Object> wheremap = new BaseMap<String, Object>();
		updatemap.put("password", new DesUtils(user.getUsername() + user.getCreatetime()).encrypt(user.getPassword()));
		updatemap.put("credentialssalt", new DesUtils().encrypt(password));
		wheremap.put("id", user.getId());
		userService.update(updatemap, wheremap);
		return j;
	}
	
	/** ?????????????????????????????????
	 * 
	 * @throws Exception **/
	@Scheduled(cron = "0 0 */6 * * ?")
	// XXX ?????????????????????
	// @Scheduled(cron = "1 * * * * ? ")
	public void cleanuser() throws Exception {
		userService.removeExpired();
	}
}
