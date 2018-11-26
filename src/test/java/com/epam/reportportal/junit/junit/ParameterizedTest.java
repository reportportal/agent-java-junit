package com.epam.reportportal.junit.junit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static com.nordstrom.automation.junit.ArtifactParams.param;

import java.util.Map;
import java.util.Optional;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.nordstrom.automation.junit.ArtifactParams;
import com.nordstrom.automation.junit.AtomIdentity;

@RunWith(Parameterized.class)
public class ParameterizedTest implements ArtifactParams {
	
	@Rule
	public final AtomIdentity identity = new AtomIdentity(this);

    private String input;
    
    public ParameterizedTest(String input) {
        this.input = input;
    }
    
    @Parameters
    public static Object[] data() {
        return new Object[] { "first test", "second test" };
    }
    
	@Override
	public Description getDescription() {
		return identity.getDescription();
	}

    @Override
    public Optional<Map<String, Object>> getParameters() {
        return ArtifactParams.mapOf(param("input", input));
    }
    
    @Test
    public void parameterized() {
    	Optional<Map<String, Object>> params = identity.getParameters();
    	assertTrue(params.isPresent());
    	assertTrue(params.get().containsKey("input"));
        assertEquals(input, params.get().get("input"));
    }
}
