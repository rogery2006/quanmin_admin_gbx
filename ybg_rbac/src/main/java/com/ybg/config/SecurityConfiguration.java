package com.ybg.config;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.vote.AuthenticatedVoter;
import org.springframework.security.access.vote.RoleVoter;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.event.LoggerListener;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.access.expression.DefaultWebSecurityExpressionHandler;
import org.springframework.security.web.access.expression.WebExpressionVoter;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import com.ybg.config.security.LoginSuccessHandler;
import com.ybg.config.security.MyAccessDecisionManager;
import com.ybg.config.security.MySecurityMetadataSource;
import com.ybg.config.security.YcAnthencationProder;
import com.ybg.rbac.user.service.LoginService;

@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {
	
	@Autowired
	private LoginService				userDetailsService;
	@Autowired
	private YcAnthencationProder		provider;
	@Autowired
	DataSource							datasource;
	@Autowired
	private MySecurityMetadataSource	mySecurityMetadataSource;
	
	@Override
	protected void configure(HttpSecurity http) throws Exception {
		// // ????????????????????????
		http.authorizeRequests().antMatchers("/", "/upload/**", "/css/**", "/js/**", "/images/**", "/resources/**", "/lib/**", "/skin/**", "/template/**", "/common/**", "/404/**", "/admin_files/**", "/echarts/**", "/fonts/**", "/notebook/**").permitAll();
		// // ????????????????????????????????????
		// http.authorizeRequests().anyRequest().authenticated();
		//
		// // ??????iframe ??????
		http.headers().frameOptions().disable();
		// // ??????csrf ??????????????????
		// http.csrf().disable();
		// / ************??????***/
		// http.addFilterAfter(MyUsernamePasswordAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
		// ????????????????????????
		http.authorizeRequests().anyRequest().authenticated().withObjectPostProcessor(new ObjectPostProcessor<FilterSecurityInterceptor>() {
			
			public <O extends FilterSecurityInterceptor> O postProcess(O fsi) {
				fsi.setSecurityMetadataSource(mySecurityMetadataSource);
				fsi.setAccessDecisionManager(accessDecisionManager());
				fsi.setAuthenticationManager(authenticationManagerBean());
				return fsi;
			}
		});
		// ?????????accessDecisionManager???????????????,????????????????????????
		// http.exceptionHandling().accessDeniedHandler(accessDeniedHandler()).and().authorizeRequests().anyRequest().authenticated().expressionHandler(webSecurityExpressionHandler());
		// ?????????????????????
		http.csrf().disable();
		// session??????
		http.sessionManagement().maximumSessions(1);
	}
	
	@Override
	public void configure(WebSecurity web) throws Exception {
		web.ignoring().antMatchers("/resources/**");
		web.ignoring().antMatchers("/webjars/**");
		web.ignoring().antMatchers("/img/**");
	}
	
	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {
		// ?????????????????????
		auth.authenticationProvider(provider);
		// ??????????????????
		auth.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
	}
	
	/** ???????????? **/
	@Bean
	public BCryptPasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder(4);
	}
	
	/** ?????????????????????????????? ????????????????????????????????? */
	@Bean
	public LoginSuccessHandler loginSuccessHandler() {
		return new LoginSuccessHandler();
	}
	
	@Bean
	public LoggerListener loggerListener() {
		return new LoggerListener();
	}
	
	@Bean
	public org.springframework.security.access.event.LoggerListener eventLoggerListener() {
		return new org.springframework.security.access.event.LoggerListener();
	}
	
	/*
	 * 
	 * ???????????????????????????????????????
	 */
	@Bean(name = "accessDecisionManager")
	public AccessDecisionManager accessDecisionManager() {
		List<AccessDecisionVoter> decisionVoters = new ArrayList<AccessDecisionVoter>();
		decisionVoters.add(new RoleVoter());
		decisionVoters.add(new AuthenticatedVoter());
		decisionVoters.add(webExpressionVoter());// ????????????????????????
		MyAccessDecisionManager accessDecisionManager = new MyAccessDecisionManager(decisionVoters);
		return accessDecisionManager;
	}
	
	@Bean(name = "authenticationManager")
	@Override
	public AuthenticationManager authenticationManagerBean() {
		AuthenticationManager authenticationManager = null;
		try {
			authenticationManager = super.authenticationManagerBean();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return authenticationManager;
	}
	
	@Bean(name = "failureHandler")
	public SimpleUrlAuthenticationFailureHandler simpleUrlAuthenticationFailureHandler() {
		return new SimpleUrlAuthenticationFailureHandler("/common/login_do/unauthorizedUrl.do");
	}
	
	/*
	 * ??????????????????
	 */
	@Bean(name = "expressionHandler")
	public DefaultWebSecurityExpressionHandler webSecurityExpressionHandler() {
		DefaultWebSecurityExpressionHandler webSecurityExpressionHandler = new DefaultWebSecurityExpressionHandler();
		return webSecurityExpressionHandler;
	}
	
	/*
	 * ??????????????????
	 */
	@Bean(name = "expressionVoter")
	public WebExpressionVoter webExpressionVoter() {
		WebExpressionVoter webExpressionVoter = new WebExpressionVoter();
		webExpressionVoter.setExpressionHandler(webSecurityExpressionHandler());
		return webExpressionVoter;
	}
}
