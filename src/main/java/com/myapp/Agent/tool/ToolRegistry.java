package com.myapp.agent.tool;

import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ToolRegistry {

    private final Map<String, Tool> tools = new ConcurrentHashMap<>();
    private final List<Tool> springTools;

    public ToolRegistry(List<Tool> springTools) {
        this.springTools = springTools;
    }

    @PostConstruct
    public void init() {
        if (springTools != null) {
            for (Tool tool : springTools) {
                register(tool);
            }
        }
    }

    public void register(Tool tool) {
        tools.put(tool.getName().toLowerCase(), tool);
    }

    public Optional<Tool> getTool(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(tools.get(name.toLowerCase()));
    }

    public List<Tool> getAllTools() {
        return new ArrayList<>(tools.values());
    }
}
