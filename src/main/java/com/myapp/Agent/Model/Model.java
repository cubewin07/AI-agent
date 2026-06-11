package com.myapp.Agent.Model;

import lombok.Data;
import lombok.AllArgsConstructor;

@Data
@AllArgsConstructor
public class Model {
    private String systemPrompt;
    private String APIKey;
    private String modelName;
    private String Url;
    private String contextLimit;

    // TODO: Add tools to the model
    // private List<Tools> tools;


    // Based on OpenAI compatible or Claude compatible (dependcies for that)
     public LLMResponse callLLM(String input) {
        // Implement the logic to call the LLM with the input and return the response
        // For now, we will return a placeholder LLM response
        return new LLMResponse("This is a placeholder LLM response for input: " + input);
    }

    

}
