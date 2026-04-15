package com.oms.agent.config;

import com.oms.agent.tools.OrderTools;
import com.oms.agent.tools.PaymentTools;
import com.oms.agent.tools.ProductTools;
import com.oms.agent.tools.ValidationTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AgentConfig {

    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .maxMessages(10)
                .build();
    }

    @Bean
    public ChatClient chatClient(ChatModel chatModel,
                                  ChatMemory chatMemory,
                                  OrderTools orderTools,
                                  PaymentTools paymentTools,
                                  ProductTools productTools,
                                  ValidationTools validationTools) {

        return ChatClient.builder(chatModel)
            .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
            .defaultTools(orderTools, paymentTools, productTools, validationTools)
            .build();
    }
}
