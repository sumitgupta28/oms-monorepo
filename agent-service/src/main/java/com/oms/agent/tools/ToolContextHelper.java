package com.oms.agent.tools;

import org.springframework.ai.chat.model.ToolContext;
import reactor.core.publisher.Sinks;

class ToolContextHelper {

    private ToolContextHelper() {}

    static void emitToolCall(ToolContext ctx, String toolName) {
        if (ctx == null) return;
        Object sink = ctx.getContext().get("toolCallSink");
        if (sink instanceof Sinks.Many<?> many) {
            //noinspection unchecked
            ((Sinks.Many<String>) many).tryEmitNext(toolName);
        }
    }
}
