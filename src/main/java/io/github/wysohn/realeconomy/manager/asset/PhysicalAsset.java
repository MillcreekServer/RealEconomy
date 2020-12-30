package io.github.wysohn.realeconomy.manager.asset;

import io.github.wysohn.rapidframework3.core.language.DynamicLang;
import io.github.wysohn.rapidframework3.utils.Validation;
import io.github.wysohn.realeconomy.main.RealEconomyLangs;
import io.github.wysohn.realeconomy.manager.asset.signature.AssetSignature;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public abstract class PhysicalAsset extends Asset {
    private double amount = 1.0;

    public PhysicalAsset(UUID key,
                         AssetSignature signature) {
        super(key, signature);
    }

    /**
     * @return
     * @deprecated use {@link #getNumericalMeasure()}
     */
    @Deprecated
    public int getAmount() {
        return (int) amount; // fractional parts are considered 'in-complete'
    }

    /**
     * @param amount
     * @deprecated use {@link #setNumericalMeasure(double)}
     */
    @Deprecated
    public void setAmount(int amount) {
        this.amount = amount;

        setLastUpdate(System.currentTimeMillis());
    }

    @Override
    public double getNumericalMeasure() {
        return amount;
    }

    @Override
    public void setNumericalMeasure(double value) {
        Validation.validate(value, v -> v > 0.0, "Must be non-zero positive.");
        this.amount = value;

        setLastUpdate(System.currentTimeMillis());
    }

    @Override
    public List<DynamicLang> lore() {
        List<DynamicLang> parentLore = super.lore();
        parentLore.add(new DynamicLang(RealEconomyLangs.GUI_Assets_Physical_Amount, (s, m) ->
                m.addInteger(getAmount())));
        return parentLore;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        PhysicalAsset that = (PhysicalAsset) o;
        return amount == that.amount;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), amount);
    }

//    public static void main(String[] ar){
//        double val = 0.9999999999999999;
//        System.out.println((int) val);
//        System.out.println((int) Math.floor(val));
//    }
}
