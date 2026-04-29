package com.oms.agent;
import com.oms.agent.security.JwtTokenHolder;
import io.micrometer.context.ContextRegistry;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import reactor.core.publisher.Hooks;
@SpringBootApplication
@EnableAsync
public class AgentServiceApplication {
    public static void main(String[] args) {
        Hooks.enableAutomaticContextPropagation();
        ContextRegistry.getInstance().registerThreadLocalAccessor(
            JwtTokenHolder.CONTEXT_KEY,
            JwtTokenHolder::get,
            JwtTokenHolder::set,
            JwtTokenHolder::clear
        );
        SpringApplication.run(AgentServiceApplication.class, args);
    }
}
