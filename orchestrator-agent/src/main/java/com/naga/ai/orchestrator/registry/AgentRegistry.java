package com.naga.ai.orchestrator.registry;

import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentSkill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Component
public class AgentRegistry {
    private static final Logger logger = LoggerFactory.getLogger(AgentRegistry.class);

    private final Map<String, AgentCard> skillToAgentCard = new HashMap<>();

    public void register(AgentCard agentCard) {
        if(null == agentCard){
            logger.info("Agent Card is Null");
            return;
        }

        if(null == agentCard.skills() || agentCard.skills().isEmpty()) {
            logger.info("Agent Skill are either null or empty");
            return;
        }

        for (AgentSkill agentSkill : agentCard.skills()) {
            logger.info("Registered skills are : {}", agentSkill.name());
            skillToAgentCard.put(agentSkill.id(), agentCard);
        }
    }

    public AgentCard findAgentBySkill(String agentSkillId) {
        logger.info("findAgentBySkill with : {}", agentSkillId);
        AgentCard card = skillToAgentCard.get(agentSkillId);
        if (card == null) {
            logger.warn("No agent found for the skill: {}",agentSkillId);
            return null;
        }
        return card;
    }

    public Map<String, AgentCard> getAllAgents() {
        return Collections.unmodifiableMap(skillToAgentCard);
    }

    public boolean hasAgents() {
        return !skillToAgentCard.isEmpty();
    }

    public int skillCount() {
        return skillToAgentCard.size();
    }
}
