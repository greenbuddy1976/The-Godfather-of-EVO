package com.greenbuddy.godfatherlive;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import static org.junit.Assert.*;

public class SetupEngineTest {
    @Test public void exactlyFiveProfilesExist() {
        assertEquals(5, SetupEngine.Profile.values().length);
    }

    @Test public void everyProfileProducesRealChanges() {
        int[] fine = new int[8];
        for (SetupEngine.Profile p : SetupEngine.Profile.values()) {
            assertFalse(p.toString(), SetupEngine.changes("Ford Mustang GT3", "Spa Francorchamps", p, fine).isEmpty());
        }
    }

    @Test public void mustangStableFastForcesTc1() {
        int[] fine = new int[8];
        fine[1] = 3; // User fine tune says TC3, Mustang rule must override it for STABLE+FAST.
        List<SetupEngine.Change> changes = SetupEngine.changes(
                "Ford Mustang GT3", "Nordschleife", SetupEngine.Profile.STABLE_FAST, fine);
        SetupEngine.Change lastTc = null;
        for (SetupEngine.Change c : changes) if ("5.1".equals(c.path)) lastTc = c;
        assertNotNull(lastTc);
        assertEquals(SetupEngine.Operation.SET, lastTc.operation);
        assertEquals(1f, lastTc.value, 0.0001f);
    }

    @Test public void stableFastDoesNotForceTcOnOtherCars() {
        int[] fine = new int[8];
        List<SetupEngine.Change> changes = SetupEngine.changes(
                "Ferrari 296 GT3", "Nordschleife", SetupEngine.Profile.STABLE_FAST, fine);
        for (SetupEngine.Change c : changes) assertNotEquals("5.1", c.path);
    }

    @Test public void carAliasesNormalizeWithoutInventingDifferentCars() {
        assertEquals(QueryLogic.key("Mazda MX-5 NA (1994)"), QueryLogic.key("Mazda MX5 NA"));
        assertEquals(QueryLogic.key("SPA Francorchamps"), QueryLogic.key("Spa-Francorchamps"));
    }

    @Test public void binaryEditorChangesOnlyParsedFloatFieldsAndReparses() {
        byte[] source = syntheticCarsetup();
        int beforeCount = BinarySetupEditor.countWritableFields(source);
        assertTrue(beforeCount >= 20);

        int[] fine = new int[8];
        List<SetupEngine.Change> changes = SetupEngine.changes(
                "Ford Mustang GT3", "Nordschleife", SetupEngine.Profile.STABLE_FAST, fine);
        BinarySetupEditor.EditResult result = BinarySetupEditor.apply(source, changes);

        assertTrue(result.applied > 0);
        assertEquals(source.length, result.bytes.length);
        assertFalse(java.util.Arrays.equals(source, result.bytes));
        assertEquals(beforeCount, BinarySetupEditor.countWritableFields(result.bytes));
    }

    private static byte[] syntheticCarsetup() {
        byte[] mechanics = concat(
                f32(1, 50000f), f32(1, 30000f),
                msg(3, f32(1, 60.0f)),
                msg(4, f32(3, 100f))
        );
        ByteArrayOutputStream root = new ByteArrayOutputStream();
        write(root, msg(1, mechanics));
        for (int i = 0; i < 4; i++) {
            write(root, msg(2, concat(f32(1, 140000f + i * 5000f), msg(2, concat(f32(1, 15f), f32(2, 30f))))));
        }
        for (int i = 0; i < 4; i++) {
            write(root, msg(3, concat(f32(1, 8000f), f32(2, 6000f), f32(3, 9000f), f32(4, 7000f))));
        }
        for (int i = 0; i < 4; i++) {
            write(root, msg(4, concat(f32(1, 27f), f32(2, -3.0f), f32(3, 0.10f), f32(4, 7f))));
        }
        write(root, msg(5, concat(f32(1, 2f), f32(2, 0f), f32(3, 2f))));
        write(root, msg(6, concat(f32(2, 55f), f32(3, 60f), f32(5, 7f))));
        write(root, msg(7, f32(1, 30f)));
        return root.toByteArray();
    }

    private static byte[] f32(int field, float value) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeVarint(out, (field << 3) | 5);
        byte[] b = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(value).array();
        write(out, b);
        return out.toByteArray();
    }

    private static byte[] msg(int field, byte[] child) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeVarint(out, (field << 3) | 2);
        writeVarint(out, child.length);
        write(out, child);
        return out.toByteArray();
    }

    private static byte[] concat(byte[]... chunks) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte[] c : chunks) write(out, c);
        return out.toByteArray();
    }

    private static void writeVarint(ByteArrayOutputStream out, int value) {
        int v = value;
        while ((v & ~0x7f) != 0) {
            out.write((v & 0x7f) | 0x80);
            v >>>= 7;
        }
        out.write(v);
    }

    private static void write(ByteArrayOutputStream out, byte[] bytes) {
        out.write(bytes, 0, bytes.length);
    }
}
