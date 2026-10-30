package industries.angelic.biomic.compat.efr;

/** Exact IWorldGenerator registrations in the pinned EFR build. */
public enum EfrGeneratorStage {

    NORMAL("ganymedes01.etfuturum.world.EtFuturumWorldGenerator", 0),
    LATE("ganymedes01.etfuturum.world.EtFuturumLateWorldGenerator", Integer.MAX_VALUE);

    private final String targetClassName;
    private final int registrationWeight;

    EfrGeneratorStage(String targetClassName, int registrationWeight) {
        this.targetClassName = targetClassName;
        this.registrationWeight = registrationWeight;
    }

    public String getTargetClassName() {
        return targetClassName;
    }

    public int getRegistrationWeight() {
        return registrationWeight;
    }
}
