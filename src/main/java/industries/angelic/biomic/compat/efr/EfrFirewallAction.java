package industries.angelic.biomic.compat.efr;

/** Explicit ownership decision for an intercepted EFR generation path. */
public enum EfrFirewallAction {

    PASS_THROUGH(false),
    SUPPRESS(true);

    private final boolean cancelsGeneration;

    EfrFirewallAction(boolean cancelsGeneration) {
        this.cancelsGeneration = cancelsGeneration;
    }

    public boolean cancelsGeneration() {
        return cancelsGeneration;
    }
}
