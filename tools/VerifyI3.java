import com.customblocks.buzzergame.BuzzerSession;
import java.util.UUID;

/** Throwaway I3 verifier: drives the REAL compiled BuzzerSession through both press cycles and asserts the
 *  target-less (bare press) vs armed (start command) flows land in the right states. */
public final class VerifyI3 {
    static int fails = 0;
    public static void main(String[] args) {
        UUID host = UUID.randomUUID();
        UUID buzzer = UUID.randomUUID();

        // ---- Bare-press flow (I3): IDLE start with no target, press 3 returns to IDLE ----
        BuzzerSession s = new BuzzerSession(host);
        s.linkBuzzer(buzzer, null);
        eq("bare: unarmed hasTarget", s.hasTarget(), false);
        eq("bare press1 phase", s.onBuzz(buzzer, "P").phase(), BuzzerSession.BuzzPhase.START);
        eq("bare running hasTarget (no هدف line)", s.hasTarget(), false);
        eq("bare result climbs from 0", s.resultText().equals("0.00"), true); // 0 ticks in yet
        eq("bare press2 phase", s.onBuzz(buzzer, "P").phase(), BuzzerSession.BuzzPhase.STOP);
        eq("bare frozen hasTarget", s.hasTarget(), false);
        eq("bare press3 phase", s.onBuzz(buzzer, "P").phase(), BuzzerSession.BuzzPhase.RESET);
        eq("bare press3 → back to IDLE (targetless, no هدف)", s.hasTarget(), false);
        eq("bare press4 restarts (IDLE→START)", s.onBuzz(buzzer, "P").phase(), BuzzerSession.BuzzPhase.START);

        // ---- Armed flow (start command): هدف shown, press 3 returns to ARMED keeping the هدف ----
        BuzzerSession a = new BuzzerSession(host);
        a.linkBuzzer(buzzer, null);
        a.armTarget(5.0);
        eq("armed hasTarget (هدف shown)", a.hasTarget(), true);
        eq("armed target text", a.targetText().equals("5.00"), true);
        eq("armed press1 phase", a.onBuzz(buzzer, "P").phase(), BuzzerSession.BuzzPhase.START);
        eq("armed running still shows هدف", a.hasTarget(), true);
        eq("armed press2 phase", a.onBuzz(buzzer, "P").phase(), BuzzerSession.BuzzPhase.STOP);
        eq("armed press3 phase", a.onBuzz(buzzer, "P").phase(), BuzzerSession.BuzzPhase.RESET);
        eq("armed press3 → back to ARMED, هدف kept", a.hasTarget(), true);
        eq("armed target still 5.00 after reset", a.targetText().equals("5.00"), true);

        // ---- Unlinked buzzer is ignored (no session state change) ----
        BuzzerSession u = new BuzzerSession(host);
        eq("unlinked press ignored", u.onBuzz(UUID.randomUUID(), "P").phase(), BuzzerSession.BuzzPhase.IGNORED);

        System.out.println(fails == 0 ? "\nI3 VERIFY: ALL PASS" : "\nI3 VERIFY: " + fails + " FAIL(S)");
        System.exit(fails == 0 ? 0 : 1);
    }
    static void eq(String name, Object got, Object want) {
        boolean ok = (got == null && want == null) || (got != null && got.equals(want));
        System.out.println((ok ? "  ok  " : " FAIL ") + name + "  (got=" + got + ", want=" + want + ")");
        if (!ok) fails++;
    }
}
