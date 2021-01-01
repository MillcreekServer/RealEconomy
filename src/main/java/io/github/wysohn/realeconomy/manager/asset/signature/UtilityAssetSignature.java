package io.github.wysohn.realeconomy.manager.asset.signature;

import java.util.Objects;

public abstract class UtilityAssetSignature extends AssetSignature {
    private final String utilityType;

    public UtilityAssetSignature(String utilityType) {
        this.utilityType = utilityType;
    }

    @Override
    public String category() {
        return "utility";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UtilityAssetSignature that = (UtilityAssetSignature) o;
        return utilityType.equals(that.utilityType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(utilityType);
    }

    @Override
    public String toString() {
        return "Utility: " + utilityType;
    }
}
