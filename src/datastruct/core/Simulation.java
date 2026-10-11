package datastruct.core;

import java.util.List;

public record Simulation(Action action, List<Step> steps, String complexity, String outcome) {
    public Simulation {
        steps = List.copyOf(steps);
    }
}
