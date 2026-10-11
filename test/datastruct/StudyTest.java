package datastruct;

import datastruct.core.*;
import datastruct.ui.SourcePanel;
import datastruct.ui.Theme;
import java.nio.file.*;
import java.util.List;
import javax.swing.SwingUtilities;

public final class StudyTest {
    private static int checks;
    private static void check(boolean condition, String label) { checks++; if (!condition) throw new AssertionError(label); }
    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        StudioSession session = new StudioSession();
        session.configureVector(3, true);
        session.execute(StructureType.VECTOR, Action.VECTOR_ADD, "4 5 6", null);
        try { session.execute(StructureType.VECTOR, Action.VECTOR_ADD, "7", null); throw new AssertionError("overflow accepted"); }
        catch (IllegalArgumentException expected) { check(session.simulator().values(StructureType.VECTOR).equals(List.of(4,5,6)), "overflow preserves data"); }
        session.configureVector(16, false);
        check(session.simulator().vectorCapacity() == 16 && session.simulator().values(StructureType.VECTOR).size() == 3, "resize preserves occupied values");
        session.undo(); check(session.simulator().vectorCapacity() == 3, "undo capacity");
        session.redo(); check(session.simulator().vectorCapacity() == 16, "redo capacity");
        try { session.configureVector(2, false); throw new AssertionError("truncation accepted"); }
        catch (IllegalArgumentException expected) { check(session.simulator().vectorCapacity() == 16 && session.simulator().values(StructureType.VECTOR).size() == 3, "failed shrink atomic"); }
        session.configureVector(1, true); session.example(StructureType.VECTOR);
        check(session.simulator().values(StructureType.VECTOR).equals(List.of(12)), "example fits capacity one");
        session.configureVector(64, true);
        session.generate(StructureType.VECTOR, 64, -999, 999, 42);
        check(session.simulator().values(StructureType.VECTOR).size() == 64, "generate full custom capacity");
        check(session.simulator().current(StructureType.VECTOR).capacity() == 64, "capacity carried by frames");
        for (int invalid : new int[]{0,65}) {
            try { session.configureVector(invalid,true); throw new AssertionError("invalid capacity"); }
            catch (IllegalArgumentException expected) { check(session.simulator().values(StructureType.VECTOR).size()==64, "invalid config atomic"); }
        }
        Path file = Files.createTempFile("datastruct-capacity-", ".dsxml");
        try {
            SessionXml.save(session.simulator(), file); Simulator loaded = SessionXml.load(file);
            check(loaded.vectorCapacity()==64 && loaded.values(StructureType.VECTOR).equals(session.simulator().values(StructureType.VECTOR)), "custom capacity XML roundtrip");
            Simulator legacy = new Simulator(); legacy.example(StructureType.VECTOR); SessionXml.save(legacy,file);
            Files.writeString(file, Files.readString(file).replace("version=\"2\"", "version=\"1\""));
            check(SessionXml.load(file).vectorCapacity()==10, "legacy sessions retain capacity ten");
        } finally { Files.deleteIfExists(file); }
        SwingUtilities.invokeAndWait(() -> {
            Theme.install(); SourcePanel source = new SourcePanel();
            for (StructureType type : StructureType.values()) for (Action action : Action.forType(type)) {
                Simulator model = new Simulator(); model.example(type);
                Simulation trace = model.perform(type, action, action.needsValue ? 7 : null, action.needsPosition ? 0 : null);
                for (Step step : trace.steps()) {
                    source.show(action,step);
                    check(!source.displayedText().contains("Fonte não encontrada"), "method is available: " + step.sourceMethod());
                    if (!step.sourceMethod().equals("perform")) {
                        check(source.activeSourceLine()==step.sourceLine(), "actual source line highlighted " + action);
                        check(source.displayedText().contains(step.sourceMethod()+"("), "correct method shown " + action);
                    }
                }
            }
        });
        System.out.println("OK: " + checks + " checks of vector capacity, sessions and source highlighting.");
    }
}
