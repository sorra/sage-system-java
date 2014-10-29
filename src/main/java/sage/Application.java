package sage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.embedded.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.web.filter.CharacterEncodingFilter;

@Configuration
@EnableAutoConfiguration
@ImportResource({"root-context.xml", "front-servlet.xml"})
public class Application {
  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }
  
  @Bean
  public FilterRegistrationBean encodingFilter() {
    CharacterEncodingFilter filter = new CharacterEncodingFilter();
    filter.setEncoding("UTF-8");
    filter.setForceEncoding(true);
    return new FilterRegistrationBean(filter);
  }
}
