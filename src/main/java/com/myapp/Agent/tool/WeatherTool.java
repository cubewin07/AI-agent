package com.myapp.agent.tool;

import org.springframework.stereotype.Component;

@Component
public class WeatherTool implements Tool {

    @Override
    public String getName() {
        return "weather";
    }

    @Override
    public String getDescription() {
        return "Gets the current weather for a given city. Input should be the name of the city.";
    }

    @Override
    public String getParameterSchema() {
        return "The name of the city (e.g., 'London', 'New York').";
    }

    @Override
    public String execute(String input) {
        if (input == null || input.isBlank()) {
            return "Error: City name is empty";
        }
        String city = input.trim();
        // Return mock weather
        return switch (city.toLowerCase()) {
            case "london" -> "The weather in London is rainy, 15°C.";
            case "new york" -> "The weather in New York is sunny, 22°C.";
            case "tokyo" -> "The weather in Tokyo is cloudy, 18°C.";
            case "paris" -> "The weather in Paris is windy, 16°C.";
            default -> "The weather in " + city + " is sunny, 20°C.";
        };
    }
}
