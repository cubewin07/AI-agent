package com.myapp.Agent;
import com.myapp.Agent.Model.Model;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Agent {
    private Model model;

    private List<Messages> conversationHistory;
    private Map<String, Tool> tools;

    public String processQuery(String query) {
        // Prepare the input for the model, including system prompt, context, conversation history, and the new query
        // Use structured messages to maintain the conversation history and context for better understanding by the model
        String input = model.getSystemPrompt() + "\n" + conversationHistory + "\nUser: " + query;
        
        AgentState agentState = new AgentState(); // Create an instance of AgentState to manage the state of the agent
        
        while(agentState.getDone() == false) {
            DECIDEOBJECT decision = agentState.decide(input); // Get the decision from the agent state based on the input
            
            if(decision.getToolName() != null) {
                String toolResponse = tools.get(decision.getToolName()).call(input); // Call the specified tool and get the response
                input += "\nTool Response: " + toolResponse; // Add the tool response to the input for further processing
            } else {
                String llmResponse = model.callLLM(input).getContent(); // Call the LLM with the input and get the response
                input += "\nLLM Response: " + llmResponse; // Add the LLM response to the input for further processing
            }
        }


        conversationHistory.add(); // Add the model's response to the conversation history
        return agentState.getContent(); // Return the content of the model's response
    }

    

}
